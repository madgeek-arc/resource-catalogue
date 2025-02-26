/**
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ContactInfoTransfer;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.CatalogueService;
import gr.uoa.di.madgik.resourcecatalogue.service.ContactInformationService;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@org.springframework.stereotype.Service
public class ContactInformationManager implements ContactInformationService {

    private static final Logger logger = LoggerFactory.getLogger(ContactInformationManager.class);

    private final ProviderService providerService;
    private final CatalogueService catalogueService;

    public ContactInformationManager(ProviderService providerService, CatalogueService catalogueService) {
        this.providerService = providerService;
        this.catalogueService = catalogueService;
    }

    public List<String> getMy(Authentication authentication) {
        String email = AuthenticationInfo.getEmail(authentication).toLowerCase();
        List<String> myResources = new ArrayList<>();
        List<String> resourcesUserHasAnsweredFor = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        List<CatalogueBundle> catalogueList = catalogueService.getMy(null, authentication).getResults();
        if (catalogueList != null && !catalogueList.isEmpty()) {
            for (CatalogueBundle catalogueBundle : catalogueList) {
                myResources.add(catalogueBundle.getId());
                List<ContactInfoTransfer> catalogueContactInfoTransferList = catalogueBundle.getTransferContactInformation();
                if (catalogueContactInfoTransferList != null && !catalogueContactInfoTransferList.isEmpty()) {
                    for (ContactInfoTransfer contactInfoTransfer : catalogueContactInfoTransferList) {
                        if (contactInfoTransfer.getEmail().equalsIgnoreCase(email)) {
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
                        if (contactInfoTransfer.getEmail().equalsIgnoreCase(email)) {
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
        String email = AuthenticationInfo.getEmail(auth).toLowerCase();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        ContactInfoTransfer contactInfoTransfer = createContactInfoTransfer(acceptedTransfer, email);
        List<CatalogueBundle> catalogueList = catalogueService.getMy(null, auth).getResults();
        List<ProviderBundle> providerList = providerService.getMy(ff, auth).getResults();
        updateCatalogueContactInfoTransfer(contactInfoTransfer, catalogueList);
        updateProviderContactInfoTransfer(contactInfoTransfer, providerList);
        logger.info("User [{}] set his contact info transfer for all his/her Catalogues/Providers to [{}]",
                email, acceptedTransfer);
    }

    private ContactInfoTransfer createContactInfoTransfer(boolean acceptedTransfer, String email) {
        ContactInfoTransfer contactInfoTransfer = new ContactInfoTransfer();
        contactInfoTransfer.setEmail(email.toLowerCase());
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
            } catch (ResourceNotFoundException ignore) {
            }
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
            } catch (ResourceNotFoundException ignore) {
            }
        }
    }

    private void updateOrAddContactInfoTransfer(ContactInfoTransfer contactInfoTransfer,
                                                List<ContactInfoTransfer> existingTransferList) {
        boolean found = false;
        for (ContactInfoTransfer cit : existingTransferList) {
            if (cit.getEmail().equalsIgnoreCase(contactInfoTransfer.getEmail())) {
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
