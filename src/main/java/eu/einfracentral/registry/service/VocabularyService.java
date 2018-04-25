package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Vocabulary;
import org.springframework.stereotype.Service;

@Service("vocabularyService")
public interface VocabularyService extends ResourceService<Vocabulary> {
    String[] getEU();
}
