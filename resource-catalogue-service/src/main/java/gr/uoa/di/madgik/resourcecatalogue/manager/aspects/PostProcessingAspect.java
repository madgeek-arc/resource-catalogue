/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.resourcecatalogue.manager.aspects;

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.NewProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Profile("beyond")
@Aspect
@Component
public class PostProcessingAspect {

    private static final Logger logger = LoggerFactory.getLogger(PostProcessingAspect.class);

    private final VocabularyService vocabularyService;
    private final Map<String, Consumer<Object>> aspectRegistry = new HashMap<>();


    public PostProcessingAspect(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
        aspectRegistry.put("HostingLegalEntityVocabularyUpdate", obj -> {
            if (!(obj instanceof NewProviderBundle bundle)) {
                logger.debug("Skipping HostingLegalEntityVocabularyUpdate – object is {}", obj.getClass());
                return;
            }
            checkAndAddProviderToHLEVocabulary(bundle);
        });
    }

    @AfterReturning(
            pointcut = "@annotation(triggersAspects)",
            returning = "result"
    )
    public void afterReturningAdvice(JoinPoint joinPoint, TriggersAspects triggersAspects, Object result) {
        Arrays.stream(triggersAspects.value())
                .forEach(aspectName -> {
                    Consumer<Object> logic = aspectRegistry.get(aspectName);
                    if (logic != null) {
                        logic.accept(result);
                    }
                });
    }

    private void checkAndAddProviderToHLEVocabulary(NewProviderBundle bundle) {
        // TODO: field type (in model) should be 'boolean', not radio with "true"/"false" values.
        boolean legalEntity = switch (bundle.getProvider().get("legalEntity")) {
            case Boolean value -> value;
            case String str -> Boolean.parseBoolean(str);
            default -> throw new ValidationException("Error in field 'legalEntity', should be boolean.");
        };
        if (bundle.getStatus().equalsIgnoreCase("approved") && legalEntity) {
            addOrUpdateProviderHLEVocabulary(bundle.getId(), (String) bundle.getProvider().get("name"), bundle.getCatalogueId());
        }
    }

    private void addOrUpdateProviderHLEVocabulary(String id, String name, String catalogueId) {
        String hleId = createProviderHLEId(id);
        Vocabulary hle;
        try {
            hle = vocabularyService.get(hleId);
            if (!hle.getName().equals(name)) {
                hle.setName(name);
                logger.info("Updating Hosting Legal Entity Vocabulary [{}] with new name: [{}]",
                        hle.getId(), hle.getName());
                vocabularyService.update(hle, null);
            }
        } catch (ResourceException e) {
            hle = new Vocabulary();
            hle.setId(hleId);
            hle.setName(name);
            hle.setType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY.getKey());
            hle.setExtras(new HashMap<>() {{
                put("catalogueId", catalogueId);
            }});
            logger.info("Creating a new Hosting Legal Entity Vocabulary with id: [{}] and name: [{}]",
                    hle.getId(), hle.getName());
            vocabularyService.add(hle, null);
        }
    }

    private String createProviderHLEId(String id) {
        return "%s-%s".formatted(
                Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY.getKey().toLowerCase().replace(" ", "_"), id);
    }
}
