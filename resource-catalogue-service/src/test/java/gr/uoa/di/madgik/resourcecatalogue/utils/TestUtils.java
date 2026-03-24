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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

public class TestUtils {

    public static CatalogueBundle createCatalogueBundle() {
        CatalogueBundle bundle = new CatalogueBundle();
        bundle.setCatalogue(createCatalogue());
        return bundle;
    }

    public static OrganisationBundle createOrganisationBundle() {
        OrganisationBundle bundle = new OrganisationBundle();
        bundle.setOrganisation(createOrganisation());
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
        bundle.settTrainingResource(createTrainingResource());
        return bundle;
    }

    public static LinkedHashMap<String, Object> createCatalogue() {
        LinkedHashMap<String, Object> catalogue = new LinkedHashMap<>();
        catalogue.put("id", "eosc");
        catalogue.put("abbreviation", "EOSC");
        catalogue.put("name", "EOSC");
        catalogue.put("website", createURL());
        catalogue.put("legalEntity", false);
        catalogue.put("inclusionCriteria", createURL());
        catalogue.put("validationProcess", createURL());
        catalogue.put("endOfLife", "Test End Of Life");
        catalogue.put("description", "Test Description");
        catalogue.put("scope", "Test Scope");
        catalogue.put("logo", createURL());
        catalogue.put("location", createProviderLocation());
//        catalogue.setMainContact(createProviderMainContact());
        catalogue.put("publicContacts", createProviderPublicContacts());
//        catalogue.setUsers(createUsers());
        return catalogue;
    }

    public static LinkedHashMap<String, Object> createOrganisation() {
        LinkedHashMap<String, Object> provider = new LinkedHashMap<>();
        provider.put("id", "test-provider");
        provider.put("name", "Test Provider");
        provider.put("abbreviation", "Test Abbreviation");
        provider.put("website", createURL());
        provider.put("country", "AD");
        provider.put("legalEntity", "false");
        provider.put("description", "Test Description");
        provider.put("nodePID", "Node");
        provider.put("logo", createURL());
        provider.put("mainContact", createProviderMainContact());
        provider.put("publicContacts", List.of());
//        provider.put("users", createUsers());
        return provider;
    }

    public static LinkedHashMap<String, Object> createService() {
        LinkedHashMap<String, Object> service = new LinkedHashMap<>();
        service.put("id", "test-service");
        service.put("abbreviation", "Test Abbreviation");
        service.put("name", "Test Service");
        service.put("resourceOwner", "11.1111/abc123");
        service.put("webpage", createURL());
        service.put("description", "Test Description");
        service.put("tagline", "Test Tagline");
        service.put("logo", createURL());
        service.put("scientificDomains", createScientificDomains());
        service.put("categories", createCategories());
        service.put("targetUsers", List.of("target_user-businesses", "target_user-funders"));
        service.put("geographicalAvailabilities", List.of("AD", "AE"));
        service.put("languageAvailabilities", List.of("en", "es"));
        service.put("mainContact", createServiceMainContact());
        service.put("publicContacts", createServicePublicContacts());
        service.put("helpdeskEmail", "helpdesk@email.com");
        service.put("securityContactEmail", "security@email.com");
        service.put("trl", "trl-9");
        service.put("termsOfUse", createURL());
        service.put("privacyPolicy", createURL());
        service.put("orderType", "order_type-fully_open_access");
        return service;
    }

    public static LinkedHashMap<String, Object> createDatasource() {
        LinkedHashMap<String, Object> datasource = new LinkedHashMap<>();
        datasource.put("id", "test-datasource");
        datasource.put("serviceId", "dat/abc123");
        datasource.put("catalogueId", "eosc");
        datasource.put("jurisdiction", "ds_jurisdiction-global");
        datasource.put("datasourceClassification", "ds_classification-repository");
        datasource.put("researchEntityTypes", List.of("ds_research_entity_type-research_data",
                "ds_research_entity_type-research_software"));
        datasource.put("thematic", false);
        return datasource;
    }

