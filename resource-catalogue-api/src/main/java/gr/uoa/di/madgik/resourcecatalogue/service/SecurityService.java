package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecord;
import gr.uoa.di.madgik.resourcecatalogue.domain.Service;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResource;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;

public interface SecurityService {

    /**
     * @return Admin Authentication
     */
    Authentication getAdminAccess();

    /**
     * @param authentication authentication
     * @return role name
     */
    String getRoleName(Authentication authentication);

    /**
     * @param auth authentication
     * @param role from the predefined roles
     * @return False if authentication is null, True otherwise
     */
    boolean hasRole(Authentication auth, String role);

    /**
     * @param auth       Authentication
     * @param providerId Provider id
     * @return True if the authenticated user is a Provider Admin
     */
    boolean isProviderAdmin(Authentication auth, @NotNull String providerId);

    /**
     * @param auth       Authentication
     * @param providerId Provider id
     * @param noThrow    False to throw error, True to not throw error
     * @return TTrue if the authenticated user is a Provider Admin
     */
    boolean isProviderAdmin(Authentication auth, @NotNull String providerId, boolean noThrow);

    /**
     * @param user User
     * @param id   Provider or Catalogue id
     * @return True if the authenticated user is a Provider Admin
     */
    boolean userIsProviderAdmin(@NotNull User user, @NotNull String id);

    /**
     * @param auth       Authentication
     * @param resourceId resource id
     * @return True if the authenticated user is a Provider Admin for the provider where the resource is registered.
     */
    boolean isResourceProviderAdmin(Authentication auth, String resourceId);

    /**
     * @param user       User
     * @param resourceId resource id
     * @return True if the authenticated user is a Provider Admin for the provider where the resource is registered.
     */
    boolean userIsResourceProviderAdmin(User user, String resourceId);

    /**
     * @param auth    Authentication
     * @param service Service
     * @return True if provider where the service is registered is active and approved
     */
    boolean providerCanAddResources(Authentication auth, Service service);

    /**
     * @param auth             Authentication
     * @param trainingResource Training Resource
     * @return True if provider where the training resource is registered is active and approved
     */
    boolean providerCanAddResources(Authentication auth, TrainingResource trainingResource);

    /**
     * @param auth                   Authentication
     * @param interoperabilityRecord Interoperability Record
     * @return True if provider where the interoperability record is registered is active and approved
     */
    boolean providerCanAddResources(Authentication auth, InteroperabilityRecord interoperabilityRecord);

    /**
     * @param auth       Authentication
     * @param resourceId resource id
     * @return True if the authenticated user is a Provider Admin for the provider where the resource is registered
     * and provider is active
     */
    boolean providerIsActiveAndUserIsAdmin(Authentication auth, String resourceId);

    /**
     * @param id service id
     * @return True if service is active
     */
    boolean serviceIsActive(String id);

    /**
     * @param id training resource id
     * @return True if training resource is active
     */
    boolean trainingResourceIsActive(String id);

    /**
     * @param id interoperability record id
     * @return True if interoperability record (guideline) is active
     */
    boolean guidelineIsActive(String id);
}
