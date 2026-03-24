/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
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
import java.util.LinkedHashMap;
import java.util.List;

@SpringBootTest
@ContextConfiguration(classes = {ServiceConfig.class})
@ActiveProfiles("test")
@WebAppConfiguration
public class ServiceProviderRegistrationIT {

    private static final Logger logger = LoggerFactory.getLogger(ServiceProviderRegistrationIT.class);

    @Autowired
    OrganisationService providerService;

    @Autowired
    ServiceService serviceService;

    @Autowired
    SecurityService securityService;


    @Test
    public void addInvalidProviderTest() throws MalformedURLException {
        OrganisationBundle provider = null;
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
        OrganisationBundle provider;
        ServiceBundle serviceBundle;

        try {

            provider = addProvider(providerId);
            assert provider != null;
            provider = updateProvider(providerId);
            assert provider != null;

            providerService.verify(providerId, "pending template submission", true, securityService.getAdminAccess());

            serviceBundle = new ServiceBundle();
            serviceBundle.setService(createService("WP4_TestService", provider.getId()));

            serviceBundle = serviceService.add(serviceBundle, securityService.getAdminAccess());

            assert serviceBundle != null;

            providerService.verify(providerId, "rejected template", false, securityService.getAdminAccess());

            serviceService.update(serviceBundle, "woof", securityService.getAdminAccess());

            providerService.verify(providerId, "approved", true, securityService.getAdminAccess());
            providerService.verify(providerId, "approved", false, securityService.getAdminAccess());
            providerService.verify(providerId, "rejected", false, securityService.getAdminAccess());

        } catch (RuntimeException e) {
            logger.error("ERROR", e);
        } finally {
            provider = providerService.get(providerId);
            logger.info("Deleting provider with id: '{}'", provider.getId());
            providerService.delete(provider);
        }

    }

    private OrganisationBundle addProvider(String id) throws MalformedURLException {
        List<String> providerTypes = new ArrayList<>();
        providerTypes.add("provider_type-single_sited");
        providerTypes.add("provider_type-distributed");

        List<LinkedHashMap<String, Object>> providerScientificSubdomains = new ArrayList<>();
        LinkedHashMap<String, Object> serviceProviderDomain = new LinkedHashMap<>();
        serviceProviderDomain.put("scientificDomain", "provider_scientific_subdomain-agronomy_forestry_plant_breeding_centres");
        serviceProviderDomain.put("scientificSubdomain", "provider_scientific_subdomain-animal_facilities");
        providerScientificSubdomains.add(serviceProviderDomain);

        LinkedHashMap<String, Object> providerLocation = new LinkedHashMap<>();
        providerLocation.put("city", "Athens");
        providerLocation.put("streetNameAndNumber", "Epidayrou 6");
        providerLocation.put("postalCode", "12345");
        providerLocation.put("region", "Attica");

        LinkedHashMap<String, Object> mainContact = new LinkedHashMap<>();
        mainContact.put("phone", "0101010101");
        mainContact.put("mainEmail", "maincontact@gmail.com");
        mainContact.put("mainFirstName", "MainName");
        mainContact.put("mainLastName", "MainSurname");
        mainContact.put("position", "Manager");

        List<LinkedHashMap<String, Object>> publicContacts = getProviderPublicContacts();

        List<LinkedHashMap<String, Object>> users = new ArrayList<>();
        LinkedHashMap<String, Object> user = new LinkedHashMap<>();
        user.put("email", "asdf@gmail.com");
        user.put("id", "");
        user.put("userName", "Kostakis");
        user.put("userSurname", "Spyrakis");
        users.add(user);

        LinkedHashMap<String, Object> provider = new LinkedHashMap<>();
        provider.put("name", "WP4_TestProvider");
        provider.put("abbreviation", "WP4");
        provider.put("website", URI.create("http://wp4.testprovider.com").toURL());
        provider.put("description", "Jtest for PDT WP4 v2.00 01/10/19");
        provider.put("logo", URI.create("https://wp4.testprovider.logo.com").toURL());
        provider.put("structureTypes", providerTypes);
        provider.put("scientificDomains", providerScientificSubdomains);
        provider.put("lifeCycleStatus", "provider_life_cycle_status-under_construction");
        provider.put("location", providerLocation);
        provider.put("mainContact", mainContact);
        provider.put("publicContacts", publicContacts);
        provider.put("users", users);

        OrganisationBundle bundle = new OrganisationBundle();
        bundle.setId(id);
        bundle.setOrganisation(provider);
        return providerService.add(bundle, securityService.getAdminAccess());
    }