    public static LinkedHashMap<String, Object> createTrainingResource() {
        LinkedHashMap<String, Object> trainingResource = new LinkedHashMap<>();
        trainingResource.put("id", "test-training-resource");
        trainingResource.put("title", "Test Training Resource");
        trainingResource.put("resourceOwner", "11.1111/abc123");
        trainingResource.put("authors", List.of("Joe Doe", "Foo Bar"));
        trainingResource.put("url", createURL());
        trainingResource.put("license", "Test License");
        trainingResource.put("accessRights", "tr_access_right-open_access");
        trainingResource.put("versionDate", new Date(1674858000000L));
        trainingResource.put("targetGroups", List.of("target_user-businesses", "target_user-funders"));
        trainingResource.put("learningOutcomes", List.of("outcome1", "outcome2"));
        trainingResource.put("expertiseLevel", "tr_expertise_level-advanced");
        trainingResource.put("languages", List.of("en", "es"));
        trainingResource.put("geographicalAvailabilities", List.of("AD", "AE"));
        trainingResource.put("scientificDomains", createScientificDomains());
        trainingResource.put("contact", createServiceMainContact());
        return trainingResource;
    }

    private static URL createURL() {
        try {
            return URI.create("https://example.org").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static LinkedHashMap<String, Object> createProviderLocation() {
        LinkedHashMap<String, Object> location = new LinkedHashMap<>();
        location.put("streetNameAndNumber", "Test Street Name 1");
        location.put("postalCode", "12345");
        location.put("city", "Test City");
        location.put("country", "AD");
        return location;
    }

    private static LinkedHashMap<String, Object> createProviderMainContact() {
        LinkedHashMap<String, Object> contact = new LinkedHashMap<>();
        contact.put("firstName", "FirstName");
        contact.put("lastName", "LastName");
        contact.put("email", "main@email.com");
        contact.put("role", "security contact");
        contact.put("PIDs", null);
        return contact;
    }

    private static List<LinkedHashMap<String, Object>> createProviderPublicContacts() {
        LinkedHashMap<String, Object> contact1 = new LinkedHashMap<>();
        LinkedHashMap<String, Object> contact2 = new LinkedHashMap<>();
        contact1.put("email", "public1@email.com");
        contact2.put("email", "public2@email.com");
        return List.of(contact1, contact2);
    }

    private static List<LinkedHashMap<String, Object>> createUsers() {
        LinkedHashMap<String, Object> user1 = new LinkedHashMap<>();
        LinkedHashMap<String, Object> user2 = new LinkedHashMap<>();
        user1.put("userName", "UserName");
        user1.put("userSurname", "UserSurname");
        user1.put("email", "user1@email.com");
        user2.put("userName", "UserName2");
        user2.put("userSurname", "UserSurname2");
        user2.put("email", "user2@email.com");
        return List.of(user1, user2);
    }

    private static List<LinkedHashMap<String, Object>> createScientificDomains() {
        LinkedHashMap<String, Object> scientificDomain = new LinkedHashMap<>();
        scientificDomain.put("scientificDomain", "scientific_domain-agricultural_sciences");
        scientificDomain.put("scientificSubdomain", "scientific_subdomain-agricultural_sciences-agricultural_biotechnology");
        return List.of(scientificDomain);
    }

    private static List<LinkedHashMap<String, Object>> createCategories() {
        LinkedHashMap<String, Object> serviceCategory = new LinkedHashMap<>();
        serviceCategory.put("category", "category-access_physical_and_eInfrastructures-compute");
        serviceCategory.put("subcategory", "subcategory-access_physical_and_eInfrastructures-compute-container_management");
        return List.of(serviceCategory);
    }

    private static LinkedHashMap<String, Object> createServiceMainContact() {
        LinkedHashMap<String, Object> contact = new LinkedHashMap<>();
        contact.put("firstName", "MainContact FirstName");
        contact.put("email", "main@email.com");
        return contact;
    }

    private static List<LinkedHashMap<String, Object>> createServicePublicContacts() {
        LinkedHashMap<String, Object> contact1 = new LinkedHashMap<>();
        LinkedHashMap<String, Object> contact2 = new LinkedHashMap<>();
        contact1.put("email", "public1@email.com");
        contact2.put("email", "public2@email.com");
        return List.of(contact1, contact2);
    }

    public static LoggingInfo createLoggingInfo(String type, String actionType) {
        LoggingInfo loggingInfo = new LoggingInfo();
        loggingInfo.setType(type);
        loggingInfo.setActionType(actionType);
        return loggingInfo;
    }

    public static Authentication createJwtAuth() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", "test-id")
                .claim("email", "test@example.com")
                .claim("given_name", "Test")
                .claim("family_name", "User")
                .build();

        return new JwtAuthenticationToken(jwt, authorities);
    }
}
