package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Vocabulary;

public interface VocabularyService extends ResourceService<Vocabulary> {
    String[] getRegion(String name);

    boolean exists(String type, String value);
}
