package gr.uoa.di.madgik.resourcecatalogue.service;

import org.springframework.security.core.Authentication;

import java.util.List;

public interface ContactInformationService {

    /**
     * Get a list of Catalogues and Providers in which the User is Admin
     *
     * @param authentication Authentication
     */
    List<String> getMy(Authentication authentication);

    /**
     * Update the Provider's list of ContactTransferInfo
     *
     * @param acceptedTransfer boolean True/False
     * @param authentication   Authentication
     */
    void updateContactInfoTransfer(boolean acceptedTransfer, Authentication authentication);
}