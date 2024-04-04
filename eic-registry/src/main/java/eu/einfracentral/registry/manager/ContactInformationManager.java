package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.CatalogueService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ContactInformationService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@org.springframework.stereotype.Service
public class ContactInformationManager implements ContactInformationService {

    private static final Logger logger = LogManager.getLogger(ContactInformationManager.class);

    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;

    @Autowired
    public ContactInformationManager(ProviderService<ProviderBundle, Authentication> providerService,
                                     CatalogueService<CatalogueBundle, Authentication> catalogueService) {
        this.providerService = providerService;
        this.catalogueService = catalogueService;
    }

    public List<String> getMy(Authentication authentication) {
        String email = User.of(authentication).getEmail();
        List<String> myResources = new ArrayList<>();
        List<String> resourcesUserHasAnsweredFor = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        List<CatalogueBundle> catalogueList = catalogueService.getMyCatalogues(authentication);
        if (catalogueList != null && !catalogueList.isEmpty()) {
            for (CatalogueBundle catalogueBundle : catalogueList) {
                myResources.add(catalogueBundle.getId());
                List<ContactInfoTransfer> catalogueContactInfoTransferList = catalogueBundle.getTransferContactInformation();
                if (catalogueContactInfoTransferList != null && !catalogueContactInfoTransferList.isEmpty()) {
                    for (ContactInfoTransfer contactInfoTransfer : catalogueContactInfoTransferList) {
                        if (contactInfoTransfer.getEmail().equals(email)) {
                            resourcesUserHasAnsweredFor.add(catalogueBundle.getId());
                            break;
                        }
                    }
                }
            }
        }
        List<ProviderBundle> providerList = providerService.getMy(ff, authentication).getResults();
        if (providerList != null && !providerList.isEmpty()) {
            for (ProviderBundle providerBundle : providerList) {
                myResources.add(providerBundle.getId());
                List<ContactInfoTransfer> providerContactInfoTransferList = providerBundle.getTransferContactInformation();
                if (providerContactInfoTransferList != null && !providerContactInfoTransferList.isEmpty()) {
                    for (ContactInfoTransfer contactInfoTransfer : providerContactInfoTransferList) {
                        if (contactInfoTransfer.getEmail().equals(email)) {
                            resourcesUserHasAnsweredFor.add(providerBundle.getId());
                            break;
                        }
                    }
                }
            }
        }
        if (!myResources.isEmpty()) {
            if (new HashSet<>(resourcesUserHasAnsweredFor).containsAll(myResources)) {
                return null;
            } else {
                return myResources;
            }
        } else {
            return null;
        }
    }

    public void updateContactInfoTransfer(boolean acceptedTransfer, Authentication auth) {
        String email = User.of(auth).getEmail();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        ContactInfoTransfer contactInfoTransfer = createContactInfoTransfer(acceptedTransfer, email);
        List<CatalogueBundle> catalogueList = catalogueService.getMyCatalogues(auth);
        List<ProviderBundle> providerList = providerService.getMy(ff, auth).getResults();
        updateCatalogueContactInfoTransfer(contactInfoTransfer, catalogueList);
        updateProviderContactInfoTransfer(contactInfoTransfer, providerList);
        logger.info("User [{}] set his contact info transfer for all his/her Catalogues/Providers to [{}]",
                email, acceptedTransfer);
    }

    private ContactInfoTransfer createContactInfoTransfer(boolean acceptedTransfer, String email) {
        ContactInfoTransfer contactInfoTransfer = new ContactInfoTransfer();
        contactInfoTransfer.setEmail(email);
        contactInfoTransfer.setAcceptedTransfer(acceptedTransfer);
        return contactInfoTransfer;
    }

    private void updateCatalogueContactInfoTransfer(ContactInfoTransfer contactInfoTransfer,
                                                    List<CatalogueBundle> catalogueList) {
        for (CatalogueBundle catalogueBundle : catalogueList) {
            List<ContactInfoTransfer> existingTransferList = catalogueBundle.getTransferContactInformation();
            if (existingTransferList == null || existingTransferList.isEmpty()) {
                catalogueBundle.setTransferContactInformation(List.of(contactInfoTransfer));
            } else {
                updateOrAddContactInfoTransfer(contactInfoTransfer, existingTransferList);
            }
            try {
                catalogueService.update(catalogueBundle, null);
            } catch (ResourceNotFoundException ignore) {}
        }
    }

    private void updateProviderContactInfoTransfer(ContactInfoTransfer contactInfoTransfer,
                                                   List<ProviderBundle> providerList) {
        for (ProviderBundle providerBundle : providerList) {
            List<ContactInfoTransfer> existingTransferList = providerBundle.getTransferContactInformation();
            if (existingTransferList == null || existingTransferList.isEmpty()) {
                providerBundle.setTransferContactInformation(List.of(contactInfoTransfer));
            } else {
                updateOrAddContactInfoTransfer(contactInfoTransfer, existingTransferList);
            }
            try {
                providerService.update(providerBundle, null);
            } catch (ResourceNotFoundException ignore) {}
        }
    }

    private void updateOrAddContactInfoTransfer(ContactInfoTransfer contactInfoTransfer,
                                                List<ContactInfoTransfer> existingTransferList) {
        boolean found = false;
        for (ContactInfoTransfer cit : existingTransferList) {
            if (cit.getEmail().equals(contactInfoTransfer.getEmail())) {
                cit.setAcceptedTransfer(contactInfoTransfer.getAcceptedTransfer());
                found = true;
                break;
            }
        }
        if (!found) {
            existingTransferList.add(contactInfoTransfer);
        }
    }
}
