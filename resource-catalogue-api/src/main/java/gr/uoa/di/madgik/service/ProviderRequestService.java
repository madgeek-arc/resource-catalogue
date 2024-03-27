package gr.uoa.di.madgik.service;

import gr.uoa.di.madgik.domain.EmailMessage;
import gr.uoa.di.madgik.domain.ProviderRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

@Deprecated
public interface ProviderRequestService<U extends Authentication> extends ResourceService<ProviderRequest,
        Authentication> {

    /**
     * Returns a list with all the requests made on a specific Provider
     *
     * @param providerId     Provider ID
     * @param authentication Authentication
     * @return {@link List}&lt;{@link ProviderRequest}&gt;
     */
    List<ProviderRequest> getAllProviderRequests(String providerId, U authentication);

    /**
     * Send mails to all related to the Service List resource Providers
     *
     * @param serviceIds A List of Service IDs
     * @param message    EmailMessage
     */
    void sendMailsToProviders(List<String> serviceIds, EmailMessage message, U authentication);
}
