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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceCatalogueGenericService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import java.util.List;

public final class TemplateOnboardingUtils {

    private static final Logger logger = LoggerFactory.getLogger(TemplateOnboardingUtils.class);

    private TemplateOnboardingUtils() {
    }

    public static <T extends Bundle> Bundle getTemplate(String providerId, Authentication auth,
                                                        ResourceCatalogueGenericService<T> service,
                                                        VocabularyService vocabularyService) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_owner", providerId);
        ff.addFilter("published", false);
        List<T> results = service.getAll(ff, auth).getResults();
        if (results == null) {
            return null;
        }
        for (T bundle : results) {
            if (bundle.getStatus() == null || bundle.getStatus().isBlank()) {
                logger.debug("Datasource has null status.");
            } else {
                if (bundle.getStatus().equals(vocabularyService.get("pending").getId())) {
                    return bundle;
                }
            }
        }
        return null;
    }
}
