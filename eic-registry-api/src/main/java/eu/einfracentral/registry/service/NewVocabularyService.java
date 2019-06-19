package eu.einfracentral.registry.service;

import eu.einfracentral.domain.NewVocabulary;
import eu.openminted.registry.core.domain.Browsing;
import org.springframework.security.core.Authentication;

public interface NewVocabularyService extends ResourceService<NewVocabulary, Authentication> {

    /**
     * @param name
     * @return
     */
    String[] getRegion(String name);


    Browsing<NewVocabulary> convertVocabularies();

}