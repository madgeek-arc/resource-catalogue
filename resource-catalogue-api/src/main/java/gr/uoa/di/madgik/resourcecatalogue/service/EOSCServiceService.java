package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface EOSCServiceService<T extends Bundle> {

    /**
     *
     * @param providerId  Provider ID
     * @param catalogueId Catalogue ID
     * @param quantity    Quantity to be fetched
     * @param auth        Authentication
     * @return {@link Paging <T>}
     */
    Paging<T> getAllEOSCServicesOfAProvider(String providerId, String catalogueId, int quantity, Authentication auth);

    /**
     * @param authentication Authentication
     * @param ids            List of Service IDs
     * @return the list of matching resources.
     */
    List<T> getByIds(Authentication authentication, String... ids);

    /**
     * Get an EOSC Provider's Service Template, if exists, else return null
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link Bundle}
     */
    Bundle getServiceTemplate(String providerId, Authentication auth);

    /**
     * Send email notifications to all Providers with outdated EOSC Services
     *
     * @param resourceId Service ID
     * @param auth       Authentication
     */
    void sendEmailNotificationToProviderForOutdatedEOSCService(String resourceId, Authentication auth);
}
