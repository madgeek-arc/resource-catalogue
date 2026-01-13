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

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

//FIXME: need to work with external catalogues too -> get with catalogueId
@Service("securityService")
public class OIDCSecurityService implements SecurityService {

    private final CatalogueService catalogueService;
    private final ProviderService providerService;
    private final ServiceService serviceService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final DeployableServiceService deployableServiceService;
    private final AdapterService adapterService;
    private final Authentication adminAccess = new AdminAuthentication();

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    @Value("${catalogue.id}")
    private String catalogueId;

    public OIDCSecurityService(@Lazy CatalogueService catalogueService,
                               @Lazy ProviderService providerService,
                               @Lazy ServiceService serviceService,
                               @Lazy TrainingResourceService trainingResourceService,
                               @Lazy InteroperabilityRecordService interoperabilityRecordService,
                               @Lazy DeployableServiceService deployableServiceService,
                               @Lazy AdapterService adapterService,
                               CatalogueProperties properties) {
        this.catalogueService = catalogueService;
        this.providerService = providerService;
        this.serviceService = serviceService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.deployableServiceService = deployableServiceService;
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
        List<User> users = isProvider ? getProviderUsers(id) : getCatalogueUsers(id);
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

    private boolean isProvider(String id) {
        return providerService.exists(id);
    }

    private List<User> getProviderUsers(String id) {
        NewProviderBundle registeredProvider = checkProviderExistence(id);
        if (registeredProvider == null) {
            return null;
        }
        Object usersObj = registeredProvider.getProvider().get("users");
        if (!(usersObj instanceof List<?> usersList)) {
            return null;
        }

        List<User> users = new ArrayList<>();
        for (Object obj : usersList) {
            if (obj instanceof Map<?, ?> userMap) {
                users.add(mapToUser(userMap));
            }
        }
        return users;
    }

    //TODO: make global
    private User mapToUser(Map<?, ?> userMap) {
        User user = new User();
        user.setId((String) userMap.get("id"));
        user.setName((String) userMap.get("name"));
        user.setSurname((String) userMap.get("surname"));
        user.setEmail((String) userMap.get("email"));
        return user;
    }

    private List<User> getCatalogueUsers(String id) {
        CatalogueBundle registeredCatalogue = checkCatalogueExistence(id);
        if (registeredCatalogue == null || registeredCatalogue.getCatalogue().getUsers() == null) {
            return null;
        }
        return registeredCatalogue.getCatalogue().getUsers();
    }

    private NewProviderBundle checkProviderExistence(String providerId) {
        try {
            return providerService.get(providerId);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
    }

    private CatalogueBundle checkCatalogueExistence(String id) {
        try {
            return catalogueService.get(id, adminAccess);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
    }

    private boolean userMatches(User u1, User u2) {
        return u1.getEmail().equalsIgnoreCase(u2.getEmail());
    }

    @Override
    public boolean isApprovedProvider(String prefix, String suffix) {
        String id = prefix + "/" + suffix;
        NewProviderBundle bundle = providerService.get(id);
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
        NewBundle bundle = determineResourceType(resourceId);
        providerId = switch (bundle) {
            case NewServiceBundle serviceBundle -> (String) serviceBundle.getService().get("serviceOwner");
            //FIXME
//            case TrainingResourceBundle trainingResourceBundle ->
//                    trainingResourceBundle.getTrainingResource().getResourceOrganisation();
//            case DeployableServiceBundle deployableServiceBundle ->
//                    deployableServiceBundle.getDeployableService().getResourceOrganisation();
//            case null, default -> ((InteroperabilityRecordBundle) bundle).getInteroperabilityRecord().getProviderId();
            case null, default -> null;
        };
        return providerId;
    }

    private NewBundle determineResourceType(String id) {
        if (isProvider(id)) {
            return providerService.getOrElseReturnNull(id);
        } else if (isService(id)) {
            return serviceService.getOrElseReturnNull(id);
        }
        //FIXME
//        else if (isTrainingResource(resourceId)) {
//            return trainingResourceService.getOrElseReturnNull(resourceId);
//        } else if (isDeployableService(resourceId)) {
//            return deployableServiceService.getOrElseReturnNull(resourceId);
//        } else {
//            return interoperabilityRecordService.getOrElseReturnNull(resourceId);
//        }
        return null;
    }

    private boolean isService(String id) {
        return serviceService.exists(id);
    }

    private boolean isTrainingResource(String id) {
        return trainingResourceService.exists(id);
    }

    private boolean isDeployableService(String id) {
        return deployableServiceService.exists(id);
    }


    //FIXME - how to get catalogueId
    @Override
    public boolean providerCanAddResources(Authentication auth, LinkedHashMap<String, Object> service, String catalogueId) {
        String providerId = (String) service.get("serviceOwner");
        if (catalogueId == null || catalogueId.isEmpty()) {
            catalogueId = this.catalogueId;
        }
        NewProviderBundle provider = providerService.get(providerId, catalogueId);
        return canAddResources(auth, provider);
    }

    //FIXME - how to get catalogueId
    @Override
    public boolean providerCanAddResources(Authentication auth, gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResource trainingResource) {
        return false;
//        String providerId = trainingResource.getResourceOrganisation();
//        String catalogueId = trainingResource.getCatalogueId();
//        if (catalogueId == null || catalogueId.isEmpty()) {
//            catalogueId = this.catalogueId;
//        }
//        ProviderBundle provider = providerService.get(catalogueId, providerId, auth);
//        return canAddResources(auth, provider, trainingResource.getId());
    }

    //FIXME - how to get catalogueId
    @Override
    public boolean providerCanAddResources(Authentication auth, gr.uoa.di.madgik.resourcecatalogue.domain.DeployableService deployableService) {
        return false;
//        String providerId = deployableService.getResourceOrganisation();
//        String catalogueId = deployableService.getCatalogueId();
//        if (catalogueId == null || catalogueId.isEmpty()) {
//            catalogueId = this.catalogueId;
//        }
//        ProviderBundle provider = providerService.get(catalogueId, providerId, auth);
//        return canAddResources(auth, provider, deployableService.getId());
    }

    //FIXME - how to get catalogueId
    @Override
    public boolean providerCanAddResources(Authentication auth, gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecord interoperabilityRecord) {
        return false;
//        String providerId = interoperabilityRecord.getProviderId();
//        String catalogueId = interoperabilityRecord.getCatalogueId();
//        if (catalogueId == null || catalogueId.isEmpty()) {
//            catalogueId = this.catalogueId;
//        }
//        ProviderBundle provider = providerService.get(catalogueId, providerId, auth);
//        return canAddResources(auth, provider, interoperabilityRecord.getId());
    }

    private boolean canAddResources(Authentication auth, NewProviderBundle provider) {
        // provider related check
        if (!hasAdminAccess(auth, provider.getId())) {
            return false;
        }
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
        if ("rejected template status".equals(templateStatus)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean resourceIsApprovedAndUserIsAdmin(Authentication auth, String id) {
        NewBundle bundle = determineResourceType(id);
        if (bundle != null) {
            if (bundle instanceof NewProviderBundle) {
                if (bundle.getStatus().equals("approved")) {
                    return hasAdminAccess(auth, id);
                }
            } else {
                String providerId = getProviderId(id);
                NewProviderBundle provider = providerService.get(providerId);
                if (provider.getStatus().equals("approved")) {
                    return hasAdminAccess(auth, providerId);
                }
            }
        }
        return false;
    }

    @Override
    public boolean providerIsActive(String id, String catalogueId) {
        NewProviderBundle providerBundle = providerService.get(id, catalogueId);
        return providerBundle.isActive();
    }

    @Override
    public boolean serviceIsActive(String id, String catalogueId) {
        NewServiceBundle serviceBundle = serviceService.get(id, catalogueId);
        return serviceBundle.isActive();
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
    public boolean deployableServiceIsActive(String id, String catalogueId) {
        DeployableServiceBundle deployableServiceBundle = deployableServiceService.get(id, catalogueId);
        return deployableServiceBundle.isActive();
    }
    //endregion

    //region Adapters
    @Override
    public boolean hasAdapterAccess(Authentication auth, @NotNull String id) {
        return getAuthenticatedUser(auth)
                .map(user -> userHasAdapterAccess(user, id))
                .orElse(false);
    }

    @Override
    public boolean userHasAdapterAccess(User user, @NotNull String id) {
        AdapterBundle registeredAdapter = checkAdapterExistence(id);
        if (registeredAdapter == null || registeredAdapter.getAdapter().getAdmins() == null) {
            return false;
        }
        List<User> adapterAdmins = registeredAdapter.getAdapter().getAdmins();

        return adapterAdmins.parallelStream()
                .filter(Objects::nonNull)
                .anyMatch(u -> userMatches(u, user));
    }

    private AdapterBundle checkAdapterExistence(String adapterId) {
        try {
            return adapterService.get(adapterId, null, false);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
    }
    //endregion
}
