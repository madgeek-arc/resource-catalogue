package eu.einfracentral.service;

import eu.einfracentral.domain.*;
import eu.einfracentral.domain.ResourceBundle;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.manager.CatalogueManager;
import eu.einfracentral.registry.manager.PendingProviderManager;
import eu.einfracentral.registry.manager.ProviderManager;
import eu.einfracentral.registry.service.DatasourceService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.registry.service.TrainingResourceService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ServiceException;
import org.mitre.openid.connect.model.DefaultUserInfo;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.*;

@Service("securityService")
public class OIDCSecurityService implements SecurityService {

    private final ProviderManager providerManager;
    private final CatalogueManager catalogueManager;
    private final PendingProviderManager pendingProviderManager;
    private final ResourceBundleService<ServiceBundle> resourceBundleService;
    private final DatasourceService<DatasourceBundle> datasourceService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final PendingResourceService<ServiceBundle> pendingServiceManager;
    private final PendingResourceService<DatasourceBundle> pendingDatasourceManager;
    private OIDCAuthenticationToken adminAccess;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Value("${project.name:}")
    private String projectName;

    @Value("${mail.smtp.from:}")
    private String projectEmail;

    @Autowired
    OIDCSecurityService(ProviderManager providerManager, CatalogueManager catalogueManager,
                        ResourceBundleService<ServiceBundle> resourceBundleService,
                        @Lazy DatasourceService<DatasourceBundle> datasourceService,
                        @Lazy TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                        @Lazy PendingProviderManager pendingProviderManager,
                        @Lazy PendingResourceService<ServiceBundle> pendingServiceManager,
                        @Lazy PendingResourceService<DatasourceBundle> pendingDatasourceManager) {
        this.providerManager = providerManager;
        this.catalogueManager = catalogueManager;
        this.resourceBundleService = resourceBundleService;
        this.datasourceService = datasourceService;
        this.trainingResourceService = trainingResourceService;
        this.pendingProviderManager = pendingProviderManager;
        this.pendingServiceManager = pendingServiceManager;
        this.pendingDatasourceManager = pendingDatasourceManager;

        // create admin access
        List<GrantedAuthority> roles = new ArrayList<>();
        roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        DefaultUserInfo userInfo = new DefaultUserInfo();
        userInfo.setEmail(projectEmail);
        userInfo.setId(1L);
        userInfo.setGivenName(projectName);
        userInfo.setFamilyName("");
        adminAccess = new OIDCAuthenticationToken("", "", userInfo, roles, null, "", "");
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
        return isProviderAdmin(auth, providerId, catalogueName);
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
        return isProviderAdmin(auth, providerId, catalogueName, noThrow);
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
        return userIsResourceProviderAdmin(user, resourceId, catalogueName);
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
    public <T extends eu.einfracentral.domain.Service> boolean isResourceProviderAdmin(Authentication auth, T service) {
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

    @Override
    public boolean isResourceProviderAdmin(Authentication auth, ResourceBundle<?> resourceBundle, boolean noThrow) {
        if (auth == null && noThrow) {
            return false;
        }
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsResourceProviderAdmin(user, resourceBundle.getId(), resourceBundle.getPayload().getCatalogueId());
    }

    @Override
    public boolean userIsResourceProviderAdmin(@NotNull User user, String resourceId, String catalogueId) {
        ResourceBundle<?> resourceBundle;
        TrainingResourceBundle trainingResourceBundle = new TrainingResourceBundle();
        try {
            resourceBundle = resourceBundleService.getOrElseReturnNull(resourceId, catalogueId);
            if (resourceBundle == null){
                resourceBundle = datasourceService.getOrElseReturnNull(resourceId, catalogueId);
            }
            if (resourceBundle == null){
                trainingResourceBundle = trainingResourceService.getOrElseReturnNull(resourceId, catalogueId);
            }
            if (resourceBundle == null && trainingResourceBundle == null){
                resourceBundle = pendingServiceManager.get(resourceId);
            }
        } catch (ResourceException | ResourceNotFoundException e) {
            try {
                resourceBundle = pendingDatasourceManager.get(resourceId);
            } catch (RuntimeException re) {
                return false; //TODO: try/catch pendingTrainingResourceManager
            }
        } catch (RuntimeException e) {
            return false;
        }
        List<String> allProviders;
        String catalogue;
        if (resourceBundle != null){
            if (resourceBundle.getPayload().getResourceOrganisation() == null || resourceBundle.getPayload().getResourceOrganisation().equals("")) {
                throw new ValidationException("Resource has no Resource Organisation");
            }
            allProviders = new ArrayList<>(resourceBundle.getPayload().getResourceProviders());
            allProviders.add(resourceBundle.getPayload().getResourceOrganisation());
            catalogue = resourceBundle.getPayload().getCatalogueId();
        } else{
            if (trainingResourceBundle.getTrainingResource().getResourceOrganisation() == null || trainingResourceBundle.getTrainingResource().getResourceOrganisation().equals("")) {
                throw new ValidationException("Resource has no Resource Organisation");
            }
            allProviders = new ArrayList<>(trainingResourceBundle.getTrainingResource().getResourceProviders());
            allProviders.add(trainingResourceBundle.getTrainingResource().getResourceOrganisation());
            catalogue = trainingResourceBundle.getTrainingResource().getCatalogueId();
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
    public boolean providerCanAddResources(Authentication auth, ResourceBundle<?> resourceBundle) {
        String providerId = resourceBundle.getPayload().getResourceOrganisation();
        if (resourceBundle.getPayload().getCatalogueId() == null || resourceBundle.getPayload().getCatalogueId().equals("")){
            resourceBundle.getPayload().setCatalogueId(catalogueName);
        }
        ProviderBundle provider = providerManager.get(resourceBundle.getPayload().getCatalogueId(), providerId, auth);
        if (isProviderAdmin(auth, provider.getId(), provider.getPayload().getCatalogueId())) {
            if (provider.getStatus() == null) {
                throw new ServiceException("Provider status field is null");
            }
            if (provider.isActive() && provider.getStatus().equals("approved provider")) {
                return true;
            } else if (provider.getTemplateStatus().equals("no template status")) {
                FacetFilter ff = new FacetFilter();
                ff.addFilter("resource_organisation", provider.getId());
                if (resourceBundleService.getAll(ff, getAdminAccess()).getResults().isEmpty()) {
                    return true;
                }
                throw new ResourceException("You have already created a Service Template.", HttpStatus.CONFLICT);
            }
        }
        return false;
    }

    @Override
    public <T extends eu.einfracentral.domain.Service> boolean providerCanAddResources(Authentication auth, T service) {
        List<String> providerIds = Collections.singletonList(service.getResourceOrganisation());
        if (service.getCatalogueId() == null || service.getCatalogueId().equals("")){
            service.setCatalogueId(catalogueName);
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
                    if (resourceBundleService.getAll(ff, getAdminAccess()).getResults().isEmpty()) {
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
        if (trainingResource.getCatalogueId() == null || trainingResource.getCatalogueId().equals("")){
            trainingResource.setCatalogueId(catalogueName);
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
                    if (resourceBundleService.getAll(ff, getAdminAccess()).getResults().isEmpty()) {
                        return true;
                    }
                    throw new ResourceException("You have already created a Service Template.", HttpStatus.CONFLICT);
                }
            }
        }
        return false;
    }

    @Override
    public boolean providerIsActiveAndUserIsAdmin(Authentication auth, String resourceId){
        return providerIsActiveAndUserIsAdmin(auth, resourceId, catalogueName);
    }
    @Override
    public boolean providerIsActiveAndUserIsAdmin(Authentication auth, String resourceId, String catalogueId) {
        ResourceBundle<?> resourceBundle;
        TrainingResourceBundle trainingResourceBundle;
        List<String> providerIds;
        try{
            resourceBundle = resourceBundleService.get(resourceId, catalogueId);
            providerIds = Collections.singletonList(resourceBundle.getPayload().getResourceOrganisation());
        } catch (ResourceNotFoundException e) {
            trainingResourceBundle = trainingResourceService.get(resourceId, catalogueId);
            providerIds = Collections.singletonList(trainingResourceBundle.getPayload().getResourceOrganisation());
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
        ResourceBundle<?> resourceBundle = resourceBundleService.get(resourceId, catalogueId);
        return resourceBundle.isActive();
    }

    public boolean trainingResourceIsActive(String resourceId, String catalogueId) {
        TrainingResourceBundle trainingResourceBundle = trainingResourceService.get(resourceId, catalogueId);
        return trainingResourceBundle.isActive();
    }
}
