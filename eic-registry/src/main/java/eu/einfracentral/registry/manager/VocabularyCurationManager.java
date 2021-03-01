package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.VocabularyCuration;
import eu.einfracentral.registry.service.VocabularyCurationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

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
        super.add(vocabularyCuration, auth);
        logger.info("Adding Vocabulary Curation: {}", vocabularyCuration);
        return vocabularyCuration;
    }

}
