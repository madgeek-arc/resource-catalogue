package eu.einfracentral.utils;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.VocabularyService;
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
        logger.debug("Validating vocabularies, Service id: " + service.getId());
        Map<String, Vocabulary> allVocabularies = vocabularyService.getVocabulariesMap();

        // Validate Subcategories
        if (service.getSubcategories() == null || service.getSubcategories().isEmpty())
            throw new ValidationException("Field 'subcategories' is mandatory.");
        for (String subcategory : service.getSubcategories()){
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
        } else throw new ValidationException("Field 'phase' is mandatory.");

        // Validate TRL
        if (service.getTrl() != null) {
            if (!allVocabularies.containsKey(service.getTrl()))
                throw new ValidationException(String.format("TRL '%s' does not exist.", service.getTrl()));
        } else throw new ValidationException("Field 'trl' is mandatory.");

        // Validate Scientific Domains/Subdomains
        if (service.getScientificDomains() == null || service.getScientificDomains().isEmpty())
            throw new ValidationException("Field 'scientificDomains' is mandatory.");
        for (String scientificDomain : service.getScientificDomains()){
            if (!allVocabularies.containsKey(scientificDomain))
                throw new ValidationException(String.format("scientificDomain '%s' does not exist.", scientificDomain));
        }
        if (service.getScientificSubdomains() == null || service.getScientificSubdomains().isEmpty())
            throw new ValidationException("Field 'scientificSubdomains' is mandatory.");
        for (String scientificSubomain : service.getScientificSubdomains()){
            if (!allVocabularies.containsKey(scientificSubomain))
                throw new ValidationException(String.format("scientificSubdomain '%s' does not exist.", scientificSubomain));
        }

        // Validate Target Users
        if (service.getTargetUsers() == null || service.getTargetUsers().isEmpty())
            throw new ValidationException("Field 'targetUsers' is mandatory.");
        for (String targetUserNew : service.getTargetUsers()){
            if (!allVocabularies.containsKey(targetUserNew))
                throw new ValidationException(String.format("targetUser '%s' does not exist.", targetUserNew));
        }

        // Validate Access Types
        if (service.getAccessTypes() != null) {
            for (String accessType : service.getAccessTypes()){
                if (!allVocabularies.containsKey(accessType))
                    throw new ValidationException(String.format("accessType '%s' does not exist.", accessType));
            }
        }

        // Validate Access Modes
        if (service.getAccessModes() != null) {
            for (String accessMode : service.getAccessModes()){
                if (!allVocabularies.containsKey(accessMode))
                    throw new ValidationException(String.format("accessMode '%s' does not exist.", accessMode));
            }
        }

        // Validate Funders
        if (service.getFunders() != null) {
            for (String funder : service.getFunders()){
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
        logger.debug("Validating Providers, Service id: " + service.getId());
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
        logger.debug("Validating Required/Related Services, Service id: " + service.getId());
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
        if (service.getName().length() > 80) {
            throw new ValidationException("max length for 'name' is 240 chars");
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
        if (service.getDescription().length() > 1000) {
            throw new ValidationException("max length for 'description' is 1000 chars");
        }
    }

    // Validate the correctness of Service Logo.
    public void validateLogo(InfraService service) {
        if (service.getLogo() == null || service.getLogo().toString().equals("")) {
            throw new ValidationException("field 'logo' is mandatory");
        }
    }

    // Validate the max length of various variables (x10).
    public void validateMaxLength(InfraService service) {
        if (service.getTagline() != null && service.getTagline().length() > 1000) {
            throw new ValidationException("max length for 'tagline' is 100 chars");
        }
        if (service.getUserValue() != null && service.getUserValue().length() > 10000) {
            throw new ValidationException("max length for 'userValue' is 1000 chars");
        }
        for (String userBase : service.getUserBaseList()){ // TODO: check if it works as intended
            if (userBase != null && userBase.length() > 100) {
                throw new ValidationException("max length for 'userBase' is 100 chars");
            }
        }
        for (String userCase : service.getUseCases()){
            if (userCase != null && userCase.length() > 1000) {
                throw new ValidationException("max length for 'userCase' is 100 chars");
            }
        }
        for (String tag : service.getTags()){
            if (tag != null && tag.length() > 2000) {
                throw new ValidationException("max length for 'tag' is 20 chars");
            }
        }
        if (service.getVersion().length() > 100) {
            throw new ValidationException("max length for 'version' is 10 chars");
        }
        if (service.getChangeLog().length() > 10000) {
            throw new ValidationException("max length for 'changeLog' is 1000 chars");
        }
        for (String certification : service.getCertifications()){
            if (certification != null && certification.length() > 1000) {
                throw new ValidationException("max length for 'certification' is 100 chars");
            }
        }
        for (String standard : service.getStandards()){
            if (standard != null && standard.length() > 1000) {
                throw new ValidationException("max length for 'standard' is 100 chars");
            }
        }
        if (service.getOwnerName() != null && service.getOwnerName().length() > 200) {
            throw new ValidationException("max length for 'ownerName' is 20 chars");
        }
        if (service.getSupportName() != null && service.getSupportName().length() > 200) {
            throw new ValidationException("max length for 'supportName' is 20 chars");
        }
        if (service.getSecurityName() != null && service.getSecurityName().length() > 200) {
            throw new ValidationException("max length for 'securityName' is 20 chars");
        }
    }
}
