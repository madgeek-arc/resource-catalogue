package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.EmailMessage;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

@Deprecated
public interface ProviderRequestService extends ResourceService<ProviderRequest> {

    /**
     * Returns a list with all the requests made on a specific Provider
     *
     * @param providerId     Provider ID
     * @param authentication Authentication
     * @return {@link List}&lt;{@link ProviderRequest}&gt;
     */
    List<ProviderRequest> getAllProviderRequests(String providerId, Authentication authentication);

    /**
     * Send mails to all related to the Service List resource Providers
     *
     * @param serviceIds A List of Service IDs
     * @param message    EmailMessage
     */
    void sendMailsToProviders(List<String> serviceIds, EmailMessage message, Authentication authentication);
}
