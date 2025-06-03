/*
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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.resourcecatalogue.domain.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;

public class TestUtils {

    public static CatalogueBundle createCatalogueBundle() {
        CatalogueBundle bundle = new CatalogueBundle();
        bundle.setCatalogue(createCatalogue());
        return bundle;
    }

    public static ProviderBundle createProviderBundle() {
        ProviderBundle bundle = new ProviderBundle();
        bundle.setProvider(createProvider());
        return bundle;
    }

    public static ServiceBundle createServiceBundle() {
        ServiceBundle bundle = new ServiceBundle();
        bundle.setService(createService());
        return bundle;
    }

    public static DatasourceBundle createDatasourceBundle() {
        DatasourceBundle bundle = new DatasourceBundle();
        bundle.setDatasource(createDatasource());
        return bundle;
    }

    public static TrainingResourceBundle createTrainingResourceBundle() {
        TrainingResourceBundle bundle = new TrainingResourceBundle();
        bundle.setTrainingResource(createTrainingResource());
        return bundle;
    }

    public static Catalogue createCatalogue() {
        Catalogue catalogue = new Catalogue();
        catalogue.setAbbreviation("EOSC");
        catalogue.setName("EOSC");
        catalogue.setWebsite(createURL());
        catalogue.setLegalEntity(false);
        catalogue.setInclusionCriteria(createURL());
        catalogue.setValidationProcess(createURL());
        catalogue.setEndOfLife("Test End Of Life");
        catalogue.setDescription("Test Description");
        catalogue.setScope("Test Scope");
        catalogue.setLogo(createURL());
        catalogue.setLocation(createProviderLocation());
        catalogue.setMainContact(createProviderMainContact());
        catalogue.setPublicContacts(createProviderPublicContacts());
        catalogue.setUsers(createUsers());
        return catalogue;
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

    public static Datasource createDatasource() {
        Datasource datasource = new Datasource();
        datasource.setServiceId("dat/abc123");
        datasource.setCatalogueId("eosc");
        datasource.setJurisdiction("ds_jurisdiction-global");
        datasource.setDatasourceClassification("ds_classification-repository");
        datasource.setResearchEntityTypes(List.of("ds_research_entity_type-research_data",
                "ds_research_entity_type-research_software"));
        datasource.setThematic(false);
        return datasource;
    }

    public static TrainingResource createTrainingResource() {
        TrainingResource trainingResource = new TrainingResource();
        trainingResource.setTitle("Test Training Resource");
        trainingResource.setResourceOrganisation("11.1111/abc123");
        trainingResource.setAuthors(List.of("Joe Doe", "Foo Bar"));
        trainingResource.setUrl(createURL());
        trainingResource.setLicense("Test License");
        trainingResource.setAccessRights("tr_access_right-open_access");
        trainingResource.setVersionDate(new Date(1674858000000L));
        trainingResource.setTargetGroups(List.of("target_user-businesses", "target_user-funders"));
        trainingResource.setLearningOutcomes(List.of("outcome1", "outcome2"));
        trainingResource.setExpertiseLevel("tr_expertise_level-advanced");
        trainingResource.setLanguages(List.of("en", "es"));
        trainingResource.setGeographicalAvailabilities(List.of("AD", "AE"));
        trainingResource.setScientificDomains(createScientificDomains());
        trainingResource.setContact(createServiceMainContact());
        return trainingResource;
    }

    private static URL createURL() {
        try {
            return URI.create("https://example.org").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static ProviderLocation createProviderLocation() {
        ProviderLocation location = new ProviderLocation();
        location.setStreetNameAndNumber("Test Street Name 1");
        location.setPostalCode("12345");
        location.setCity("Test City");
        location.setCountry("AD");
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
