package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.VocabularyCurationService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.RegistrationMailService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.naming.AuthenticationNotSupportedException;
import java.util.*;

import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;

@Component
public class VocabularyCurationManager extends ResourceManager<VocabularyCuration> implements VocabularyCurationService<VocabularyCuration, Authentication> {

    private static final Logger logger = LogManager.getLogger(VocabularyCurationManager.class);
    private final RegistrationMailService registrationMailService;
    private final ProviderService providerService;
    private final InfraServiceService infraServiceService;

    @Autowired
    private VocabularyService vocabularyService;

    @Autowired
    public VocabularyCurationManager(@Lazy RegistrationMailService registrationMailService, ProviderService providerService,
                                     InfraServiceService infraServiceService) {
        super(VocabularyCuration.class);
        this.registrationMailService = registrationMailService;
        this.providerService = providerService;
        this.infraServiceService = infraServiceService;
    }


    @Override
    public String getResourceType() {
        return "vocabulary_curation";
    }

    @Override
    public VocabularyCuration add(VocabularyCuration vocabularyCuration, Authentication auth) {
        if ((vocabularyCuration.getId() == null) || vocabularyCuration.getId().equals("")) {
            vocabularyCuration.setId(UUID.randomUUID().toString());
        } else {
            throw new ValidationException("You must not provide a VocabularyCuration id");
        }
        // set status, dateOfRequest, userId
        vocabularyCuration.setStatus(VocabularyCuration.Status.PENDING.getKey());
        vocabularyCuration.setRejectionReason(null);
        vocabularyCuration.setResolutionDate(null);
        vocabularyCuration.setResolutionUser(null);
        for (VocabularyEntryRequest vocEntryRequest : vocabularyCuration.getVocabularyEntryRequests()){
            vocEntryRequest.setDateOfRequest(now());
            vocEntryRequest.setUserId(((OIDCAuthenticationToken) auth).getUserInfo().getEmail());
        }
        validate(vocabularyCuration, auth);

        super.add(vocabularyCuration, auth);
        logger.info("Adding Vocabulary Curation: {}", vocabularyCuration);

        registrationMailService.sendVocabularyCurationEmails(vocabularyCuration, ((OIDCAuthenticationToken) auth).getUserInfo().getName());
        return vocabularyCuration;
    }

    @Override
    public VocabularyCuration update(VocabularyCuration vocabularyCuration, Authentication auth) {
        validate(vocabularyCuration);
        super.update(vocabularyCuration, auth);
        logger.debug("Updating Vocabulary Curation {}", vocabularyCuration);
        return vocabularyCuration;
    }