    private static List<LinkedHashMap<String, Object>> getProviderPublicContacts() {
        LinkedHashMap<String, Object> contact1 = new LinkedHashMap<>();
        contact1.put("email", "contact1@gmail.com");
        contact1.put("firstName", "FirstName1");
        contact1.put("lastName", "LastName1");
        contact1.put("phone", "0123456789");
        LinkedHashMap<String, Object> contact2 = new LinkedHashMap<>();
        contact2.put("email", "contact2@gmail.com");
        contact2.put("firstName", "FirstName1");
        contact2.put("lastName", "LastName1");
        contact2.put("phone", "9876543210");
        List<LinkedHashMap<String, Object>> publicContacts = new ArrayList<>();
        publicContacts.add(contact1);
        publicContacts.add(contact2);
        return publicContacts;
    }

    private OrganisationBundle updateProvider(String id) throws MalformedURLException, ResourceNotFoundException {
        // get provider
        OrganisationBundle provider = providerService.get(id);

        // update provider
        provider.getOrganisation().put("name", "WP4_Test UPDATED");
        provider.getOrganisation().put("abbreviation", "WP4UPDATED");
        provider.getOrganisation().put("website", URI.create("http://wp4.test.updated.com").toURL());
        provider.getOrganisation().put("description", "Jtest for PDT WP4 v2.00 01/10/19 UPDATED");
        provider.getOrganisation().put("logo", URI.create("https://wp4.testprovider.logo.updated.com").toURL());
        provider.getOrganisation().put("lifeCycleStatus", "provider_life_cycle_status-being_upgraded");
        ((LinkedHashMap<String, Object>) provider.getOrganisation().get("location")).put("country", "EU");

        return providerService.update(provider, securityService.getAdminAccess());
    }


    public LinkedHashMap<String, Object> createService(String serviceName, String providerId) throws MalformedURLException {
        List<LinkedHashMap<String, Object>> scientificSubdomains = new ArrayList<>();
        LinkedHashMap<String, Object> serviceProviderDomain = new LinkedHashMap<>();
        serviceProviderDomain.put("scientificDomain", "scientific_subdomain-natural_sciences-mathematics");
        serviceProviderDomain.put("scientificSubdomain", "scientific_subdomain-natural_sciences-computer_sciences");
        scientificSubdomains.add(serviceProviderDomain);

        List<LinkedHashMap<String, Object>> subcategories = new ArrayList<>();
        LinkedHashMap<String, Object> serviceCategory = new LinkedHashMap<>();
        serviceCategory.put("category", "category-access_physical_and_eInfrastructures-instrument_and_equipment");
        serviceCategory.put("subcategory", "subcategory-access_physical_and_eInfrastructures-instrument_and_equipment-spectrometer");
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

        List<LinkedHashMap<String, Object>> contacts = getServicePublicContacts();

        LinkedHashMap<String, Object> service = new LinkedHashMap<>();
        service.put("name", serviceName);
        service.put("webpage", URI.create("https:wp4.testservice.com").toURL());
        service.put("description", "Jtest for SDT WP4 v2.00 01/10/19");
        service.put("logo", URI.create("https:wp4.testservice.logo.com").toURL());
        service.put("resourceOrganisation", providerId);
        service.put("scientificDomains", scientificSubdomains);
        service.put("categories", subcategories);
        service.put("targetUsers", targetUsers);
        service.put("languageAvailabilities", languages);
        service.put("geographicalAvailabilities", places);
        service.put("publicContacts", contacts);

        return service;
    }

    private static List<LinkedHashMap<String, Object>> getServicePublicContacts() {
        LinkedHashMap<String, Object> contact1 = new LinkedHashMap<>();
        contact1.put("email", "contact1@gmail.com");
        contact1.put("firstName", "FirstName1");
        contact1.put("lastName", "LastName1");
        contact1.put("phone", "0123456789");
        LinkedHashMap<String, Object> contact2 = new LinkedHashMap<>();
        contact2.put("email", "contact2@gmail.com");
        contact2.put("firstName", "FirstName1");
        contact2.put("lastName", "LastName1");
        contact2.put("phone", "9876543210");
        List<LinkedHashMap<String, Object>> contacts = new ArrayList<>();
        contacts.add(contact1);
        contacts.add(contact2);
        return contacts;
    }
}
