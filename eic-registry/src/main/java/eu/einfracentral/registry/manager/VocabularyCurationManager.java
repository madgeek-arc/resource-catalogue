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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        List<String> possibleValues = new ArrayList<>();
        for (VocabularyCuration.Vocab vocab : VocabularyCuration.Vocab.values()){
            possibleValues.add(vocab.getKey());
        }
        String voc = vocabularyCuration.getVocabulary();
        if (!possibleValues.contains(voc)){
            throw new ValidationException("Vocabulary " + voc + "' does not exist.");
        }
        return vocabularyCuration;
    }

}
