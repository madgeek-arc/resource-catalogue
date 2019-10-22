package eu.einfracentral.utils;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.openminted.registry.core.service.SearchService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ServiceValidators {

    private static final Logger logger = LogManager.getLogger(ServiceValidators.class);

    private static final int NAME_LENGTH = 80;
    private static final int FIELD_LENGTH = 100;
    private static final int FIELD_LENGTH_SMALL = 20;
    private static final int TEXT_LENGTH = 1000;

    private InfraServiceService<InfraService, InfraService> infraServiceService;
    private ProviderService<Provider, Authentication> providerService;
    private VocabularyService vocabularyService;

    @Autowired
    private ServiceValidators(@Lazy InfraServiceService<InfraService, InfraService> infraServiceService,
                              ProviderService<Provider, Authentication> providerService,
                              VocabularyService vocabularyService) {
        this.infraServiceService = infraServiceService;
        this.providerService = providerService;
        this.vocabularyService = vocabularyService;
    }

    public void validateVocabularies(InfraService service) {
        logger.debug("Validating vocabularies, Service id: {}", service.getId());
        Map<String, Vocabulary> allVocabularies = vocabularyService.getVocabulariesMap();

        // Validate Subcategories
        if (service.getSubcategories() == null || service.getSubcategories().isEmpty())
            throw new ValidationException("Field 'subcategories' is mandatory.");
        for (String subcategory : service.getSubcategories()) {
            if (!allVocabularies.containsKey(subcategory))
                throw new ValidationException(String.format("subcategory '%s' does not exist.", subcategory));
        }

        // Validate Places
        if (service.getPlaces() != null && !service.getPlaces().isEmpty()) {
            List<String> notFoundPlaces = new ArrayList<>();
            List<String> foundPlaces = new ArrayList<>();
            for (String placeId : service.getPlaces()) {
                if (allVocabularies.containsKey(placeId)) {
                    if (!foundPlaces.contains(placeId)) {
                        foundPlaces.add(placeId);
                    }
                } else {
                    notFoundPlaces.add(placeId);
                }
            }
            if (!notFoundPlaces.isEmpty()) {
                throw new ValidationException(String.format("Places not found: %s",
                        String.join(", ", notFoundPlaces)));
            }
            service.setPlaces(foundPlaces);
        } else throw new ValidationException("Field 'places' is mandatory.");

        // Validate Languages
        if (service.getLanguages() != null && !service.getLanguages().isEmpty()) {
            List<String> notFoundLanguages = new ArrayList<>();
            List<String> foundLanguages = new ArrayList<>();
            for (String languageId : service.getLanguages()) {
                if (allVocabularies.containsKey(languageId)) {
                    if (!foundLanguages.contains(languageId)) {
                        foundLanguages.add(languageId);
                    }
                } else {
                    notFoundLanguages.add(languageId);
                }
            }
            if (!notFoundLanguages.isEmpty()) {
                throw new ValidationException(String.format("Languages not found: %s",
                        String.join(", ", notFoundLanguages)));
            }
            service.setLanguages(foundLanguages);
        } else throw new ValidationException("Field 'languages' is mandatory.");

        // Validate Phase
        if (service.getPhase() != null) {
            if (!allVocabularies.containsKey(service.getPhase()))
                throw new ValidationException(String.format("Phase '%s' does not exist.",
                        service.getPhase()));
        }

        // Validate TRL
        if (service.getTrl() != null) {
            if (!allVocabularies.containsKey(service.getTrl()))
                throw new ValidationException(String.format("TRL '%s' does not exist.", service.getTrl()));
        }

        // Validate Scientific Subdomains
        if (service.getScientificSubdomains() == null || service.getScientificSubdomains().isEmpty())
            throw new ValidationException("Field 'scientificSubdomains' is mandatory.");
        for (String scientificSubomain : service.getScientificSubdomains()) {
            if (!allVocabularies.containsKey(scientificSubomain))
                throw new ValidationException(String.format("scientificSubdomain '%s' does not exist.", scientificSubomain));
        }

        // Validate Target Users
        if (service.getTargetUsers() == null || service.getTargetUsers().isEmpty())
            throw new ValidationException("Field 'targetUsers' is mandatory.");
        for (String targetUserNew : service.getTargetUsers()) {
            if (!allVocabularies.containsKey(targetUserNew))
                throw new ValidationException(String.format("targetUser '%s' does not exist.", targetUserNew));
        }

        // Validate Access Types
        if (service.getAccessTypes() != null) {
            if (service.getAccessTypes().size() == 1 && "".equals(service.getAccessTypes().get(0))) {
                service.getAccessTypes().remove(0);
            }
            for (String accessType : service.getAccessTypes()) {
                if (!allVocabularies.containsKey(accessType))
                    throw new ValidationException(String.format("accessType '%s' does not exist.", accessType));
            }
        }

        // Validate Access Modes
        if (service.getAccessModes() != null) {
            if (service.getAccessModes().size() == 1 && "".equals(service.getAccessModes().get(0))) {
                service.getAccessModes().remove(0);
            }
            for (String accessMode : service.getAccessModes()) {
                if (!allVocabularies.containsKey(accessMode))
                    throw new ValidationException(String.format("accessMode '%s' does not exist.", accessMode));
            }
        }

        // Validate Funders
        if (service.getFunders() != null) {
            if (service.getFunders().size() == 1 && "".equals(service.getFunders().get(0))) {
                service.getFunders().remove(0);
            }
            for (String funder : service.getFunders()) {
                if (!allVocabularies.containsKey(funder))
                    throw new ValidationException(String.format("funder '%s' does not exist.", funder));
            }
        }

        // Validate Order Type
        if (service.getOrderType() == null)
            throw new ValidationException("Field 'orderType' is mandatory.");
        if (!allVocabularies.containsKey(service.getOrderType()))
            throw new ValidationException(String.format("orderType '%s' does not exist.", service.getOrderType()));

    }

    // Validate the correctness of Providers.
    public void validateProviders(InfraService service) {
        logger.debug("Validating Providers, Service id: {}", service.getId());
        List<String> providers = service.getProviders();
        List<String> validProviders = new ArrayList<>();
        if ((providers == null) || CollectionUtils.isEmpty(service.getProviders()) ||
                (service.getProviders().stream().filter(Objects::nonNull).mapToInt(p -> 1).sum() == 0)) {
            throw new ValidationException("field 'providers' is obligatory");
        }
        if (service.getProviders().stream().filter(Objects::nonNull)
                .anyMatch(x -> providerService.getResource(x) == null)) {
            throw new ValidationException("Provider does not exist");
        }
        for (String provider : providers) {
            if (!validProviders.contains(provider)) {
                validProviders.add(provider);
            }
        }
        service.setProviders(validProviders);
    }

    // Validate the correctness of Related and Required Services.
    public void validateServices(InfraService service) {
        logger.debug("Validating Required/Related Services, Service id: {}", service.getId());
        List<String> relatedServices = service.getRelatedServices();
        List<String> existingRelatedServices = new ArrayList<>();
        if (relatedServices != null) {
            for (String serviceRel : relatedServices) {
                if (infraServiceService.exists(new SearchService.KeyValue("infra_service_id", serviceRel))
                        && !existingRelatedServices.contains(serviceRel)) {
                    existingRelatedServices.add(serviceRel);
                }
            }
            service.setRelatedServices(existingRelatedServices);
        }

        List<String> requiredServices = service.getRequiredServices();
        List<String> existingRequiredServices = new ArrayList<>();
        if (requiredServices != null) {
            for (String serviceReq : requiredServices) {
                if (infraServiceService.exists(new SearchService.KeyValue("infra_service_id", serviceReq))
                        && !existingRequiredServices.contains(serviceReq)) {
                    existingRequiredServices.add(serviceReq);
                }
            }
            service.setRequiredServices(existingRequiredServices);
        }
    }

    // Validate the correctness of Service Name.
    public void validateName(InfraService service) {
        if (service.getName() == null || service.getName().equals("")) {
            throw new ValidationException("field 'name' is obligatory");
        }
        if (service.getName().length() > NAME_LENGTH) {
            throw new ValidationException("max length for 'name' is " + NAME_LENGTH + " chars");
        }
    }

    // Validate the correctness of Service URL.
    public void validateURL(InfraService service) {
        if (service.getUrl() == null || service.getUrl().toString().equals("")) {
            throw new ValidationException("field 'url' is mandatory");
        }
    }

    // Validate the correctness of Service Description.
    public void validateDescription(InfraService service) {
        if (service.getDescription() == null || service.getDescription().equals("")) {
            throw new ValidationException("field 'description' is mandatory");
        }
        if (service.getDescription().length() > TEXT_LENGTH) {
            throw new ValidationException("max length for 'description' is " + TEXT_LENGTH + " chars");
        }
    }

    // Validate the correctness of Service Logo.
    public void validateLogo(InfraService service) {
        if (service.getLogo() == null || service.getLogo().toString().equals("")) {
            throw new ValidationException("field 'logo' is mandatory");
        }
    }

    // Validate the correctness of Service Options.
    public void validateOptions(InfraService service) {
        if (service.getOptions() != null) {
            for (ServiceOption option : service.getOptions()) {

                // Create Option's id
                option.setId(UUID.randomUUID().toString());

                // Validate the Option's fields requirement
                if (option.getId() == null || option.getId().equals("")) {
                    throw new ValidationException("field 'id' is mandatory");
                }
                if (option.getName() == null || option.getName().equals("")) {
                    throw new ValidationException("field 'name' is mandatory");
                }
                if (option.getDescription() == null || option.getDescription().equals("")) {
                    throw new ValidationException("field 'description' is mandatory");
                }
                if (option.getUrl() == null || option.getUrl().toString().equals("")) {
                    throw new ValidationException("field 'url' is mandatory");
                }

                // Validate max length of Option's fields
                if (option.getName().length() > NAME_LENGTH) {
                    throw new ValidationException("max length for 'name' is " + NAME_LENGTH + " chars");
                }
                if (option.getDescription().length() > TEXT_LENGTH) {
                    throw new ValidationException("max length for 'description' is " + TEXT_LENGTH + " chars");
                }

                // Validate Option's Contacts
                // Validate the Contact's fields requirement
                if (option.getContacts() == null || option.getContacts().isEmpty())
                    throw new ValidationException("Field 'contacts' is mandatory. You need to provide at least 1 contact.");
                for (Contact contact : option.getContacts()){
                    if (contact.getFirstName() == null || contact.getFirstName().equals("")) {
                        throw new ValidationException("field 'firstName' is mandatory");
                    }
                    if (contact.getLastName() == null || contact.getLastName().equals("")) {
                        throw new ValidationException("field 'lastName' is mandatory");
                    }
                    if (contact.getEmail() == null || contact.getEmail().equals("")) {
                        throw new ValidationException("field 'email' is mandatory");
                    }
                    if (contact.getTel() == null || contact.getTel().equals("")) {
                        throw new ValidationException("field 'tel' is mandatory");
                    }

                    // Validate max length of Contact's fields
                    if (contact.getFirstName().length() > FIELD_LENGTH_SMALL) {
                        throw new ValidationException("max length for 'firstName' is " + FIELD_LENGTH_SMALL + " chars");
                    }
                    if (contact.getLastName().length() > FIELD_LENGTH_SMALL) {
                        throw new ValidationException("max length for 'lastName' is " + FIELD_LENGTH_SMALL + " chars");
                    }
                    if (contact.getTel().length() > FIELD_LENGTH_SMALL) {
                        throw new ValidationException("max length for 'tel' is " + FIELD_LENGTH_SMALL + " chars");
                    }
                    if (contact.getPosition().length() > FIELD_LENGTH_SMALL) {
                        throw new ValidationException("max length for 'position' is " + FIELD_LENGTH_SMALL + " chars");
                    }
                }
            }
        }
    }

    // Validate the correctness of Service Version.
    public void validateVersion(InfraService service) {
        if (service.getVersion() != null) {
            if (service.getVersion().length() > FIELD_LENGTH_SMALL) {
                throw new ValidationException("max length for 'version' is " + FIELD_LENGTH_SMALL + " chars");
            }
        }
    }

    // Validate the correctness of Service Contacts.
    public void validateContacts(InfraService service){
        if (service.getContacts() == null || service.getContacts().isEmpty())
            throw new ValidationException("Field 'contacts' is mandatory. You need to provide at least 1 contact.");
        for (Contact contact : service.getContacts()) {

            // Validate the Contact's fields requirement
            if (contact.getFirstName() == null || contact.getFirstName().equals("")) {
                throw new ValidationException("field 'firstName' is mandatory");
            }
            if (contact.getLastName() == null || contact.getLastName().equals("")) {
                throw new ValidationException("field 'lastName' is mandatory");
            }
            if (contact.getEmail() == null || contact.getEmail().equals("")) {
                throw new ValidationException("field 'email' is mandatory");
            }
            if (contact.getTel() == null || contact.getTel().equals("")) {
                throw new ValidationException("field 'tel' is mandatory");
            }

            // Validate max length of Contact's fields
            if (contact.getFirstName().length() > FIELD_LENGTH_SMALL) {
                throw new ValidationException("max length for 'firstName' is " + FIELD_LENGTH_SMALL + " chars");
            }
            if (contact.getLastName().length() > FIELD_LENGTH_SMALL) {
                throw new ValidationException("max length for 'lastName' is " + FIELD_LENGTH_SMALL + " chars");
            }
            if (contact.getTel().length() > FIELD_LENGTH_SMALL) {
                throw new ValidationException("max length for 'tel' is " + FIELD_LENGTH_SMALL + " chars");
            }
            if (contact.getPosition().length() > FIELD_LENGTH_SMALL) {
                throw new ValidationException("max length for 'position' is " + FIELD_LENGTH_SMALL + " chars");
            }
        }
    }

    // Validate the correctness of Service Aggregator Information
    public void validateExtraFields(InfraService service) {
        if (service.getAggregatedServices() == null) {
            service.setAggregatedServices(1);
        } else if (service.getAggregatedServices() < 1) {
            throw new ValidationException("Aggregated services cannot be less than 1");
        }
        if (service.getPublications() == null) {
            service.setPublications(0);
        } else if (service.getPublications() < 0) {
            throw new ValidationException("Publications number cannot be negative");
        }
        if (service.getDatasets() == null) {
            service.setDatasets(0);
        } else if (service.getDatasets() < 0) {
            throw new ValidationException("Data(sets) number cannot be negative");
        }
        if (service.getSoftware() == null) {
            service.setSoftware(0);
        } else if (service.getSoftware() < 0) {
            throw new ValidationException("Software number cannot be negative");
        }
        if (service.getApplications() == null) {
            service.setApplications(0);
        } else if (service.getApplications() < 0) {
            throw new ValidationException("Applications number cannot be negative");
        }
        if (service.getOtherProducts() == null) {
            service.setOtherProducts(0);
        } else if (service.getOtherProducts() < 0) {
            throw new ValidationException("Other products number cannot be negative");
        }
    }

    // Validate the max length of various variables (x10).
    public void validateMaxLength(InfraService service) {
        if (service.getTagline() != null && service.getTagline().length() > FIELD_LENGTH) {
            throw new ValidationException("max length for 'tagline' is " + FIELD_LENGTH + " chars");
        }
        if (service.getUserValue() != null && service.getUserValue().length() > TEXT_LENGTH) {
            throw new ValidationException("max length for 'userValue' is " + TEXT_LENGTH + " chars");
        }
        if (service.getUserBaseList() != null) {
            if (service.getUserBaseList().size() == 1 && "".equals(service.getUserBaseList().get(0))) {
                service.getUserBaseList().remove(0);
            }
            for (String userBase : service.getUserBaseList()) {
                if (userBase != null && userBase.length() > FIELD_LENGTH) {
                    throw new ValidationException("max length for 'userBase' is " + FIELD_LENGTH + " chars");
                }
                if (userBase == null || userBase.equals("")) {
                    throw new ValidationException("One or more items of the userBase list is null or empty");
                }
            }
        }
        if (service.getUseCases() != null) {
            if (service.getUseCases().size() == 1 && "".equals(service.getUseCases().get(0))) {
                service.getUseCases().remove(0);
            }
            for (String userCase : service.getUseCases()) {
                if (userCase != null && userCase.length() > FIELD_LENGTH) {
                    throw new ValidationException("max length for 'useCase' is " + FIELD_LENGTH + " chars");
                }
                if (userCase == null || userCase.equals("")) {
                    throw new ValidationException("One or more items of the useCases list is null or empty");
                }
            }
        }
        if (service.getTags() != null) {
            if (service.getTags().size() == 1 && "".equals(service.getTags().get(0))) {
                service.getTags().remove(0);
            }
            for (String tag : service.getTags()) {
                if (tag != null && tag.length() > FIELD_LENGTH_SMALL) {
                    throw new ValidationException("max length for 'tag' is " + FIELD_LENGTH_SMALL + " chars");
                }
                if (tag == null || tag.equals("")) {
                    throw new ValidationException("One or more items of the tags list is null or empty");
                }
            }
        }
        if (service.getChangeLog() != null && service.getChangeLog().length() > TEXT_LENGTH) {
            throw new ValidationException("max length for 'changeLog' is " + TEXT_LENGTH + "chars");
        }
        if (service.getCertifications() != null) {
            if (service.getCertifications().size() == 1 && "".equals(service.getCertifications().get(0))) {
                service.getCertifications().remove(0);
            }
            for (String certification : service.getCertifications()) {
                if (certification != null && certification.length() > FIELD_LENGTH) {
                    throw new ValidationException("max length for 'certification' is " + FIELD_LENGTH + " chars");
                }
                if (certification == null || certification.equals("")) {
                    throw new ValidationException("One or more items of the certifications list is null or empty");
                }
            }
        }
        if (service.getStandards() != null) {
            if (service.getStandards().size() == 1 && "".equals(service.getStandards().get(0))) {
                service.getStandards().remove(0);
            }
            for (String standard : service.getStandards()) {
                if (standard != null && standard.length() > FIELD_LENGTH) {
                    throw new ValidationException("max length for 'standard' is " + FIELD_LENGTH + " chars");
                }
                if (standard == null || standard.equals("")) {
                    throw new ValidationException("One or more items of the standards list is null or empty");
                }
            }
        }
        if (service.getRelatedPlatforms() != null) {
            if (service.getRelatedPlatforms().size() == 1 && "".equals(service.getRelatedPlatforms().get(0))) {
                service.getRelatedPlatforms().remove(0);
            }
            for (String relatedPlatform : service.getRelatedPlatforms()) {
                if (relatedPlatform != null && relatedPlatform.length() > FIELD_LENGTH_SMALL) {
                    throw new ValidationException("max length for 'relatedPlatform' is " + FIELD_LENGTH_SMALL + " chars");
                }
                if (relatedPlatform == null || relatedPlatform.equals("")) {
                    throw new ValidationException("One or more items of the relatedPlatforms list is null or empty");
                }
            }
        }
    }
}
