package eu.einfracentral.service;

import eu.einfracentral.domain.ResourceBundle;
import eu.einfracentral.domain.User;
import org.springframework.security.core.Authentication;

import javax.validation.constraints.NotNull;

public interface SecurityService {

    Authentication getAdminAccess();

    String getRoleName(Authentication authentication);

    boolean hasRole(Authentication auth, String role);

    boolean isProviderAdmin(Authentication auth, @NotNull String providerId);

    boolean isProviderAdmin(Authentication auth, @NotNull String providerId, boolean noThrow);

    boolean userIsProviderAdmin(@NotNull User user, @NotNull String providerId);

    boolean isCatalogueAdmin(Authentication auth, @NotNull String catalogueId);

    boolean isCatalogueAdmin(Authentication auth, @NotNull String catalogueId, boolean noThrow);

    boolean userIsCatalogueAdmin(@NotNull User user, @NotNull String catalogueId);

    boolean isResourceProviderAdmin(Authentication auth, String resourceId);

    boolean isResourceProviderAdmin(Authentication auth, String resourceId, String catalogueId);

    boolean isResourceProviderAdmin(Authentication auth, String resourceId, boolean noThrow);

    boolean isResourceProviderAdmin(Authentication auth, ResourceBundle<?> resourceBundle);

    boolean isResourceProviderAdmin(Authentication auth, ResourceBundle<?> resourceBundle, boolean noThrow);

    boolean userIsResourceProviderAdmin(User user, ResourceBundle<?> resourceBundle);

    boolean userIsResourceProviderAdmin(User user, String resourceId);

    boolean userIsResourceProviderAdmin(User user, String resourceId, String catalogueId);

    boolean providerCanAddResources(Authentication auth, ResourceBundle<?> resourceBundle);

    boolean providerIsActiveAndUserIsAdmin(Authentication auth, String resourceId);

    boolean resourceIsActive(String resourceId);
}
