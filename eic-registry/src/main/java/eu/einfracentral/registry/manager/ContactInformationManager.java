package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.CatalogueService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.ContactInformationService;
import org.springframework.security.core.Authentication;

import java.util.List;

@org.springframework.stereotype.Service("contactInformationManager")
public class ContactInformationManager extends ResourceManager<Bundle>
        implements ContactInformationService<Bundle, Authentication> {

    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;

    public ContactInformationManager(Class<Bundle> typeParameterClass,
                                     ProviderService<ProviderBundle, Authentication> providerService,
                                     CatalogueService<CatalogueBundle, Authentication> catalogueService) {
        super(typeParameterClass);
        this.providerService = providerService;
        this.catalogueService= catalogueService;
    }

    @Override
    public String getResourceType() {
        return null;
    }

    public void updateContactInfoTransfer(boolean acceptedTransfer, Authentication auth) {
        ContactInfoTransfer contactInfoTransfer = createContactInfoTransfer(acceptedTransfer, auth);
        List<CatalogueBundle> catalogueList = catalogueService.getMyCatalogues(auth);
        List<ProviderBundle> providerList = providerService.getMy(null, auth).getResults();
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
                if (!existingTransferList.contains(contactInfoTransfer)) {
                    existingTransferList.add(contactInfoTransfer);
                }
            }
            super.update(catalogueBundle, null);
        }
    }

    private void updateProviderContactInfoTransfer(ContactInfoTransfer contactInfoTransfer,
                                                   List<ProviderBundle> providerList) {
        for (ProviderBundle providerBundle : providerList) {
            List<ContactInfoTransfer> existingTransferList = providerBundle.getTransferContactInformation();
            if (existingTransferList == null || existingTransferList.isEmpty()) {
                providerBundle.setTransferContactInformation(List.of(contactInfoTransfer));
            } else {
                if (!existingTransferList.contains(contactInfoTransfer)) {
                    existingTransferList.add(contactInfoTransfer);
                }
            }
            super.update(providerBundle, null);
        }
    }
}
