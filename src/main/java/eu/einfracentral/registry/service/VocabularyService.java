package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Vocabulary;
import eu.openminted.registry.core.service.SearchService;

public interface VocabularyService extends ResourceService<Vocabulary> {
    String[] getRegion(String name);

    boolean exists(SearchService.KeyValue... ids);
}
