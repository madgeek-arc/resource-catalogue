package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.VocabularyCuration;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.VocabularyCurationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Component
public class VocabularyCurationManager extends ResourceManager<VocabularyCuration> implements VocabularyCurationService<VocabularyCuration, Authentication> {

    private static final Logger logger = LogManager.getLogger(VocabularyCurationManager.class);

    @Autowired
    public VocabularyCurationManager() {
        super(VocabularyCuration.class);
    }

    @Override
    public String getResourceType() {
        return "vocabulary_curation";
    }


    @Override
    public VocabularyCuration add(VocabularyCuration vocabularyCuration, Authentication auth) {
        if ((vocabularyCuration.getId() == null) || vocabularyCuration.getId().equals("")) {
            vocabularyCuration.setId(UUID.randomUUID().toString());
        }
        vocabularyCuration.setStatus(Provider.States.PENDING_1.getKey());
        validate(vocabularyCuration);
        // check if parent exists

        super.add(vocabularyCuration, auth);
        logger.info("Adding Vocabulary Curation: {}", vocabularyCuration);
        return vocabularyCuration;
    }

    @Override
    public VocabularyCuration validate(VocabularyCuration vocabularyCuration){
        Object[] possibleValues = Arrays.stream(VocabularyCuration.Vocab.class.getDeclaringClass().getEnumConstants()).toArray();
        String voc = vocabularyCuration.getVocabulary();
        if (!Arrays.asList(possibleValues).contains(voc)){
            throw new ValidationException("Vocabulary " + voc + "' does not exist.");
        }
        return vocabularyCuration;
    }

}
