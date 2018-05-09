package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Vocabulary;

public interface VocabularyService extends ResourceService<Vocabulary> {
    String[] getEU();
}
