package eu.einfracentral.registry.service;

import eu.einfracentral.domain.NewVocabulary;
import eu.openminted.registry.core.domain.Browsing;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface NewVocabularyService extends ResourceService<NewVocabulary, Authentication> {

    /**
     * @param name
     * @return
     */
    String[] getRegion(String name);

    /**
     * Get all vocabularies of a specific type.
     *
     * @param type
     * @return
     */
    List<NewVocabulary> getByType(NewVocabulary.Type type);

}