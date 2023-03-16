package eu.einfracentral.service;

import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.ResourceBundle;
import eu.einfracentral.domain.User;
import org.springframework.security.core.Authentication;

import javax.validation.constraints.NotNull;

public interface SecurityService {

    Authentication getAdminAccess();

    String getRoleName(Authentication authentication);

    boolean hasRole(Authentication auth, String role);

//    boolean isProviderAdmin(Authentication auth, @NotNull String providerId);
    boolean isProviderAdmin(Authentication auth, @NotNull String providerId);
    boolean isProviderAdmin(Authentication auth, @NotNull String providerId, @NotNull String catalogueId);
    boolean isProviderAdmin(Authentication auth, @NotNull String providerId, boolean noThrow);
    boolean isProviderAdmin(Authentication auth, @NotNull String providerId, @NotNull String catalogueId, boolean noThrow);

    boolean isCatalogueAdmin(Authentication auth, @NotNull String catalogueId);

    boolean isCatalogueAdmin(Authentication auth, @NotNull String catalogueId, boolean noThrow);

    //    boolean isResourceProviderAdmin(Authentication auth, String resourceId);

    boolean isResourceProviderAdmin(Authentication auth, String resourceId);
    boolean isResourceProviderAdmin(Authentication auth, String resourceId, String catalogueId);

//    boolean isResourceProviderAdmin(Authentication auth, String resourceId, boolean noThrow);

    boolean isResourceProviderAdmin(Authentication auth, ResourceBundle<?> resourceBundle, boolean noThrow);
    <T extends eu.einfracentral.domain.Service> boolean isResourceProviderAdmin(Authentication auth, T service);

    boolean userIsProviderAdmin(@NotNull User user, @NotNull String providerId, @NotNull String catalogueId);

    boolean userIsProviderAdmin(@NotNull User user, @NotNull ProviderBundle providerBundle);

    boolean userIsCatalogueAdmin(@NotNull User user, @NotNull String catalogueId);

//    boolean userIsResourceProviderAdmin(User user, ResourceBundle<?> resourceBundle);

//    boolean userIsResourceProviderAdmin(User user, String resourceId);

    boolean userIsResourceProviderAdmin(User user, String resourceId, String catalogueId);

    boolean providerCanAddResources(Authentication auth, String resourceId, String catalogueId);
    boolean providerCanAddResources(Authentication auth, ResourceBundle<?> resourceBundle);
    <T extends eu.einfracentral.domain.Service> boolean providerCanAddResources(Authentication auth, T service);

    boolean providerIsActiveAndUserIsAdmin(Authentication auth, String resourceId);
    boolean providerIsActiveAndUserIsAdmin(Authentication auth, String resourceId, String catalogueId);

    boolean resourceIsActive(String resourceId, String catalogueId);
    boolean datasourceIsActive(String resourceId, String catalogueId);
    boolean resourceOrDatasourceIsActive(String resourceId, String catalogueId);
    boolean trainingResourceIsActive(String resourceId, String catalogueId);
}
