package eu.einfracentral.registry.service;

import eu.einfracentral.domain.NewVocabulary;
import org.springframework.security.core.Authentication;

public interface NewVocabularyService extends ResourceService<NewVocabulary, Authentication> {

    /**
     * @param name
     * @return
     */
    String[] getRegion(String name);

}