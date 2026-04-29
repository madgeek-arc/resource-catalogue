/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

//FIXME: need to work with external catalogues too -> get with catalogueId
@Service("securityService")
public class OIDCSecurityService implements SecurityService {

    private final CatalogueService catalogueService;
    private final OrganisationService organisationService;
    private final ServiceService serviceService;
    private final DatasourceService datasourceService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final DeployableApplicationService deployableApplicationService;
    private final AdapterService adapterService;
    private final Authentication adminAccess = new AdminAuthentication();

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    @Value("${catalogue.id}")
    private String catalogueId;

    public OIDCSecurityService(@Lazy CatalogueService catalogueService,
            @Lazy OrganisationService organisationService,
            @Lazy ServiceService serviceService,
            @Lazy DatasourceService datasourceService,
            @Lazy TrainingResourceService trainingResourceService,
            @Lazy InteroperabilityRecordService interoperabilityRecordService,
            @Lazy DeployableApplicationService deployableApplicationService,
            @Lazy AdapterService adapterService,
            CatalogueProperties properties) {
        this.catalogueService = catalogueService;
        this.organisationService = organisationService;
        this.serviceService = serviceService;
        this.datasourceService = datasourceService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.deployableApplicationService = deployableApplicationService;
        this.adapterService = adapterService;
    }

    @Override
    public Authentication getAdminAccess() {
        return adminAccess;
    }

    @Override
    public String getRoleName(Authentication authentication) {
        String role;
        if (hasRole(authentication, "ROLE_ADMIN")) {
            role = "ADMIN";
        } else if (hasRole(authentication, "ROLE_EPOT")) {
            role = "EPOT";
        } else if (hasRole(authentication, "ROLE_PROVIDER")) {
            role = "PROVIDER";
        } else {
            role = "USER";
        }
        return role;
    }

    @Override
    public boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    public boolean hasPortalAdminRole(Authentication auth) {
        return auth != null && (hasRole(auth, "ROLE_ADMIN") || hasRole(auth, "ROLE_EPOT"));
    }

    // region Catalogues & Providers
    @Override
    public boolean hasAdminAccess(Authentication auth, @NotNull String id) {
        return getAuthenticatedUser(auth)
                .map(user -> userHasAdminAccess(user, id))
                .orElse(false);
    }

    @Override
    public boolean userHasAdminAccess(User user, @NotNull String id) {
        boolean isProvider = isProvider(id);
        //FIXME: if provider,catalogue have the same ID
        List<User> users = getProviderUsers(id);
//        List<User> users = isProvider ? getProviderUsers(id) : getCatalogueUsers(id); //FIXME
        if (users == null) {
            return false;
        }
        return users.parallelStream()
                .filter(Objects::nonNull)
                .anyMatch(u -> userMatches(u, user));
    }

    private Optional<User> getAuthenticatedUser(Authentication auth) {
        if (auth == null || hasRole(auth, "ROLE_ANONYMOUS")) {
            return Optional.empty();
        }
        return Optional.of(Objects.requireNonNull(User.of(auth)));
    }

    public List<User> getProviderUsers(String id) {
        OrganisationBundle registeredProvider = checkProviderExistence(id);
        return getProviderUsers(registeredProvider); // reuse logic
    }

    public List<User> getProviderUsers(OrganisationBundle organisationBundle) {
        if (organisationBundle == null) {
            return Collections.emptyList();
        }

        Object usersObj = organisationBundle.getOrganisation().get("users");
        if (!(usersObj instanceof List<?> usersList)) {
            return Collections.emptyList();
        }

        List<User> users = new ArrayList<>();
        for (Object obj : usersList) {
            if (obj instanceof Map<?, ?> userMap) {
                users.add(User.fromMap(userMap));
            }
        }
        return users;
    }

//    private List<User> getCatalogueUsers(String id) {
//        CatalogueBundle registeredCatalogue = checkCatalogueExistence(id);
//        if (registeredCatalogue == null || registeredCatalogue.getCatalogue().getUsers() == null) {
//            return null;
//        }
//        return registeredCatalogue.getCatalogue().getUsers();
//    }

