package eu.einfracentral.registry.manager;

import eu.einfracentral.config.ServiceConfig;
import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.configuration.ElasticConfiguration;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ServiceConfig.class})
//@ContextConfiguration(classes = { TestConfig.class })
//@ContextConfiguration(classes = { MockDatabaseConfiguration.class })
@ActiveProfiles("test")
@WebAppConfiguration
public class ServiceProviderRegistrationIT {

    private static final Logger logger = LogManager.getLogger(ServiceProviderRegistrationIT.class);

    @Autowired
    ProviderService<Provider, Authentication> providerService;

    @Autowired
    InfraServiceService<InfraService, InfraService> infraServiceService;

    @Autowired
    VocabularyService vocabularyService;

    @Autowired
    SecurityService securityService;

    @Autowired
    ElasticConfiguration elastic;

    @Test
    public void addInvalidProviderTest() throws MalformedURLException {
        Provider provider = null;
        try {
            provider = addProvider("*&*");
        } catch (ServiceException e) {
            logger.info(e);
        }
        assert provider == null;
    }


    @Test
    public void addUpdateAndDeleteProvider() throws ResourceNotFoundException, MalformedURLException {
        String providerId = "wp6";
        Provider provider;
        InfraService infraService;

        try {

            provider = addProvider(providerId);
            assert provider != null;
            provider = updateProvider(providerId);
            assert provider != null;

            providerService.verifyProvider(providerId, Provider.States.ST_SUBMISSION, true, securityService.getAdminAccess());

            infraService = new InfraService(createService("WP4_TestService", provider));

            infraService = infraServiceService.addService(infraService, securityService.getAdminAccess());

            assert infraService != null;

            providerService.verifyProvider(providerId, Provider.States.REJECTED_ST, false, securityService.getAdminAccess());

            infraServiceService.updateService(infraService, securityService.getAdminAccess());

            providerService.verifyProvider(providerId, Provider.States.APPROVED, true, securityService.getAdminAccess());
            providerService.verifyProvider(providerId, Provider.States.APPROVED, false, securityService.getAdminAccess());
            providerService.verifyProvider(providerId, Provider.States.REJECTED, false, securityService.getAdminAccess());

        } catch (RuntimeException e) {
            logger.error("ERROR", e);
        } finally {
            provider = providerService.get(providerId, securityService.getAdminAccess());
            logger.info("Deleting provider with id: {}", provider.getId());
            providerService.delete(provider);
        }

    }

    private Provider addProvider(String id) throws MalformedURLException {
        List<String> providerTypes = new ArrayList<>();
        providerTypes.add("provider_type-single_sited");
        providerTypes.add("provider_type-distributed");

        List<String> providerCategories = new ArrayList<>();
        providerCategories.add("provider_category-agronomy_forestry_plant_breeding_centres");
        providerCategories.add("provider_category-animal_facilities");

        ProviderLocation providerLocation = new ProviderLocation();
        providerLocation.setCity("Athens");
        providerLocation.setName("ARC");
        providerLocation.setNumber("6");
        providerLocation.setPostalCode("12345");
        providerLocation.setRegion("Attica");
        providerLocation.setStreet("Epidayrou");

        Contact contact1 = new Contact();
        contact1.setEmail("contact1@gmail.com");
        contact1.setFirstName("FirstName1");
        contact1.setLastName("LastName1");
        contact1.setTel("0123456789");
        Contact contact2 = new Contact();
        contact2.setEmail("contact2@gmail.com");
        contact2.setFirstName("FirstName1");
        contact2.setLastName("LastName1");
        contact2.setTel("9876543210");
        List<Contact> contacts = new ArrayList<>();
        contacts.add(contact1);
        contacts.add(contact2);

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
        provider.setAcronym("WP4");
        provider.setWebsite(new URL("http://wp4.testprovider.com"));
        provider.setDescription("Jtest for PDT WP4 v2.00 01/10/19");
        provider.setLogo(new URL("https://wp4.testprovider.logo.com"));
        provider.setTypes(providerTypes);
        provider.setCategories(providerCategories);
        provider.setLifeCycleStatus("provider_life_cycle_status-under_construction");
        provider.setLocation(providerLocation);
        provider.setCoordinatingCountry("GR");
        provider.setContacts(contacts);
        provider.setUsers(users);

        return providerService.add(provider, securityService.getAdminAccess());
    }

    private Provider updateProvider(String id) throws MalformedURLException, ResourceNotFoundException {
        // get provider
        Provider provider = providerService.get(id);

        // update provider
        provider.setName("WP4_Test UPDATED");
        provider.setAcronym("WP4UPDATED");
        provider.setWebsite(new URL("http://wp4.test.updated.com"));
        provider.setDescription("Jtest for PDT WP4 v2.00 01/10/19 UPDATED");
        provider.setLogo(new URL("https://wp4.testprovider.logo.updated.com"));
        provider.setLifeCycleStatus("provider_life_cycle_status-being_upgraded");
        provider.setCoordinatingCountry("EU");

        return providerService.update(provider, securityService.getAdminAccess());
    }


    public Service createService(String serviceName, Provider provider) throws MalformedURLException {
        List<String> scientificSubdomains = new ArrayList<>();
        scientificSubdomains.add("scientific_subdomain-natural_sciences-mathematics");
        scientificSubdomains.add("scientific_subdomain-natural_sciences-computer_sciences");

        List<String> subcategories = new ArrayList<>();
        subcategories.add("subcategory-access_physical_and_eInfrastructures-instrument_and_equipment-spectrometer");
        subcategories.add("subcategory-access_physical_and_eInfrastructures-instrument_and_equipment-radiation");

        List<String> targetUsers = new ArrayList<>();
        targetUsers.add("target_users-researchers");
        targetUsers.add("target_users-research_groups");

        List<String> languages = new ArrayList<>();
        languages.add("luba-katanga");
        languages.add("english");

        List<String> places = new ArrayList<>();
        places.add("GR");
        places.add("FR");

        Contact contact1 = new Contact();
        contact1.setEmail("contact1@gmail.com");
        contact1.setFirstName("FirstName1");
        contact1.setLastName("LastName1");
        contact1.setTel("0123456789");
        Contact contact2 = new Contact();
        contact2.setEmail("contact2@gmail.com");
        contact2.setFirstName("FirstName1");
        contact2.setLastName("LastName1");
        contact2.setTel("9876543210");
        List<Contact> contacts = new ArrayList<>();
        contacts.add(contact1);
        contacts.add(contact2);

        Service service = new Service();
        service.setName(serviceName);
        service.setUrl(new URL("https:wp4.testservice.com"));
        service.setDescription("Jtest for SDT WP4 v2.00 01/10/19");
        service.setLogo(new URL("https:wp4.testservice.logo.com"));
        service.setProviders(Collections.singletonList(provider.getId()));
        service.setScientificSubdomains(scientificSubdomains);
        service.setSubcategories(subcategories);
        service.setTargetUsers(targetUsers);
        service.setLanguages(languages);
        service.setPlaces(places);
        service.setOrderType("order_type-order_required");
        service.setContacts(contacts);

        return service;
    }
}
