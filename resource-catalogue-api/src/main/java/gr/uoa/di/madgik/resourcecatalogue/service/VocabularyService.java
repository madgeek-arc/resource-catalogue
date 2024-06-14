package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.dto.VocabularyTree;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

public interface VocabularyService extends ResourceService<Vocabulary> {

    /**
     * Retrieves Vocabulary based on id or throws exception if not found.
     *
     * @param id Vocabulary ID
     * @return {@link Vocabulary}
     */
    Vocabulary getOrElseThrow(String id);

    /**
     * Get the members of a Region providing its name (WW or EU)
     *
     * @param name Region name
     * @return {@link Array} of {@link String}
     */
    String[] getRegion(String name);

    /**
     * Get parent Vocabulary.
     *
     * @param id Vocabulary ID
     * @return {@link Vocabulary}
     */
    Vocabulary getParent(String id);

    /**
     * Get all Vocabularies by type in a Map.
     *
     * @return {@link Map}&lt;{@link Vocabulary.Type}, {@link List}&lt;{@link Vocabulary}&gt;&gt;
     */
    Map<Vocabulary.Type, List<Vocabulary>> getAllVocabulariesByType();

    /**
     * Get a List of all Vocabularies of a specific Type
     *
     * @param type Vocabulary Type
     * @return {@link List}&lt;{@link Vocabulary}&gt;
     */
    List<Vocabulary> getByType(Vocabulary.Type type);

    /**
     * Get all Vocabularies in a Map.
     *
     * @return {@link Map}&lt;{@link String}, {@link Vocabulary}&gt;
     */
    Map<String, Vocabulary> getVocabulariesMap();

    /**
     * Deletes all Vocabularies.
     */
    void deleteAll(Authentication auth);

    /**
     * Returns a Tree of parents and children for a specific Vocabulary Type
     *
     * @param type Vocabulary Type
     * @return {@link VocabularyTree}
     */
    VocabularyTree getVocabulariesTree(Vocabulary.Type type);
}
