package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.manager.CatalogueManager;
import gr.uoa.di.madgik.resourcecatalogue.manager.DraftProviderManager;
import gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.*;

@Service("securityService")
public class OIDCSecurityService implements SecurityService {

    private final ProviderManager providerManager;
    private final CatalogueManager catalogueManager;
    private final DraftProviderManager pendingProviderManager;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final DraftResourceService<ServiceBundle> pendingServiceManager;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final Authentication adminAccess = new AdminAuthentication();

    @Value("${catalogue.id}")
    private String catalogueId;

    OIDCSecurityService(@Lazy ProviderManager providerManager, CatalogueManager catalogueManager,
                        @Lazy ServiceBundleService<ServiceBundle> serviceBundleService,
                        @Lazy TrainingResourceService trainingResourceService,
                        @Lazy DraftProviderManager pendingProviderManager,
                        @Lazy DraftResourceService<ServiceBundle> pendingServiceManager,
                        @Lazy InteroperabilityRecordService interoperabilityRecordService) {
        this.providerManager = providerManager;
        this.catalogueManager = catalogueManager;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.pendingProviderManager = pendingProviderManager;
        this.pendingServiceManager = pendingServiceManager;
        this.interoperabilityRecordService = interoperabilityRecordService;
    }

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

    @Override
    public boolean userIsProviderAdmin(User user, ProviderBundle providerBundle) {
        return userIsProviderAdmin(user, providerBundle.getId(), providerBundle.getPayload().getCatalogueId());
    }

    @Override
    public boolean isProviderAdmin(Authentication auth, @NotNull String providerId) {
        return isProviderAdmin(auth, providerId, catalogueId);
    }

    @Override
    public boolean isProviderAdmin(Authentication auth, @NotNull String providerId, @NotNull String catalogueId) {
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsProviderAdmin(user, providerId, catalogueId);
    }

    @Override
    public boolean isProviderAdmin(Authentication auth, @NotNull String providerId, boolean noThrow) {
        return isProviderAdmin(auth, providerId, catalogueId, noThrow);
    }

    @Override
    public boolean isProviderAdmin(Authentication auth, @NotNull String providerId, @NotNull String catalogueId, boolean noThrow) {
        if (noThrow) {
            return true;
        }
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsProviderAdmin(user, providerId, catalogueId);
    }

    @Override
    public boolean userIsProviderAdmin(User user, @NotNull String providerId, @NotNull String catalogueId) {
        ProviderBundle registeredProvider;
        try {
            registeredProvider = providerManager.get(catalogueId, providerId, adminAccess);
        } catch (ResourceException | ResourceNotFoundException e) {
            try {
                registeredProvider = pendingProviderManager.get(providerId);
            } catch (RuntimeException re) {
                return false;
            }
        }
        if (registeredProvider == null) {
            throw new ResourceNotFoundException("Provider with id '" + providerId + "' does not exist.");
        }
        if (registeredProvider.getProvider().getUsers() == null) {
            return false;
        }
        return registeredProvider.getProvider().getUsers()
                .parallelStream()
                .filter(Objects::nonNull)
                .anyMatch(u -> {
                    if (u.getId() != null) {
                        if (u.getEmail() != null) {
                            return u.getId().equals(user.getId())
                                    || u.getEmail().equalsIgnoreCase(user.getEmail());
                        }
                        return u.getId().equals(user.getId());
                    }
                    return u.getEmail().equalsIgnoreCase(user.getEmail());
                });
    }

    @Override
    public boolean isCatalogueAdmin(Authentication auth, @NotNull String catalogueId) {
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsCatalogueAdmin(user, catalogueId);
    }

    @Override
    public boolean isCatalogueAdmin(Authentication auth, @NotNull String catalogueId, boolean noThrow) {
        if (auth == null && noThrow) {
            return false;
        }
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsCatalogueAdmin(user, catalogueId);
    }