    public VocabularyCuration validate(VocabularyCuration vocabularyCuration, Authentication auth){
        // check if vocabulary already exists
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<Vocabulary> allVocs = vocabularyService.getAll(ff, null).getResults();
        List<String> allVocsIds = new ArrayList<>();
        for (Vocabulary vocabulary : allVocs){
            allVocsIds.add(vocabulary.getName());
        }
        if (allVocsIds.contains(vocabularyCuration.getEntryValueName())){
            throw new ValidationException("Vocabulary with name " + vocabularyCuration.getEntryValueName() + " already exists.");
        }

        // check if vocabularyCuration already exists in "pending"
        List<VocabularyCuration> allVocabularyCurations = getAll(ff, auth).getResults();
        for (VocabularyCuration vocCuration : allVocabularyCurations){
            if (vocCuration.getEntryValueName().equals(vocabularyCuration.getEntryValueName()) &&
                    vocCuration.getStatus().equals(VocabularyCuration.Status.PENDING.getKey())){
                throw new ValidationException("Vocabulary Curation with name " + vocabularyCuration.getEntryValueName() + " already exists");
            }
        }

        // validate vocabulary
        List<String> possibleValues = new ArrayList<>();
        for (Vocabulary.Type vocab : Vocabulary.Type.values()){
            possibleValues.add(vocab.getKey().toLowerCase());
        }
        String voc = vocabularyCuration.getVocabulary();
        if (!possibleValues.contains(voc.toLowerCase())){
            throw new ValidationException("Vocabulary " + voc + "' does not exist.");
        }

        // check if parent exists
        List<Vocabulary> specificVocs;
        List<String> specificVocsIds = new ArrayList<>();
        switch(vocabularyCuration.getVocabulary()){
            case "category":
                if (vocabularyCuration.getParent() == null || vocabularyCuration.getParent().equals("")){
                    throw new ValidationException("Vocabulary " + vocabularyCuration.getVocabulary() + " cannot have an empty parent.");
                } else {
                    specificVocs = vocabularyService.getByType(Vocabulary.Type.SUPERCATEGORY);
                    for (Vocabulary vocabulary : specificVocs){
                        specificVocsIds.add(vocabulary.getId());
                    }
                    if (!specificVocsIds.contains(vocabularyCuration.getParent())){
                        throw new ValidationException("Parent vocabulary " + vocabularyCuration.getParent() + " does not exist.");
                    }
                }
                break;
            case "subcategory":
                if (vocabularyCuration.getParent() == null || vocabularyCuration.getParent().equals("")){
                    throw new ValidationException("Vocabulary " + vocabularyCuration.getVocabulary() + " cannot have an empty parent.");
                } else {
                    specificVocs = vocabularyService.getByType(Vocabulary.Type.CATEGORY);
                    if (!specificVocs.contains(vocabularyCuration.getParent())) {
                        throw new ValidationException("Parent vocabulary " + vocabularyCuration.getParent() + " does not exist.");
                    }
                }
                break;
            case "provider meril scientific subdomain":
                if (vocabularyCuration.getParent() == null || vocabularyCuration.getParent().equals("")){
                    throw new ValidationException("Vocabulary " + vocabularyCuration.getVocabulary() + " cannot have an empty parent.");
                } else {
                    specificVocs = vocabularyService.getByType(Vocabulary.Type.PROVIDER_MERIL_SCIENTIFIC_DOMAIN);
                    if (!specificVocs.contains(vocabularyCuration.getParent())) {
                        throw new ValidationException("Parent vocabulary " + vocabularyCuration.getParent() + " does not exist.");
                    }
                }
                break;
            case "scientific subdomain":
                if (vocabularyCuration.getParent() == null || vocabularyCuration.getParent().equals("")){
                    throw new ValidationException("Vocabulary " + vocabularyCuration.getVocabulary() + " cannot have an empty parent.");
                } else {
                    specificVocs = vocabularyService.getByType(Vocabulary.Type.SCIENTIFIC_DOMAIN);
                    if (!specificVocs.contains(vocabularyCuration.getParent())) {
                        throw new ValidationException("Parent vocabulary " + vocabularyCuration.getParent() + " does not exist.");
                    }
                }
                break;
            default:
                vocabularyCuration.setParent(null);
        }

        // validate resourceType/vocabulary combo
        String resourceType = vocabularyCuration.getVocabularyEntryRequests().get(0).getResourceType();
        if (resourceType.equalsIgnoreCase("provider")){
            if (!vocabularyCuration.getVocabulary().equalsIgnoreCase(Vocabulary.Type.SCIENTIFIC_DOMAIN.getKey()) && !vocabularyCuration.getVocabulary().equalsIgnoreCase(Vocabulary.Type.SCIENTIFIC_SUBDOMAIN.getKey())
                && !vocabularyCuration.getVocabulary().equalsIgnoreCase(Vocabulary.Type.COUNTRY.getKey())){
                if (!StringUtils.containsIgnoreCase(vocabularyCuration.getVocabulary(), "provider")){
                    throw new ValidationException("Resource Type " +resourceType.toLowerCase()+ " can't have as a Vocabulary the value " +vocabularyCuration.getVocabulary());
                }
            }
        } else if (resourceType.equalsIgnoreCase("resource") || resourceType.equalsIgnoreCase("service")){
            if (StringUtils.containsIgnoreCase(vocabularyCuration.getVocabulary(), "provider")){
                throw new ValidationException("Resource Type " +resourceType.toLowerCase()+ " can't have as a Vocabulary the value " +vocabularyCuration.getVocabulary());
            }
        } else {
            throw new ValidationException("The resourceType you submitted is not supported. Possible resourceType values are 'provider', 'resource'");
        }

        // validate if providerId/resourceId exists
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(10000);
        List<ProviderBundle> allProviders = providerService.getAll(facetFilter, auth).getResults();
        List<InfraService> allResources = infraServiceService.getAll(facetFilter, auth).getResults();
        List<String> providerIds = new ArrayList<>();
        List<String> resourceIds = new ArrayList<>();
        for (ProviderBundle provider : allProviders){
            providerIds.add(provider.getId());
        }
        for (InfraService resource : allResources){
            resourceIds.add(resource.getId());
        }
        String providerId = vocabularyCuration.getVocabularyEntryRequests().get(0).getProviderId();
        String resourceId = vocabularyCuration.getVocabularyEntryRequests().get(0).getResourceId();
        if (providerId != null && !providerIds.contains(providerId)){
            throw new ValidationException("Provider with id " +vocabularyCuration.getVocabularyEntryRequests().get(0).getProviderId()+ " does not exist.");
        }
        if (resourceId != null && !resourceIds.contains(resourceId)){
            throw new ValidationException("Resource with id " +vocabularyCuration.getVocabularyEntryRequests().get(0).getProviderId()+ " does not exist.");
        }

        return vocabularyCuration;
    }

