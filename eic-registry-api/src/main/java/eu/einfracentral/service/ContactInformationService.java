package eu.einfracentral.service;

import eu.einfracentral.registry.service.ResourceService;
import org.springframework.security.core.Authentication;

public interface ContactInformationService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    /**
     * Update the Provider's list of ContactTransferInfo
     *
     * @param acceptedTransfer boolean True/False
     * @param authentication Authentication
     */
    void updateContactInfoTransfer(boolean acceptedTransfer, U authentication);
}
