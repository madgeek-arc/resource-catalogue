/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

// TODO: REFACTOR
//  1) Replace user.stream().filter/search? with facet filter  email=x
//  2) Use facet filter to set status/published/active (in methods everywhere in general)
@Service("securityService")
public class OIDCSecurityService implements SecurityService {

    private final CatalogueService catalogueService;
    private final ProviderService providerService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final AdapterService adapterService;
    private final Authentication adminAccess = new AdminAuthentication();

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    @Value("${catalogue.id}")
    private String catalogueId;

    public OIDCSecurityService(@Lazy CatalogueService catalogueService,
                               @Lazy ProviderService providerService,
                               @Lazy ServiceBundleService<ServiceBundle> serviceBundleService,
                               @Lazy TrainingResourceService trainingResourceService,
                               @Lazy InteroperabilityRecordService interoperabilityRecordService,
                               @Lazy AdapterService adapterService,
                               CatalogueProperties properties) {
        this.catalogueService = catalogueService;
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
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
            role = "admin";
        } else if (hasRole(authentication, "ROLE_EPOT")) {
            role = "EPOT";
        } else if (hasRole(authentication, "ROLE_PROVIDER")) {
            role = "provider";
        } else {
            role = "user";
        }
        return role;
    }

    @Override
    public boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
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
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return Optional.empty();
        }
        return Optional.of(Objects.requireNonNull(User.of(auth)));
    }

    private boolean isProvider(String id) {
        return providerService.exists(id);
    }

    private List<User> getProviderUsers(String id) {
        ProviderBundle registeredProvider = checkProviderExistence(id);
        if (registeredProvider == null || registeredProvider.getProvider().getUsers() == null) {
            return null;
        }
        return registeredProvider.getProvider().getUsers();
    }

    private List<User> getCatalogueUsers(String id) {
        CatalogueBundle registeredCatalogue = checkCatalogueExistence(id);
        if (registeredCatalogue == null || registeredCatalogue.getCatalogue().getUsers() == null) {
            return null;
        }
        return registeredCatalogue.getCatalogue().getUsers();
    }

    private ProviderBundle checkProviderExistence(String providerId) {
        try {
            return providerService.get(providerId, adminAccess);
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
        Bundle<?> bundle = determineResourceType(resourceId);
        if (bundle instanceof ServiceBundle) {
            providerId = ((ServiceBundle) bundle).getService().getResourceOrganisation();
        } else if (bundle instanceof TrainingResourceBundle) {
            providerId = ((TrainingResourceBundle) bundle).getTrainingResource().getResourceOrganisation();
        } else {
            providerId = ((InteroperabilityRecordBundle) bundle).getInteroperabilityRecord().getProviderId();
        }
        return providerId;
    }

    private Bundle<?> determineResourceType(String resourceId) {
        if (isService(resourceId)) {
            return serviceBundleService.getOrElseReturnNull(resourceId);
        } else if (isTrainingResource(resourceId)) {
            return trainingResourceService.getOrElseReturnNull(resourceId);
        } else {
            return interoperabilityRecordService.getOrElseReturnNull(resourceId);
        }
    }

    private boolean isService(String id) {
        return serviceBundleService.exists(id);
    }

    private boolean isTrainingResource(String id) {
        return trainingResourceService.exists(id);
    }

    @Override
    public boolean providerCanAddResources(Authentication auth, gr.uoa.di.madgik.resourcecatalogue.domain.Service service) {
        String providerId = service.getResourceOrganisation();
        String catalogueId = service.getCatalogueId();
        if (catalogueId == null || catalogueId.isEmpty()) {
            catalogueId = this.catalogueId;
        }
        ProviderBundle provider = providerService.get(catalogueId, providerId, auth);
        return providerCanAddResources(auth, provider, service.getId());
    }

    @Override
    public boolean providerCanAddResources(Authentication auth, gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResource trainingResource) {
        String providerId = trainingResource.getResourceOrganisation();
        String catalogueId = trainingResource.getCatalogueId();
        if (catalogueId == null || catalogueId.isEmpty()) {
            catalogueId = this.catalogueId;
        }
        ProviderBundle provider = providerService.get(catalogueId, providerId, auth);
        return providerCanAddResources(auth, provider, trainingResource.getId());
    }

    @Override
    public boolean providerCanAddResources(Authentication auth, gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecord interoperabilityRecord) {
        String providerId = interoperabilityRecord.getProviderId();
        String catalogueId = interoperabilityRecord.getCatalogueId();
        if (catalogueId == null || catalogueId.isEmpty()) {
            catalogueId = this.catalogueId;
        }
        ProviderBundle provider = providerService.get(catalogueId, providerId, auth);
        return providerCanAddResources(auth, provider, interoperabilityRecord.getId());
    }

    private boolean providerCanAddResources(Authentication auth, ProviderBundle provider, String resourceId) {
        if (!hasAdminAccess(auth, provider.getId())) {
            return false;
        }
        if (provider.getStatus() == null) {
            throw new ServiceException("Provider status field is null");
        }
        if (provider.isActive() && "approved provider".equals(provider.getStatus())) {
            return true;
        }
        return "no template status".equals(provider.getTemplateStatus()) &&
                checkIfProviderHasRegisteredAServiceTemplate(provider, resourceId);
    }

    private boolean checkIfProviderHasRegisteredAServiceTemplate(ProviderBundle provider, String resourceId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", provider.getId());

        Bundle<?> bundle = determineResourceType(resourceId);
        boolean isServiceBundle = bundle instanceof ServiceBundle;
        boolean isTrainingResource = bundle instanceof TrainingResourceBundle;

        if ((isServiceBundle && serviceBundleService.getAll(ff, getAdminAccess()).getResults().isEmpty()) ||
                (isTrainingResource && trainingResourceService.getAll(ff, getAdminAccess()).getResults().isEmpty())) {
            return true;
        }

        throw new ResourceException("You have already created a Service Template.", HttpStatus.CONFLICT);
    }

    @Override
    public boolean providerIsActiveAndUserIsAdmin(Authentication auth, String resourceId) {
        String providerId = getProviderId(resourceId);
        ProviderBundle provider = providerService.get(catalogueId, providerId, auth);
        if (provider != null && provider.isActive()) {
            return hasAdminAccess(auth, providerId);
        }
        return false;
    }

    @Override
    public boolean providerIsActive(String id, String catalogueId, boolean published) {
        ProviderBundle providerBundle = providerService.get(id, catalogueId, published);
        return providerBundle.isActive();
    }

    @Override
    public boolean serviceIsActive(String id, String catalogueId, boolean published) {
        ServiceBundle serviceBundle = serviceBundleService.get(id, catalogueId, published);
        return serviceBundle.isActive();
    }

    @Override
    public boolean trainingResourceIsActive(String id, String catalogueId, boolean published) {
        TrainingResourceBundle trainingResourceBundle = trainingResourceService.get(id, catalogueId, published);
        return trainingResourceBundle.isActive();
    }

    @Override
    public boolean guidelineIsActive(String id, String catalogueId, boolean published) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(id, catalogueId, published);
        return interoperabilityRecordBundle.isActive();
    }
    //endregion

    //TODO: refactor this region now that Maintainers became Users
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
