package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Vocabulary;
import org.springframework.security.core.Authentication;

public interface VocabularyService extends ResourceService<Vocabulary, Authentication> {

    /**
     *
     * @param name
     * @return
     */
    String[] getRegion(String name);

}
