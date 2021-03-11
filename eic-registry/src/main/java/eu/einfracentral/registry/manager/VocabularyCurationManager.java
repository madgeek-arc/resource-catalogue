package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.domain.VocabularyCuration;
import eu.einfracentral.domain.VocabularyEntryRequest;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.VocabularyCurationService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.RegistrationMailService;
import eu.openminted.registry.core.domain.FacetFilter;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class VocabularyCurationManager extends ResourceManager<VocabularyCuration> implements VocabularyCurationService<VocabularyCuration, Authentication> {

    private static final Logger logger = LogManager.getLogger(VocabularyCurationManager.class);
    private final RegistrationMailService registrationMailService;

    @Autowired
    private VocabularyService vocabularyService;

    @Autowired
    public VocabularyCurationManager(@Lazy RegistrationMailService registrationMailService) {
        super(VocabularyCuration.class);
        this.registrationMailService = registrationMailService;
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
        vocabularyCuration.setStatus(Provider.States.PENDING_1.getKey());
        for (VocabularyEntryRequest vocEntryRequest : vocabularyCuration.getVocabularyEntryRequests()){
            vocEntryRequest.setDateOfRequest(now());
            vocEntryRequest.setUserId(((OIDCAuthenticationToken) auth).getUserInfo().getEmail());
        }
        validate(vocabularyCuration);

        super.add(vocabularyCuration, auth);
        logger.info("Adding Vocabulary Curation: {}", vocabularyCuration);

        registrationMailService.sendVocabularyCurationEmails(vocabularyCuration, ((OIDCAuthenticationToken) auth).getUserInfo().getName());
        return vocabularyCuration;
    }

    @Override
    public VocabularyCuration validate(VocabularyCuration vocabularyCuration){
        // check if vocabulary already exists
        FacetFilter ff = new FacetFilter();
        List<Vocabulary> allVocs = vocabularyService.getAll(ff, null).getResults();
        if (allVocs.contains(vocabularyCuration.getEntryValueName())){
            throw new ValidationException("Vocabulary with name " + vocabularyCuration.getEntryValueName() + " already exists.");
        }
        // validate vocabulary
        List<String> possibleValues = new ArrayList<>();
        for (VocabularyCuration.Vocab vocab : VocabularyCuration.Vocab.values()){
            possibleValues.add(vocab.getKey().toLowerCase());
        }
        String voc = vocabularyCuration.getVocabulary();
        if (!possibleValues.contains(voc.toLowerCase())){
            throw new ValidationException("Vocabulary " + voc + "' does not exist.");
        }
        // validate provider/resource ids
        for (VocabularyEntryRequest vocEntryRequest : vocabularyCuration.getVocabularyEntryRequests()){
            if (vocEntryRequest.getProviderId() == null && vocEntryRequest.getResourceId() == null){
                throw new ValidationException("You should provide at least one of providerId, resourceId");
            }
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

        return vocabularyCuration;
    }

    public void addFront(String resourceId, String providerId, String resourceType,
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
    }

    public Date now(){
        return DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
    }

}
