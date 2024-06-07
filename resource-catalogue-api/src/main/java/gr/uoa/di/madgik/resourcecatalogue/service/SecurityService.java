package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Service;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import org.springframework.security.core.Authentication;

import javax.validation.constraints.NotNull;

public interface SecurityService {

    /**
     * @return
     */
    Authentication getAdminAccess();

    /**
     * @param authentication
     * @return
     */
    String getRoleName(Authentication authentication);

    /**
     * @param auth
     * @param role
     * @return
     */
    boolean hasRole(Authentication auth, String role);

    /**
     * @param auth
     * @param providerId
     * @return
     */
    boolean isProviderAdmin(Authentication auth, @NotNull String providerId);

    /**
     * @param auth
     * @param providerId
     * @param catalogueId
     * @return
     */
    boolean isProviderAdmin(Authentication auth, @NotNull String providerId, @NotNull String catalogueId);

    /**
     * @param auth
     * @param providerId
     * @param noThrow
     * @return
     */
    boolean isProviderAdmin(Authentication auth, @NotNull String providerId, boolean noThrow);

    /**
     * @param auth
     * @param providerId
     * @param catalogueId
     * @param noThrow
     * @return
     */
    boolean isProviderAdmin(Authentication auth, @NotNull String providerId, @NotNull String catalogueId, boolean noThrow);

    /**
     * @param auth
     * @param catalogueId
     * @return
     */
    boolean isCatalogueAdmin(Authentication auth, @NotNull String catalogueId);

    /**
     * @param auth
     * @param catalogueId
     * @param noThrow
     * @return
     */
    boolean isCatalogueAdmin(Authentication auth, @NotNull String catalogueId, boolean noThrow);

    /**
     * @param auth
     * @param resourceId
     * @return
     */
    boolean isResourceProviderAdmin(Authentication auth, String resourceId);

    /**
     * @param auth
     * @param resourceId
     * @param catalogueId
     * @return
     */
    boolean isResourceProviderAdmin(Authentication auth, String resourceId, String catalogueId);

    /**
     * @param auth
     * @param serviceBundle
     * @param noThrow
     * @return
     */
    boolean isResourceProviderAdmin(Authentication auth, ServiceBundle serviceBundle, boolean noThrow);

    /**
     * @param auth
     * @param service
     * @param <T>
     * @return
     */
    <T extends Service> boolean isResourceProviderAdmin(Authentication auth, T service);

    /**
     * @param user
     * @param providerId
     * @param catalogueId
     * @return
     */
    boolean userIsProviderAdmin(@NotNull User user, @NotNull String providerId, @NotNull String catalogueId);

    /**
     * @param user
     * @param providerBundle
     * @return
     */
    boolean userIsProviderAdmin(@NotNull User user, @NotNull ProviderBundle providerBundle);

    /**
     * @param user
     * @param catalogueId
     * @return
     */
    boolean userIsCatalogueAdmin(@NotNull User user, @NotNull String catalogueId);

    /**
     * @param user
     * @param resourceId
     * @param catalogueId
     * @return
     */
    boolean userIsResourceProviderAdmin(User user, String resourceId, String catalogueId);

    /**
     * @param auth
     * @param resourceId
     * @param catalogueId
     * @return
     */
    boolean providerCanAddResources(Authentication auth, String resourceId, String catalogueId);

    /**
     * @param auth
     * @param serviceBundle
     * @return
     */
    boolean providerCanAddResources(Authentication auth, ServiceBundle serviceBundle);

    /**
     * @param auth
     * @param service
     * @param <T>
     * @return
     */
    <T extends Service> boolean providerCanAddResources(Authentication auth, T service);

    /**
     * @param auth
     * @param resourceId
     * @return
     */
    boolean providerIsActiveAndUserIsAdmin(Authentication auth, String resourceId);

    /**
     * @param auth
     * @param resourceId
     * @param catalogueId
     * @return
     */
    boolean providerIsActiveAndUserIsAdmin(Authentication auth, String resourceId, String catalogueId);

    /**
     * @param resourceId
     * @param catalogueId
     * @return
     */
    boolean resourceIsActive(String resourceId, String catalogueId);

    /**
     * @param resourceId
     * @param catalogueId
     * @return
     */
    boolean trainingResourceIsActive(String resourceId, String catalogueId);
}
