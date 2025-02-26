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

import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.config.ServiceConfig;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@ContextConfiguration(classes = {ServiceConfig.class})
@ActiveProfiles("test")
@WebAppConfiguration
public class ServiceProviderRegistrationIT {

    private static final Logger logger = LoggerFactory.getLogger(ServiceProviderRegistrationIT.class);

    @Autowired
    ProviderService providerService;

    @Autowired
    ServiceBundleService<ServiceBundle> serviceBundleService;

    @Autowired
    SecurityService securityService;


    @Test
    public void addInvalidProviderTest() throws MalformedURLException {
        ProviderBundle provider = null;
        try {
            provider = addProvider("*&*");
        } catch (ServiceException e) {
            logger.info(e.getMessage(), e);
        }
        assert provider == null;
    }

    //TODO: Refactor IT with new model

    @Test
    public void addUpdateAndDeleteProvider() throws MalformedURLException {
        String providerId = "wp6";
        ProviderBundle provider;
        ServiceBundle serviceBundle;

        try {

            provider = addProvider(providerId);
            assert provider != null;
            provider = updateProvider(providerId);
            assert provider != null;

            providerService.verify(providerId, "pending template submission", true, securityService.getAdminAccess());

            serviceBundle = new ServiceBundle(createService("WP4_TestService", provider.getProvider()));

            serviceBundle = (ServiceBundle) serviceBundleService.addResource(serviceBundle, securityService.getAdminAccess());

            assert serviceBundle != null;

            providerService.verify(providerId, "rejected template", false, securityService.getAdminAccess());

            serviceBundleService.updateResource(serviceBundle, "woof", securityService.getAdminAccess());

            providerService.verify(providerId, "approved", true, securityService.getAdminAccess());
            providerService.verify(providerId, "approved", false, securityService.getAdminAccess());
            providerService.verify(providerId, "rejected", false, securityService.getAdminAccess());

        } catch (RuntimeException e) {
            logger.error("ERROR", e);
        } finally {
            provider = providerService.get(providerId, securityService.getAdminAccess());
            logger.info("Deleting provider with id: '{}'", provider.getId());
            providerService.delete(provider);
        }

    }

    private ProviderBundle addProvider(String id) throws MalformedURLException {
        List<String> providerTypes = new ArrayList<>();
        providerTypes.add("provider_type-single_sited");
        providerTypes.add("provider_type-distributed");

        List<ServiceProviderDomain> providerScientificSubdomains = new ArrayList<>();
        ServiceProviderDomain serviceProviderDomain = new ServiceProviderDomain();
        serviceProviderDomain.setScientificDomain("provider_scientific_subdomain-agronomy_forestry_plant_breeding_centres");
        serviceProviderDomain.setScientificSubdomain("provider_scientific_subdomain-animal_facilities");
        providerScientificSubdomains.add(serviceProviderDomain);

        ProviderLocation providerLocation = new ProviderLocation();
        providerLocation.setCity("Athens");
        providerLocation.setStreetNameAndNumber("Epidayrou 6");
        providerLocation.setPostalCode("12345");
        providerLocation.setRegion("Attica");

        ProviderMainContact mainContact = new ProviderMainContact();
        mainContact.setPhone("0101010101");
        mainContact.setEmail("maincontact@gmail.com");
        mainContact.setFirstName("MainName");
        mainContact.setLastName("MainSurname");
        mainContact.setPosition("Manager");

        List<ProviderPublicContact> publicContacts = getProviderPublicContacts();

        List<User> users = new ArrayList<>();
        User user = new User();
        user.setEmail("asdf@gmail.com");
        user.setId("");
        user.setName("Kostakis");
        user.setSurname("Spyrakis");
        users.add(user);

        Provider provider = new Provider();
        provider.setId(id);
        provider.setName("WP4_TestProvider");
        provider.setAbbreviation("WP4");
        provider.setWebsite(URI.create("http://wp4.testprovider.com").toURL());
        provider.setDescription("Jtest for PDT WP4 v2.00 01/10/19");
        provider.setLogo(URI.create("https://wp4.testprovider.logo.com").toURL());
        provider.setStructureTypes(providerTypes);
        provider.setScientificDomains(providerScientificSubdomains);
        provider.setLifeCycleStatus("provider_life_cycle_status-under_construction");
        provider.setLocation(providerLocation);
        provider.setMainContact(mainContact);
        provider.setPublicContacts(publicContacts);
        provider.setUsers(users);

        return providerService.add(new ProviderBundle(provider), securityService.getAdminAccess());
    }

