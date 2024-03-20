package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.CatalogueService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ContactInformationService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
public class ContactInformationManager implements ContactInformationService {

    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;

    @Autowired
    public ContactInformationManager(ProviderService<ProviderBundle, Authentication> providerService,
                                     CatalogueService<CatalogueBundle, Authentication> catalogueService) {
        this.providerService = providerService;
        this.catalogueService = catalogueService;
    }

    public List<String> getMy(Authentication authentication) {
        List<String> myResources = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        List<CatalogueBundle> catalogueList = catalogueService.getMyCatalogues(authentication);
        List<ProviderBundle> providerList = providerService.getMy(ff, authentication).getResults();
        if (catalogueList != null && !catalogueList.isEmpty()) {
            for (CatalogueBundle catalogueBundle : catalogueList) {
                myResources.add(catalogueBundle.getCatalogue().getName());
            }
        }
        if (providerList != null && !providerList.isEmpty()) {
            for (ProviderBundle providerBundle : providerList) {
                myResources.add(providerBundle.getProvider().getName());
            }
        }
        return myResources;
    }

    public void updateContactInfoTransfer(boolean acceptedTransfer, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        ContactInfoTransfer contactInfoTransfer = createContactInfoTransfer(acceptedTransfer, auth);
        List<CatalogueBundle> catalogueList = catalogueService.getMyCatalogues(auth);
        List<ProviderBundle> providerList = providerService.getMy(ff, auth).getResults();
        updateCatalogueContactInfoTransfer(contactInfoTransfer, catalogueList);
        updateProviderContactInfoTransfer(contactInfoTransfer, providerList);
    }

    private ContactInfoTransfer createContactInfoTransfer(boolean acceptedTransfer, Authentication auth) {
        ContactInfoTransfer contactInfoTransfer = new ContactInfoTransfer();
        contactInfoTransfer.setEmail(User.of(auth).getEmail());
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
                for (ContactInfoTransfer cit : existingTransferList) {
                    if (cit.getEmail().equals(contactInfoTransfer.getEmail())) {
                        cit.setAcceptedTransfer(contactInfoTransfer.getAcceptedTransfer());
                    }
                    break;
                }
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
                for (ContactInfoTransfer cit : existingTransferList) {
                    if (cit.getEmail().equals(contactInfoTransfer.getEmail())) {
                        cit.setAcceptedTransfer(contactInfoTransfer.getAcceptedTransfer());
                    }
                    break;
                }
            }
            try {
                providerService.update(providerBundle, null);
            } catch (ResourceNotFoundException ignore) {}
        }
    }
}