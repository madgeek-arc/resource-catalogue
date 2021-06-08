package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.dto.VocabularyTree;
import eu.openminted.registry.core.domain.FacetFilter;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface VocabularyService extends ResourceService<Vocabulary, Authentication> {

    /**
     * Retrieves Vocabulary based on id or throws exception if not found.
     *
     * @return
     */
    Vocabulary getOrElseThrow(String id);

    /**
     * @param name
     * @return
     */
    String[] getRegion(String name);

    /**
     * Get parent vocabulary.
     *
     * @param id
     * @return
     */
    Vocabulary getParent(String id);

    /**
     * Get all vocabularies by type in a Map.
     *
     * @return
     */
    Map<Vocabulary.Type, List<Vocabulary>> getAllVocabulariesByType();

    /**
     * Get all vocabularies of a specific type.
     *
     * @param type
     * @return
     */
    List<Vocabulary> getByType(Vocabulary.Type type);

    /**
     * Get all vocabularies in a Map.
     *
     * @return
     */
    Map<String, Vocabulary> getVocabulariesMap();

    /**
     * Get vocabularies in a Map.
     *
     * @param ff
     * @return
     */
    Map<String, Vocabulary> getVocabulariesMap(FacetFilter ff);

    /**
     * Adds all new vocs.
     */
    void addAll(List<Vocabulary> newVocabularies, Authentication auth);

    /**
     * Deletes all vocs.
     */
    void deleteAll(Authentication auth);

    VocabularyTree getVocabulariesTree(Vocabulary.Type type);
}
