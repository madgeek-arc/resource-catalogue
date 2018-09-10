package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Vocabulary;
import eu.openminted.registry.core.service.SearchService;
import org.springframework.security.core.Authentication;

public interface VocabularyService extends ResourceService<Vocabulary, Authentication> {


    String[] getRegion(String name);

}
