package eu.einfracentral.registry.service;

import eu.einfracentral.domain.EmailMessage;
import eu.einfracentral.domain.ProviderRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

@Deprecated
public interface ProviderRequestService<U extends Authentication> extends ResourceService<ProviderRequest,
        Authentication> {

    /**
     * Returns a list with all the requests made on a specific Provider
     *
     * @param providerId     The ID of the Provider
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
