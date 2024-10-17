package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Service;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;

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
     * @param noThrow
     * @return
     */
    boolean isProviderAdmin(Authentication auth, @NotNull String providerId, boolean noThrow);

    /**
     * @param auth
     * @param resourceId
     * @return
     */
    boolean isResourceProviderAdmin(Authentication auth, String resourceId);

    /**
     * @param user
     * @param id
     * @return
     */
    boolean userIsProviderAdmin(@NotNull User user, @NotNull String id);

    /**
     * @param user
     * @param resourceId
     * @return
     */
    boolean userIsResourceProviderAdmin(User user, String resourceId);

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
     * @param resourceId
     * @return
     */
    boolean serviceIsActive(String resourceId);

    /**
     * @param resourceId
     * @return
     */
    boolean trainingResourceIsActive(String resourceId);

    /**
     * @param resourceId
     * @return
     */
    boolean guidelineIsActive(String resourceId);
}