    @Override
    public boolean userIsCatalogueAdmin(User user, @NotNull String catalogueId) {
        CatalogueBundle registeredCatalogue;
        try {
            registeredCatalogue = catalogueManager.get(catalogueId);
        } catch (RuntimeException e) {
            return false;
        }
        if (registeredCatalogue == null) {
            throw new ResourceNotFoundException("Catalogue with id '" + catalogueId + "' does not exist.");
        }
        if (registeredCatalogue.getCatalogue().getUsers() == null) {
            return false;
        }
        return registeredCatalogue.getCatalogue().getUsers()
                .parallelStream()
                .filter(Objects::nonNull)
                .anyMatch(u -> {
                    if (u.getId() != null) {
                        if (u.getEmail() != null) {
                            return u.getId().equals(user.getId())
                                    || u.getEmail().equalsIgnoreCase(user.getEmail());
                        }
                        return u.getId().equals(user.getId());
                    }
                    return u.getEmail().equalsIgnoreCase(user.getEmail());
                });
    }

    @Override
    public boolean isResourceProviderAdmin(Authentication auth, String resourceId) {
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsResourceProviderAdmin(user, resourceId, catalogueId);
    }

    @Override
    public boolean isResourceProviderAdmin(Authentication auth, String resourceId, String catalogueId) {
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsResourceProviderAdmin(user, resourceId, catalogueId);
    }

    @Override
    public <T extends gr.uoa.di.madgik.resourcecatalogue.domain.Service> boolean isResourceProviderAdmin(Authentication auth, T service) {
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsResourceProviderAdmin(user, service.getId(), service.getCatalogueId());
    }

    public boolean isResourceProviderAdmin(Authentication auth, TrainingResource trainingResource) {
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsResourceProviderAdmin(user, trainingResource.getId(), trainingResource.getCatalogueId());
    }

    public boolean isResourceProviderAdmin(Authentication auth, InteroperabilityRecord interoperabilityRecord) {
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsResourceProviderAdmin(user, interoperabilityRecord.getId(), interoperabilityRecord.getCatalogueId());
    }

    @Override
    public boolean isResourceProviderAdmin(Authentication auth, ServiceBundle serviceBundle, boolean noThrow) {
        if (auth == null && noThrow) {
            return false;
        }
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsResourceProviderAdmin(user, serviceBundle.getId(), serviceBundle.getService().getCatalogueId());
    }

    @Override
    public boolean userIsResourceProviderAdmin(@NotNull User user, String resourceId, String catalogueId) {
        ServiceBundle serviceBundle;
        TrainingResourceBundle trainingResourceBundle = new TrainingResourceBundle();
        InteroperabilityRecordBundle interoperabilityRecordBundle = new InteroperabilityRecordBundle();
        try {
            serviceBundle = serviceBundleService.getOrElseReturnNull(resourceId, catalogueId);
            if (serviceBundle == null) {
                trainingResourceBundle = trainingResourceService.getOrElseReturnNull(resourceId, catalogueId);
            }
            if (serviceBundle == null && trainingResourceBundle == null) {
                interoperabilityRecordBundle = interoperabilityRecordService.getOrElseReturnNull(resourceId, catalogueId);
            }
            if (serviceBundle == null && trainingResourceBundle == null && interoperabilityRecordBundle == null) {
                serviceBundle = pendingServiceManager.get(resourceId);
            }
        } catch (RuntimeException e) {
            return false;
        }
        List<String> allProviders = new ArrayList<>();
        String catalogue;
        if (serviceBundle != null) {
            if (serviceBundle.getService().getResourceOrganisation() == null || serviceBundle.getService().getResourceOrganisation().equals("")) {
                throw new ValidationException("Resource has no Resource Organisation");
            }
            allProviders.add(serviceBundle.getService().getResourceOrganisation());
            if (serviceBundle.getService().getResourceProviders() != null) {
                allProviders.addAll(serviceBundle.getService().getResourceProviders());
            }
            catalogue = serviceBundle.getService().getCatalogueId();
        } else if (trainingResourceBundle != null) {
            if (trainingResourceBundle.getTrainingResource().getResourceOrganisation() == null || trainingResourceBundle.getTrainingResource().getResourceOrganisation().equals("")) {
                throw new ValidationException("Training Resource has no Resource Organisation");
            }
            allProviders.add(trainingResourceBundle.getTrainingResource().getResourceOrganisation());
            if (trainingResourceBundle.getTrainingResource().getResourceProviders() != null) {
                allProviders.addAll(trainingResourceBundle.getTrainingResource().getResourceProviders());
            }
            catalogue = trainingResourceBundle.getTrainingResource().getCatalogueId();
        } else {
            if (interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId() == null || interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId().equals("")) {
                throw new ValidationException("Interoperability Record has no Provider ID");
            }
            allProviders.add(interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId());
            catalogue = interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId();
        }
        return allProviders
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(id -> userIsProviderAdmin(user, id, catalogue));
    }

