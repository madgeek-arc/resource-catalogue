package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface EOSCResourceService<T extends Bundle> {

    /**
     *
     * @param providerId  Provider ID
     * @param catalogueId Catalogue ID
     * @param quantity    Quantity to be fetched
     * @param auth        Authentication
     * @return {@link Paging <T>}
     */
    Paging<T> getAllEOSCResourcesOfAProvider(String providerId, String catalogueId, int quantity, Authentication auth);

    /**
     * @param authentication Authentication
     * @param ids            List of Service IDs
     * @return the list of matching resources.
     */
    List<T> getByIds(Authentication authentication, String... ids);

    /**
     * Send email notifications to all Providers with outdated EOSC Services
     *
     * @param resourceId Service ID
     * @param auth       Authentication
     */
    void sendEmailNotificationToProviderForOutdatedEOSCResource(String resourceId, Authentication auth);
}
