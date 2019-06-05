package eu.einfracentral.utils;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.domain.VocabularyEntry;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.manager.VocabularyManager;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.openminted.registry.core.service.SearchService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ServiceValidators {

    private final Logger logger = LogManager.getLogger(ServiceValidators.class);

    private InfraServiceService<InfraService, InfraService> infraServiceService;
    private ProviderService<Provider, Authentication> providerService;
    private VocabularyManager vocabularyManager;

    @Autowired
    private ServiceValidators(@Lazy InfraServiceService<InfraService, InfraService> infraServiceService,
                              ProviderService<Provider, Authentication> providerService, VocabularyManager vocabularyManager) {
        this.infraServiceService = infraServiceService;
        this.providerService = providerService;
        this.vocabularyManager = vocabularyManager;
    }

    public void validateVocabularies(InfraService service) {
        logger.debug("Validating vocabularies, Service id: " + service.getId());
        //Validate Categories/Subcategories
        if (service.getCategory() != null) {
            Vocabulary categories = vocabularyManager.get("categories");
            VocabularyEntry category = categories.getEntries().get(service.getCategory());
            if (category == null)
                throw new ValidationException(String.format("category '%s' does not exist.", service.getCategory()));
            List<VocabularyEntry> subcategory = category.getChildren();
            if (service.getSubcategory() == null) {
                throw new ValidationException("Field 'subcategory' is mandatory.");
            }
            boolean flag = false;
            for (VocabularyEntry aSubcategory : subcategory) {
                if (aSubcategory.getId().equals(service.getSubcategory())) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                throw new ValidationException(String.format("subcategory '%s' does not exist.", service.getSubcategory()));
            }
        } else throw new ValidationException("Field 'category' is mandatory.");

        //Validate Places
        if (service.getPlaces() != null && !service.getPlaces().isEmpty()) {
            Map<String, VocabularyEntry> places = vocabularyManager.get("places").getEntries();
            List<String> servicePlaces = service.getPlaces();
            List<String> notFoundPlaces = new ArrayList<>();
            List<String> foundPlaces = new ArrayList<>();
            for (String place : servicePlaces) {
                VocabularyEntry placeFound = places.get(place);
                if (placeFound == null) {
                    notFoundPlaces.add(place);
                }
                if (places.containsKey(place) && !foundPlaces.contains(place)) {
                    foundPlaces.add(place);
                }
            }
            if (!notFoundPlaces.isEmpty()) {
                throw new ValidationException(String.format("Places not found: %s", String.join(", ", notFoundPlaces)));
            }
            service.setPlaces(foundPlaces);
        } else throw new ValidationException("Field 'places' is mandatory.");

        //Validate Languages
        if (service.getLanguages() != null && !service.getLanguages().isEmpty()) {
            Map<String, VocabularyEntry> languages = vocabularyManager.get("languages").getEntries();
            List<String> serviceLanguages = service.getLanguages();
            List<String> notFoundLanguages = new ArrayList<>();
            List<String> foundLanguages = new ArrayList<>();
            for (String language : serviceLanguages) {
                VocabularyEntry languageFound = languages.get(language);
                if (languageFound == null) {
                    notFoundLanguages.add(language);
                }
                if (languages.containsKey(language) && !foundLanguages.contains(language)) {
                    foundLanguages.add(language);
                }
            }
            if (!notFoundLanguages.isEmpty()) {
                throw new ValidationException(String.format("Languages not found: %s", String.join(", ", notFoundLanguages)));
            }
            service.setLanguages(foundLanguages);
        } else throw new ValidationException("Field 'languages' is mandatory.");

        //Validate LifeCycleStatus
        if (service.getLifeCycleStatus() != null) {
            Vocabulary lifecyclestatus = vocabularyManager.get("lifecyclestatus");
            VocabularyEntry lfc = lifecyclestatus.getEntries().get(service.getLifeCycleStatus());
            if (lfc == null)
                throw new ValidationException(String.format("LifeCycleStatus '%s' does not exist.", service.getLifeCycleStatus()));
        } else throw new ValidationException("Field 'lifeCycleStatus' is mandatory.");

        //Validate TRL
        if (service.getTrl() != null) {
            Vocabulary trl = vocabularyManager.get("trl");
            VocabularyEntry trlEntry = trl.getEntries().get(service.getTrl());
            if (trlEntry == null)
                throw new ValidationException(String.format("TRL '%s' does not exist.", service.getTrl()));
        } else throw new ValidationException("Field 'trl' is mandatory.");
    }

    //validates the correctness of Providers.
    public void validateProviders(InfraService service) {
        logger.debug("Validating Providers, Service id: " + service.getId());
        List<String> providers = service.getProviders();
        List<String> validProviders = new ArrayList<>();
        if ((providers == null) || CollectionUtils.isEmpty(service.getProviders()) ||
                (service.getProviders().stream().filter(Objects::nonNull).mapToInt(p -> 1).sum() == 0)) {
            throw new ValidationException("field 'providers' is obligatory");
        }
        if (service.getProviders().stream().filter(Objects::nonNull).anyMatch(x -> providerService.getResource(x) == null)) {
            throw new ValidationException("Provider does not exist");
        }
        for (String provider : providers) {
            if (!validProviders.contains(provider)) {
                validProviders.add(provider);
            }
        }
        service.setProviders(validProviders);
    }

    //validates the correctness of Related and Required Services.
    public void validateServices(InfraService service) {
        logger.debug("Validating Required/Related Services, Service id: " + service.getId());
        List<String> relatedServices = service.getRelatedServices();
        List<String> existingRelatedServices = new ArrayList<>();
        if (relatedServices != null) {
            for (String serviceRel : relatedServices) {
                if (infraServiceService.exists(new SearchService.KeyValue("infra_service_id", serviceRel)) && !existingRelatedServices.contains(serviceRel)) {
                    existingRelatedServices.add(serviceRel);
                }
            }
            service.setRelatedServices(existingRelatedServices);
        }

        List<String> requiredServices = service.getRequiredServices();
        List<String> existingRequiredServices = new ArrayList<>();
        if (requiredServices != null) {
            for (String serviceReq : requiredServices) {
                if (infraServiceService.exists(new SearchService.KeyValue("infra_service_id", serviceReq)) && !existingRequiredServices.contains(serviceReq)) {
                    existingRequiredServices.add(serviceReq);
                }
            }
            service.setRequiredServices(existingRequiredServices);
        }
    }

    //validates the correctness of Service Name.
    public void validateName(InfraService service) {
        if (service.getName() == null || service.getName().equals("")) {
            throw new ValidationException("field 'name' is obligatory");
        }
        //TODO: Core should check the max length
        if (service.getName().length() > 160) {
            throw new ValidationException("max length for 'name' is 160 chars");
        }
    }

    //validates the correctness of Service URL.
    public void validateURL(InfraService service) {
        if (service.getUrl() == null || service.getUrl().toString().equals("")) {
            throw new ValidationException("field 'url' is mandatory");
        }
    }

    //validates the correctness of Service Description.
    public void validateDescription(InfraService service) {
        if (service.getDescription() == null || service.getDescription().equals("")) {
            throw new ValidationException("field 'description' is mandatory");
        }
        //TODO: Core should check the max length
        if (service.getDescription().length() > 3000) {
            throw new ValidationException("max length for 'description' is 3000 chars");
        }
    }

    //validates the correctness of Service Symbol.
    public void validateSymbol(InfraService service) {
        if (service.getSymbol() == null || service.getSymbol().toString().equals("")) {
            throw new ValidationException("field 'symbol' is mandatory");
        }
    }

    //validates the correctness of Service Version.
    public void validateVersion(InfraService service) {
        if (service.getVersion() == null || service.getVersion().equals("")) {
            throw new ValidationException("field 'version' is mandatory");
        }
        //TODO: Core should check the max length
        if (service.getVersion().length() > 20) {
            throw new ValidationException("max length for 'version' is 20 chars");
        }
    }

    //validates the correctness of Service Last Update (Revision Date).
    public void validateLastUpdate(InfraService service) {
        if (service.getLastUpdate() == null || service.getLastUpdate().toString().equals("")) {
            throw new ValidationException("field 'Revision Date' (lastUpdate) is mandatory");
        }
    }

    //validates the correctness of URL for requesting the service from the service providers.
    public void validateOrder(InfraService service) {
        if (service.getOrder() == null || service.getOrder().toString().equals("")) {
            throw new ValidationException("field 'order' is mandatory");
        }
    }

    //validates the correctness of Service SLA.
    public void validateSLA(InfraService service) {
        if (service.getServiceLevelAgreement() == null || service.getServiceLevelAgreement().toString().equals("")) {
            throw new ValidationException("field 'serviceLevelAgreement' is mandatory");
        }
    }

    //validates the max length of various variables.
    //FIXME: Core should check the max length
    public void validateMaxLength(InfraService service) {
        if (service.getTagline() != null && service.getTagline().length() > 300) {
            throw new ValidationException("max length for 'tagline' is 300 chars");
        }
        if (service.getOptions() != null && service.getOptions().length() > 3000) {
            throw new ValidationException("max length for 'options' is 3000 chars");
        }
        if (service.getTargetUsers() != null && service.getTargetUsers().length() > 3000) {
            throw new ValidationException("max length for 'targetUsers' is 3000 chars");
        }
        if (service.getUserValue() != null && service.getUserValue().length() > 3000) {
            throw new ValidationException("max length for 'userValue' is 3000 chars");
        }
        if (service.getUserBase() != null && service.getUserBase().length() > 3000) {
            throw new ValidationException("max length for 'userBase' is 3000 chars");
        }
        if (service.getChangeLog() != null && service.getChangeLog().length() > 3000) {
            throw new ValidationException("max length for 'changeLog' is 3000 chars");
        }
        if (service.getFunding() != null && service.getFunding().length() > 1500) {
            throw new ValidationException("max length for 'funding' is 1500 chars");
        }
    }
}