    @Override
    public boolean providerCanAddResources(Authentication auth, String resourceId, String catalogueId) {
        return true;
    }

    @Override
    public boolean providerCanAddResources(Authentication auth, ServiceBundle serviceBundle) {
        String providerId = serviceBundle.getService().getResourceOrganisation();
        if (serviceBundle.getService().getCatalogueId() == null || serviceBundle.getService().getCatalogueId().equals("")) {
            serviceBundle.getService().setCatalogueId(catalogueId);
        }
        ProviderBundle provider = providerManager.get(serviceBundle.getService().getCatalogueId(), providerId, auth);
        if (isProviderAdmin(auth, provider.getId(), provider.getPayload().getCatalogueId())) {
            if (provider.getStatus() == null) {
                throw new ServiceException("Provider status field is null");
            }
            if (provider.isActive() && provider.getStatus().equals("approved provider")) {
                return true;
            } else if (provider.getTemplateStatus().equals("no template status")) {
                FacetFilter ff = new FacetFilter();
                ff.addFilter("resource_organisation", provider.getId());
                if (serviceBundleService.getAll(ff, getAdminAccess()).getResults().isEmpty()) {
                    return true;
                }
                throw new ResourceException("You have already created a Service Template.", HttpStatus.CONFLICT);
            }
        }
        return false;
    }

    @Override
    public <T extends gr.uoa.di.madgik.resourcecatalogue.domain.Service> boolean providerCanAddResources(Authentication auth, T service) {
        List<String> providerIds = Collections.singletonList(service.getResourceOrganisation());
        if (service.getCatalogueId() == null || service.getCatalogueId().equals("")) {
            service.setCatalogueId(catalogueId);
        }
        for (String providerId : providerIds) {
            ProviderBundle provider = providerManager.get(service.getCatalogueId(), providerId, auth);
            if (isProviderAdmin(auth, provider.getId(), service.getCatalogueId())) {
                if (provider.getStatus() == null) {
                    throw new ServiceException("Provider status field is null");
                }
                if (provider.isActive() && provider.getStatus().equals("approved provider")) {
                    return true;
                } else if (provider.getTemplateStatus().equals("no template status")) {
                    FacetFilter ff = new FacetFilter();
                    ff.addFilter("resource_organisation", provider.getId());
                    if (serviceBundleService.getAll(ff, getAdminAccess()).getResults().isEmpty()) {
                        return true;
                    }
                    throw new ResourceException("You have already created a Service Template.", HttpStatus.CONFLICT);
                }
            }
        }
        return false;
    }

