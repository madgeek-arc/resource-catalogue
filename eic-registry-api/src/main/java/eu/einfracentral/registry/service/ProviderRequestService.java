package eu.einfracentral.registry.service;

import eu.einfracentral.domain.EmailMessage;
import eu.einfracentral.domain.ProviderRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ProviderRequestService<U extends Authentication> extends ResourceService<ProviderRequest, Authentication> {

    /**
     * Returns a list with all the requests made on a specific Provider
     *
     * @param providerId
     * @param authentication
     * @return
     */
    List<ProviderRequest> getAllProviderRequests(String providerId, U authentication);

    /**
     * Returns a list with all the requests made on a specific Provider
     *
     * @param serviceIds
     * @param message
     * @return
     */
    void sendMailsToProviders(List<String> serviceIds, EmailMessage message, U authentication);

}
