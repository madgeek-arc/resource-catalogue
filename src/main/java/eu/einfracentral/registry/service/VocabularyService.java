package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Vocabulary;
import eu.openminted.registry.core.service.ResourceCRUDService;

/**
 * Created by pgl on 24/7/2017.
 */
@org.springframework.stereotype.Service("vocabularyService")

public interface VocabularyService extends ResourceCRUDService<Vocabulary> {

}
