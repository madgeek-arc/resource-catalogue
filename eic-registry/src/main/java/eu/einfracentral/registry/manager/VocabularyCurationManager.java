package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.VocabularyCuration;
import eu.einfracentral.registry.service.VocabularyCurationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

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
        return "vocabulary_entry";
    }


    @Override
    public VocabularyCuration add(VocabularyCuration vocabularyCuration, Authentication auth) {
        if ((vocabularyCuration.getId() == null) || vocabularyCuration.getId().equals("")) {
            vocabularyCuration.setId(UUID.randomUUID().toString());
        }
        super.add(vocabularyCuration, auth);
        logger.info("Adding Vocabulary Curation: {}", vocabularyCuration);
        return vocabularyCuration;
    }

}