    private static @NotNull List<ProviderPublicContact> getProviderPublicContacts() {
        ProviderPublicContact contact1 = new ProviderPublicContact();
        contact1.setEmail("contact1@gmail.com");
        contact1.setFirstName("FirstName1");
        contact1.setLastName("LastName1");
        contact1.setPhone("0123456789");
        ProviderPublicContact contact2 = new ProviderPublicContact();
        contact2.setEmail("contact2@gmail.com");
        contact2.setFirstName("FirstName1");
        contact2.setLastName("LastName1");
        contact2.setPhone("9876543210");
        List<ProviderPublicContact> publicContacts = new ArrayList<>();
        publicContacts.add(contact1);
        publicContacts.add(contact2);
        return publicContacts;
    }

    private ProviderBundle updateProvider(String id) throws MalformedURLException, ResourceNotFoundException {
        // get provider
        ProviderBundle provider = providerService.get(id);

        // update provider
        provider.getProvider().setName("WP4_Test UPDATED");
        provider.getProvider().setAbbreviation("WP4UPDATED");
        provider.getProvider().setWebsite(URI.create("http://wp4.test.updated.com").toURL());
        provider.getProvider().setDescription("Jtest for PDT WP4 v2.00 01/10/19 UPDATED");
        provider.getProvider().setLogo(URI.create("https://wp4.testprovider.logo.updated.com").toURL());
        provider.getProvider().setLifeCycleStatus("provider_life_cycle_status-being_upgraded");
        provider.getProvider().getLocation().setCountry("EU");

        return providerService.update(provider, securityService.getAdminAccess());
    }


    public Service createService(String serviceName, Provider provider) throws MalformedURLException {
        List<ServiceProviderDomain> scientificSubdomains = new ArrayList<>();
        ServiceProviderDomain serviceProviderDomain = new ServiceProviderDomain();
        serviceProviderDomain.setScientificDomain("scientific_subdomain-natural_sciences-mathematics");
        serviceProviderDomain.setScientificSubdomain("scientific_subdomain-natural_sciences-computer_sciences");
        scientificSubdomains.add(serviceProviderDomain);

        List<ServiceCategory> subcategories = new ArrayList<>();
        ServiceCategory serviceCategory = new ServiceCategory();
        serviceCategory.setCategory("category-access_physical_and_eInfrastructures-instrument_and_equipment");
        serviceCategory.setSubcategory("subcategory-access_physical_and_eInfrastructures-instrument_and_equipment-spectrometer");
        subcategories.add(serviceCategory);

        List<String> targetUsers = new ArrayList<>();
        targetUsers.add("target_users-researchers");
        targetUsers.add("target_users-research_groups");

        List<String> languages = new ArrayList<>();
        languages.add("luba-katanga");
        languages.add("english");

        List<String> places = new ArrayList<>();
        places.add("GR");
        places.add("FR");

        List<ServicePublicContact> contacts = getServicePublicContacts();

        Service service = new Service();
        service.setName(serviceName);
        service.setWebpage(URI.create("https:wp4.testservice.com").toURL());
        service.setDescription("Jtest for SDT WP4 v2.00 01/10/19");
        service.setLogo(URI.create("https:wp4.testservice.logo.com").toURL());
        service.setResourceOrganisation(provider.getId());
        service.setScientificDomains(scientificSubdomains);
        service.setCategories(subcategories);
        service.setTargetUsers(targetUsers);
        service.setLanguageAvailabilities(languages);
        service.setGeographicalAvailabilities(places);
        service.setPublicContacts(contacts);

        return service;
    }

    private static @NotNull List<ServicePublicContact> getServicePublicContacts() {
        ServicePublicContact contact1 = new ServicePublicContact();
        contact1.setEmail("contact1@gmail.com");
        contact1.setFirstName("FirstName1");
        contact1.setLastName("LastName1");
        contact1.setPhone("0123456789");
        ServicePublicContact contact2 = new ServicePublicContact();
        contact2.setEmail("contact2@gmail.com");
        contact2.setFirstName("FirstName1");
        contact2.setLastName("LastName1");
        contact2.setPhone("9876543210");
        List<ServicePublicContact> contacts = new ArrayList<>();
        contacts.add(contact1);
        contacts.add(contact2);
        return contacts;
    }
}
