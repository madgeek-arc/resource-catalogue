package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.resourcecatalogue.domain.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

public class TestUtils {

    public static ProviderBundle createValidProviderBundle() {
        ProviderBundle bundle = new ProviderBundle();
        bundle.setProvider(createValidTestProvider());
        //TODO: populate according to test needs
        return bundle;
    }

    public static Provider createValidTestProvider() {
        Provider provider = new Provider();
        provider.setAbbreviation("Test Abbreviation");
        provider.setName("Test Provider");
        provider.setWebsite(createURL());
        provider.setLegalEntity(false);
        provider.setDescription("Test Description");
        provider.setLogo(createURL());
        provider.setLocation(createProviderLocation());
        provider.setMainContact(createMainContact());
        provider.setPublicContacts(createPublicContact());
        provider.setUsers(createUsers());
        return provider;
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

    private static ProviderMainContact createMainContact() {
        ProviderMainContact contact = new ProviderMainContact();
        contact.setFirstName("MainContact FirstName");
        contact.setEmail("main@email.com");
        return contact;
    }

    private static List<ProviderPublicContact> createPublicContact() {
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
}