    private OrganisationBundle checkProviderExistence(String providerId) {
        try {
            return organisationService.get(providerId);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
    }

//    private CatalogueBundle checkCatalogueExistence(String id) {
//        try {
//            return catalogueService.get(id, adminAccess);
//        } catch (ResourceException | ResourceNotFoundException e) {
//            return null;
//        }
//    }

    private boolean userMatches(User u1, User u2) {
        return u1.getEmail().equalsIgnoreCase(u2.getEmail());
    }

    @Override
    public boolean isApprovedProvider(String prefix, String suffix) {
        String id = prefix + "/" + suffix;
        OrganisationBundle bundle = organisationService.get(id);
        return "approved".equals(bundle.getStatus());
    }
    //endregion


    // region Resources
    @Override
    public boolean isResourceAdmin(Authentication auth, String resourceId) {
        return getAuthenticatedUser(auth)
                .map(user -> userIsResourceAdmin(user, resourceId))
                .orElse(false);
    }

    @Override
    public boolean userIsResourceAdmin(@NotNull User user, String resourceId) {
        String providerId = getProviderId(resourceId);
        return userHasAdminAccess(user, providerId);
    }

    //TODO: expand to check for other resources too
    //TODO: when done, refactor PreAuthorization annotations to include them
    private String getProviderId(String resourceId) {
        String providerId;
        Bundle bundle = determineResourceType(resourceId);
        providerId = switch (bundle) {
            case ServiceBundle serviceBundle -> (String) serviceBundle.getService().get("resourceOwner");
            case DatasourceBundle datasourceBundle -> (String) datasourceBundle.getDatasource().get("resourceOwner");
            case TrainingResourceBundle trainingResourceBundle ->
                    (String) trainingResourceBundle.getTrainingResource().get("resourceOwner");
            case DeployableApplicationBundle deployableApplicationBundle ->
                    (String) deployableApplicationBundle.getDeployableApplication().get("resourceOwner");
            case AdapterBundle adapterBundle -> (String) adapterBundle.getAdapter().get("resourceOwner");
            case null, default ->
                    (String) ((InteroperabilityRecordBundle) bundle).getInteroperabilityRecord().get("resourceOwner");
        };
        return providerId;
    }

    private Bundle determineResourceType(String id) {
        if (isProvider(id)) {
            return organisationService.getOrElseReturnNull(id);
        } else if (isService(id)) {
            return serviceService.getOrElseReturnNull(id);
        } else if (isDatasource(id)) {
            return datasourceService.getOrElseReturnNull(id);
        } else if (isTrainingResource(id)) {
            return trainingResourceService.getOrElseReturnNull(id);
        } else if (isDeployableApplication(id)) {
            return deployableApplicationService.getOrElseReturnNull(id);
        } else if (isAdapter(id)) {
            return adapterService.getOrElseReturnNull(id);
        } else {
            return interoperabilityRecordService.getOrElseReturnNull(id);
        }
    }

    private boolean isProvider(String id) {
        try {
            organisationService.get(id);
            return true;
        } catch (ResourceException e) {
            return false;
        }
    }

    private boolean isService(String id) {
        try {
            serviceService.get(id);
            return true;
        } catch (ResourceException e) {
            return false;
        }
    }

    private boolean isDatasource(String id) {
        try {
            datasourceService.get(id);
            return true;
        } catch (ResourceException e) {
            return false;
        }
    }

    private boolean isTrainingResource(String id) {
        try {
            trainingResourceService.get(id);
            return true;
        } catch (ResourceException e) {
            return false;
        }
    }

    private boolean isDeployableApplication(String id) {
        try {
            deployableApplicationService.get(id);
            return true;
        } catch (ResourceException e) {
            return false;
        }
    }

    private boolean isAdapter(String id) {
        try {
            adapterService.get(id);
            return true;
        } catch (ResourceException e) {
            return false;
        }
    }

    @Override
    public boolean providerCanAddResources(Authentication auth, LinkedHashMap<String, Object> resource, String catalogueId) {
        String providerId = (String) resource.get("resourceOwner");
        OrganisationBundle provider = organisationService.get(providerId, catalogueId);
        return canAddResources(auth, provider);
    }

    private boolean canAddResources(Authentication auth, OrganisationBundle provider) {
        // provider related check
        if (!provider.isActive()) {
            return false;
        }
        String status = provider.getStatus();
        if (status == null) {
            throw new ServiceException("Provider status field is null");
        }
        if (!"approved".equals(status)) {
            return false;
        }

        // resource onboarding related check
        String templateStatus = provider.getTemplateStatus();
        if ("no template status".equals(templateStatus)) {
            return true;
        }
        if ("rejected template".equals(templateStatus)) {
            return true;
        }

        return hasAdminAccess(auth, provider.getId());
    }

    @Override
    public boolean resourceIsApprovedAndUserIsAdmin(Authentication auth, String id) {
        Bundle bundle = determineResourceType(id);
        if (bundle != null) {
            if (bundle instanceof OrganisationBundle) {
                if (bundle.getStatus().equals("approved")) {
                    return hasAdminAccess(auth, id);
                }
            } else {
                String providerId = getProviderId(id);
                OrganisationBundle provider = organisationService.get(providerId);
                if (provider.getStatus().equals("approved")) {
                    return hasAdminAccess(auth, providerId);
                }
            }
        }
        return false;
    }

    @Override
    public boolean providerIsActive(String id, String catalogueId) {
        OrganisationBundle organisationBundle = organisationService.get(id, catalogueId);
        return organisationBundle.isActive();
    }

    @Override
    public boolean serviceIsActive(String id, String catalogueId) {
        ServiceBundle serviceBundle = serviceService.get(id, catalogueId);
        return serviceBundle.isActive();
    }

    @Override
    public boolean datasourceIsActive(String id, String catalogueId) {
        DatasourceBundle datasourceBundle = datasourceService.get(id, catalogueId);
        return datasourceBundle.isActive();
    }

    @Override
    public boolean catalogueIsActive(String id) {
        CatalogueBundle catalogueBundle = catalogueService.get(id);
        return catalogueBundle.isActive();
    }

    @Override
    public boolean trainingResourceIsActive(String id, String catalogueId) {
        TrainingResourceBundle trainingResourceBundle = trainingResourceService.get(id, catalogueId);
        return trainingResourceBundle.isActive();
    }

    @Override
    public boolean guidelineIsActive(String id, String catalogueId) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(id, catalogueId);
        return interoperabilityRecordBundle.isActive();
    }

    @Override
    public boolean deployableApplicationIsActive(String id, String catalogueId) {
        DeployableApplicationBundle deployableApplicationBundle = deployableApplicationService.get(id, catalogueId);
        return deployableApplicationBundle.isActive();
    }

    @Override
    public boolean adapterIsActive(String id, String catalogueId) {
        AdapterBundle adapterBundle = adapterService.get(id, catalogueId);
        return adapterBundle.isActive();
    }
    //endregion
}
