package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Vocabulary;
import org.apache.log4j.Logger;

/**
 * Created by pgl on 24/7/2017.
 */
@org.springframework.stereotype.Service("vocabularyService")
public class VocabularyServiceImpl<T> extends BaseGenericResourceCRUDService<Vocabulary> implements VocabularyService {

    public VocabularyServiceImpl() {
        super(Vocabulary.class);
    }

    @Override
    public String getResourceType() {
        return "vocabulary";
    }

}
