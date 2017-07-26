package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Vocabulary;
import eu.openminted.registry.core.service.ResourceCRUDService;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 24/7/2017.
 */
@Service("vocabularyService")
public interface VocabularyService extends ResourceCRUDService<Vocabulary> {
}
