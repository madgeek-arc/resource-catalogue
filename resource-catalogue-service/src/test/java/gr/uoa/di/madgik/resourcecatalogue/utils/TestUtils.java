package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.resourcecatalogue.domain.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

public class TestUtils {

    public static ProviderBundle createProviderBundle() {
        ProviderBundle bundle = new ProviderBundle();
        bundle.setProvider(createProvider());
        //TODO: populate according to test needs
        return bundle;
    }

    public static ServiceBundle createServiceBundle() {
        ServiceBundle bundle = new ServiceBundle();
        bundle.setService(createService());
        //TODO: populate according to test needs
        return bundle;
    }

    public static Provider createProvider() {
        Provider provider = new Provider();
        provider.setAbbreviation("Test Abbreviation");
        provider.setName("Test Provider");
        provider.setWebsite(createURL());
        provider.setLegalEntity(false);
        provider.setDescription("Test Description");
        provider.setLogo(createURL());
        provider.setLocation(createProviderLocation());
        provider.setMainContact(createProviderMainContact());
        provider.setPublicContacts(createProviderPublicContacts());
        provider.setUsers(createUsers());
        return provider;
    }

    public static Service createService() {
        Service service = new Service();
        service.setAbbreviation("Test Abbreviation");
        service.setName("Test Service");
        service.setResourceOrganisation("11.1111/abc123");
        service.setWebpage(createURL());
        service.setDescription("Test Description");
        service.setTagline("Test Tagline");
        service.setLogo(createURL());
        service.setScientificDomains(createScientificDomains());
        service.setCategories(createCategories());
        service.setTargetUsers(List.of("target_user-businesses", "target_user-funders"));
        service.setGeographicalAvailabilities(List.of("AD", "AE"));
        service.setLanguageAvailabilities(List.of("en", "es"));
        service.setMainContact(createServiceMainContact());
        service.setPublicContacts(createServicePublicContacts());
        service.setHelpdeskEmail("helpdesk@email.com");
        service.setSecurityContactEmail("security@email.com");
        service.setTrl("trl-9");
        service.setTermsOfUse(createURL());
        service.setPrivacyPolicy(createURL());
        service.setOrderType("order_type-fully_open_access");
        return service;
    }

    private static URL createURL() {
        try {
            return URI.create("https://www.example.com").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static ProviderLocation createProviderLocation() {
        ProviderLocation location = new ProviderLocation();
        location.setStreetNameAndNumber("Test Street Name 1");
        location.setPostalCode("12345");
        location.setCity("Test City");
        location.setCountry("EL");
        return location;
    }

    private static ProviderMainContact createProviderMainContact() {
        ProviderMainContact contact = new ProviderMainContact();
        contact.setFirstName("MainContact FirstName");
        contact.setEmail("main@email.com");
        return contact;
    }

    private static List<ProviderPublicContact> createProviderPublicContacts() {
        ProviderPublicContact contact1 = new ProviderPublicContact();
        ProviderPublicContact contact2 = new ProviderPublicContact();
        contact1.setEmail("public1@email.com");
        contact2.setEmail("public2@email.com");
        return List.of(contact1, contact2);
    }

    private static List<User> createUsers() {
        User user1 = new User();
        User user2 = new User();
        user1.setName("User");
        user1.setSurname("One");
        user1.setEmail("user1@email.com");
        user2.setName("User");
        user2.setSurname("Two");
        user2.setEmail("user2@email.com");
        return List.of(user1, user2);
    }

    private static List<ServiceProviderDomain> createScientificDomains() {
        ServiceProviderDomain scientificDomain = new ServiceProviderDomain();
        scientificDomain.setScientificDomain("scientific_domain-agricultural_sciences");
        scientificDomain.setScientificSubdomain("scientific_subdomain-agricultural_sciences-agricultural_biotechnology");
        return List.of(scientificDomain);
    }

    private static List<ServiceCategory> createCategories() {
        ServiceCategory serviceCategory = new ServiceCategory();
        serviceCategory.setCategory("category-access_physical_and_eInfrastructures-compute");
        serviceCategory.setSubcategory("subcategory-access_physical_and_eInfrastructures-compute-container_management");
        return List.of(serviceCategory);
    }

    private static ServiceMainContact createServiceMainContact() {
        ServiceMainContact contact = new ServiceMainContact();
        contact.setFirstName("MainContact FirstName");
        contact.setEmail("main@email.com");
        return contact;
    }

    private static List<ServicePublicContact> createServicePublicContacts() {
        ServicePublicContact contact1 = new ServicePublicContact();
        ServicePublicContact contact2 = new ServicePublicContact();
        contact1.setEmail("public1@email.com");
        contact2.setEmail("public2@email.com");
        return List.of(contact1, contact2);
    }

    public static LoggingInfo createLoggingInfo(String type, String actionType) {
        LoggingInfo loggingInfo = new LoggingInfo();
        loggingInfo.setType(type);
        loggingInfo.setActionType(actionType);
        return loggingInfo;
    }
}
