/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
     *
     * @param parentId Parent Vocabulary ID
     * @return {@link List<Vocabulary>}
     */
    List<Vocabulary> getChildren(String parentId);

    /**
     * Get a List of all Vocabularies of a specific Type
     *
     * @param type Vocabulary Type
     * @return {@link List}&lt;{@link Vocabulary}&gt;
     */
    List<Vocabulary> getByType(Vocabulary.Type type);

    /**
     * Deletes all Vocabularies.
     */
    void deleteAll(Authentication auth);

    /**
     * Deletes all Vocabularies of a specific Vocabulary.Type
     *
     * @param type Vocabulary.Type
     */
    void deleteByType(Vocabulary.Type type);
}
