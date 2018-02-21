package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.VocabularyService;

/**
 * Created by pgl on 24/7/2017.
 */
@org.springframework.stereotype.Service("vocabularyService")
public class VocabularyServiceImpl extends ResourceServiceImpl<Vocabulary> implements VocabularyService {
    public VocabularyServiceImpl() {
        super(Vocabulary.class);
    }

    @Override
    public String getResourceType() {
        return "vocabulary";
    }
}
