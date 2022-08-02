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
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ServiceException;
import org.mitre.openid.connect.model.DefaultUserInfo;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final PendingResourceService<ServiceBundle> pendingServiceManager;
    private OIDCAuthenticationToken adminAccess;

    @Value("${project.name:}")
    private String projectName;

    @Value("${mail.smtp.from:}")
    private String projectEmail;

    @Autowired
    OIDCSecurityService(ProviderManager providerManager, CatalogueManager catalogueManager,
                        ResourceBundleService<ServiceBundle> resourceBundleService,
                        PendingProviderManager pendingProviderManager, PendingResourceService<ServiceBundle> pendingServiceManager) {
        this.providerManager = providerManager;
        this.catalogueManager = catalogueManager;
        this.resourceBundleService = resourceBundleService;
        this.pendingProviderManager = pendingProviderManager;
        this.pendingServiceManager = pendingServiceManager;

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

    public boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    public boolean isProviderAdmin(Authentication auth, @NotNull String providerId) {
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsProviderAdmin(user, providerId);
    }

    public boolean isProviderAdmin(Authentication auth, @NotNull String providerId, boolean noThrow) {
        if (auth == null && noThrow) {
            return false;
        }
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsProviderAdmin(user, providerId);
    }

    public boolean userIsProviderAdmin(User user, @NotNull String providerId) {
        ProviderBundle registeredProvider;
        try {
            registeredProvider = providerManager.get(providerId);
        } catch (ResourceException e) {
            try {
                registeredProvider = pendingProviderManager.get(providerId);
            } catch (RuntimeException re) {
                return false;
            }
        } catch (RuntimeException e) {
            return false;
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

    public boolean isCatalogueAdmin(Authentication auth, @NotNull String catalogueId) {
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsCatalogueAdmin(user, catalogueId);
    }

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
        return userIsResourceProviderAdmin(user, resourceId);
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
    public boolean isResourceProviderAdmin(Authentication auth, String resourceId, boolean noThrow) {
        if (auth == null && noThrow) {
            return false;
        }
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsResourceProviderAdmin(user, resourceId);
    }

    @Override
    public boolean isResourceProviderAdmin(Authentication auth, ResourceBundle<?> resourceBundle) {
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return false;
        }
        User user = User.of(auth);
        return userIsResourceProviderAdmin(user, resourceBundle);
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
        return userIsResourceProviderAdmin(user, resourceBundle);
    }

    @Override
    public boolean userIsResourceProviderAdmin(User user, ResourceBundle<?> resourceBundle) {
        if (resourceBundle.getPayload().getResourceOrganisation() == null || resourceBundle.getPayload().getResourceOrganisation().equals("")) {
            throw new ValidationException("Resource has no Resource Organisation");
        }
        List<String> allProviders = Collections.singletonList(resourceBundle.getPayload().getResourceOrganisation());
        Optional<List<String>> providers = Optional.of(allProviders);
        return providers
                .get()
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(id -> userIsProviderAdmin(user, id));
    }

    @Override
    public boolean userIsResourceProviderAdmin(@NotNull User user, String resourceId) {
        ResourceBundle<?> resourceBundle;
        try {
            resourceBundle = resourceBundleService.get(resourceId);
        } catch (ResourceException | ResourceNotFoundException e) {
            try {
                resourceBundle = pendingServiceManager.get(resourceId);
            } catch (RuntimeException re) {
                return false;
            }
        } catch (RuntimeException e) {
            return false;
        }
        if (resourceBundle.getPayload().getResourceOrganisation() == null || resourceBundle.getPayload().getResourceOrganisation().equals("")) {
            throw new ValidationException("Resource has no Resource Organisation");
        }
        List<String> allProviders = Collections.singletonList(resourceBundle.getPayload().getResourceOrganisation());
        Optional<List<String>> providers = Optional.of(allProviders);
        return providers
                .get()
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(id -> userIsProviderAdmin(user, id));
    }

    @Override
    public boolean userIsResourceProviderAdmin(@NotNull User user, String resourceId, String catalogueId) {
        ResourceBundle<?> resourceBundle;
        try {
            resourceBundle = resourceBundleService.get(resourceId, catalogueId);
        } catch (ResourceException | ResourceNotFoundException e) {
            try {
                resourceBundle = pendingServiceManager.get(resourceId);
            } catch (RuntimeException re) {
                return false;
            }
        } catch (RuntimeException e) {
            return false;
        }
        if (resourceBundle.getPayload().getResourceOrganisation() == null || resourceBundle.getPayload().getResourceOrganisation().equals("")) {
            throw new ValidationException("Resource has no Resource Organisation");
        }

        List<String> allProviders = Collections.singletonList(resourceBundle.getPayload().getResourceOrganisation());
        Optional<List<String>> providers = Optional.of(allProviders);
        return providers
                .get()
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(id -> userIsProviderAdmin(user, id));
    }

    public boolean providerCanAddResources(Authentication auth, String resourceId) {
        return this.providerCanAddResources(auth, resourceBundleService.get(resourceId));
    }

    public boolean providerCanAddResources(Authentication auth, ResourceBundle<?> resourceBundle) {
        List<String> providerIds = Collections.singletonList(resourceBundle.getPayload().getResourceOrganisation());
        for (String providerId : providerIds) {
            ProviderBundle provider = providerManager.get(providerId);
            if (isProviderAdmin(auth, provider.getId())) {
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

    public boolean providerIsActiveAndUserIsAdmin(Authentication auth, String resourceId) {
        ResourceBundle<?> resourceBundle = resourceBundleService.get(resourceId);
        List<String> providerIds = Collections.singletonList(resourceBundle.getPayload().getResourceOrganisation());
        for (String providerId : providerIds) {
            ProviderBundle provider = providerManager.get(providerId);
            if (provider != null && provider.isActive()) {
                if (isProviderAdmin(auth, providerId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean resourceIsActive(String resourceId) {
        ResourceBundle<?> resourceBundle = resourceBundleService.get(resourceId);
        return resourceBundle.isActive();
    }

    public boolean resourceIsActive(String resourceId, String catalogueId) {
        ResourceBundle<?> resourceBundle = resourceBundleService.get(resourceId, catalogueId);
        return resourceBundle.isActive();
    }
}
