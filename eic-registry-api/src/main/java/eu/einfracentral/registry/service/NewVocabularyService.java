package eu.einfracentral.registry.service;

import eu.einfracentral.domain.NewVocabulary;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

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

    /**
     * Get all vocabularies in a Map.
     *
     * @return
     */
    Map<String, NewVocabulary> getVocabulariesMap();

    /**
     * Get vocabularies in a Map.
     *
     * @param ff
     * @return
     */
    Map<String, NewVocabulary> getVocabulariesMap(FacetFilter ff);

}