    public VocabularyCuration addFront(String resourceId, String providerId, String resourceType,
                                        String entryValueName, String vocabulary, String parent, Authentication auth){
        List<VocabularyEntryRequest> vocabularyEntryRequests = new ArrayList<>();
        VocabularyEntryRequest vocabularyEntryRequest = new VocabularyEntryRequest();
        VocabularyCuration vocabularyCuration = new VocabularyCuration();
        vocabularyEntryRequest.setResourceType(resourceType);
        vocabularyEntryRequest.setResourceId(resourceId);
        vocabularyEntryRequest.setProviderId(providerId);
        vocabularyEntryRequests.add(vocabularyEntryRequest);
        vocabularyCuration.setVocabularyEntryRequests(vocabularyEntryRequests);
        vocabularyCuration.setEntryValueName(entryValueName);
        vocabularyCuration.setVocabulary(vocabulary);
        vocabularyCuration.setParent(parent);
        add(vocabularyCuration, auth);
        return vocabularyCuration;
    }

    public Date now(){
        return DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
    }

    public Browsing<VocabularyCuration> getAllVocabularyCurationRequests(FacetFilter ff, Authentication auth) {
        Browsing<VocabularyCuration> vocabularyCurationBrowsing = super.getAll(ff, auth);
        return vocabularyCurationBrowsing;
    }

    public void approveOrRejectVocabularyCuration(VocabularyCuration vocabularyCuration, boolean approved, String rejectionReason, Authentication authentication){
        vocabularyCuration.setResolutionUser(User.of(authentication).getEmail());
        vocabularyCuration.setResolutionDate(now());
        logger.info("Updating VocabularyRequest " +vocabularyCuration.getEntryValueName());
        if (approved){
            vocabularyCuration.setStatus(VocabularyCuration.Status.APPROVED.getKey());
            createNewVocabulary(vocabularyCuration, authentication);
            registrationMailService.approveOrRejectVocabularyCurationEmails(vocabularyCuration);
        } else{
            vocabularyCuration.setStatus(VocabularyCuration.Status.REJECTED.getKey());
            vocabularyCuration.setRejectionReason(rejectionReason);
            registrationMailService.approveOrRejectVocabularyCurationEmails(vocabularyCuration);
        }
        update(vocabularyCuration, authentication);
    }

    public void createNewVocabulary(VocabularyCuration vocabularyCuration, Authentication authentication){
        Vocabulary vocabulary = new Vocabulary();
        String valueNameFormed = vocabularyCuration.getEntryValueName().replaceAll(" ", "_").toLowerCase();
        String vocabularyFormed = vocabularyCuration.getVocabulary().replaceAll(" ", "_").toLowerCase();
        String parentFormed = vocabularyCuration.getParent().replaceAll(" ", "_").toLowerCase();
        vocabulary.setId(vocabularyFormed+"-"+valueNameFormed);
        vocabulary.setDescription("Vocabulary submitted by " +vocabularyCuration.getVocabularyEntryRequests().get(0).getUserId());
        vocabulary.setName(vocabularyCuration.getEntryValueName());
        vocabulary.setType(Vocabulary.Type.valueOf(vocabularyFormed.toUpperCase()));
        if (vocabularyCuration.getParent() != null && !vocabularyCuration.getParent().equals("")){
            vocabulary.setParentId(parentFormed);
        }
        logger.info("User " +User.of(authentication).getEmail()+ " is adding a new Vocabulary by resolving the vocabulary request " +vocabularyCuration.getId());
        vocabularyService.add(vocabulary, authentication);
    }
}