    public boolean providerCanAddResources(Authentication auth, TrainingResource trainingResource) {
        List<String> providerIds = Collections.singletonList(trainingResource.getResourceOrganisation());
        if (trainingResource.getCatalogueId() == null || trainingResource.getCatalogueId().equals("")) {
            trainingResource.setCatalogueId(catalogueId);
        }
        for (String providerId : providerIds) {
            ProviderBundle provider = providerManager.get(trainingResource.getCatalogueId(), providerId, auth);
            if (isProviderAdmin(auth, provider.getId(), trainingResource.getCatalogueId())) {
                if (provider.getStatus() == null) {
                    throw new ServiceException("Provider status field is null");
                }
                if (provider.isActive() && provider.getStatus().equals("approved provider")) {
                    return true;
                } else if (provider.getTemplateStatus().equals("no template status")) {
                    FacetFilter ff = new FacetFilter();
                    ff.addFilter("resource_organisation", provider.getId());
                    if (serviceBundleService.getAll(ff, getAdminAccess()).getResults().isEmpty()) {
                        return true;
                    }
                    throw new ResourceException("You have already created a Service Template.", HttpStatus.CONFLICT);
                }
            }
        }
        return false;
    }

    public boolean providerCanAddResources(Authentication auth, InteroperabilityRecord interoperabilityRecord) {
        if (interoperabilityRecord.getCatalogueId() == null || interoperabilityRecord.getCatalogueId().equals("")) {
            interoperabilityRecord.setCatalogueId(catalogueId);
        }
        ProviderBundle provider = providerManager.get(interoperabilityRecord.getCatalogueId(), interoperabilityRecord.getProviderId(), auth);
        if (isProviderAdmin(auth, provider.getId(), interoperabilityRecord.getCatalogueId())) {
            if (provider.getStatus() == null) {
                throw new ServiceException("Provider status field is null");
            }
            return provider.isActive() && provider.getStatus().equals("approved provider");
        }
        return false;
    }

    @Override
    public boolean providerIsActiveAndUserIsAdmin(Authentication auth, String resourceId) {
        return providerIsActiveAndUserIsAdmin(auth, resourceId, catalogueId);
    }

    @Override
    public boolean providerIsActiveAndUserIsAdmin(Authentication auth, String resourceId, String catalogueId) {
        ServiceBundle serviceBundle;
        TrainingResourceBundle trainingResourceBundle;
        InteroperabilityRecordBundle interoperabilityRecordBundle;
        List<String> providerIds;
        try {
            serviceBundle = serviceBundleService.get(resourceId, catalogueId);
            providerIds = Collections.singletonList(serviceBundle.getService().getResourceOrganisation());
        } catch (ResourceNotFoundException e) {
            try {
                trainingResourceBundle = trainingResourceService.get(resourceId, catalogueId);
                providerIds = Collections.singletonList(trainingResourceBundle.getPayload().getResourceOrganisation());
            } catch (ResourceNotFoundException j) {
                interoperabilityRecordBundle = interoperabilityRecordService.get(resourceId, catalogueId);
                providerIds = Collections.singletonList(interoperabilityRecordBundle.getPayload().getProviderId());
            }
        }
        for (String providerId : providerIds) {
            ProviderBundle provider = providerManager.get(catalogueId, providerId, auth);
            if (provider != null && provider.isActive()) {
                if (isProviderAdmin(auth, providerId, provider.getPayload().getCatalogueId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean resourceIsActive(String resourceId, String catalogueId) {
        ServiceBundle serviceBundle = serviceBundleService.get(resourceId, catalogueId);
        return serviceBundle.isActive();
    }

    public boolean trainingResourceIsActive(String resourceId, String catalogueId) {
        TrainingResourceBundle trainingResourceBundle = trainingResourceService.get(resourceId, catalogueId);
        return trainingResourceBundle.isActive();
    }

    static class AdminAuthentication implements Authentication {

        private final String name = "Administrator";
        private boolean authenticated = true;

        private AdminAuthentication() {
            // no-arg constructor
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            Map<String, Object> info = new HashMap<>();
            info.put("email", "admin@resource-catalogue.gr");
            info.put("givenName", "Admin");
            info.put("familyName", "Administrator");
            return new OidcUserInfo(info);
        }

        @Override
        public Object getPrincipal() {
            return name;
        }

        @Override
        public boolean isAuthenticated() {
            return authenticated;
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            this.authenticated = isAuthenticated;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
