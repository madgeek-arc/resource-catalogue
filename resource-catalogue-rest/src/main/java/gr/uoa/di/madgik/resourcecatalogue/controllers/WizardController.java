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

package gr.uoa.di.madgik.resourcecatalogue.controllers;

import gr.uoa.di.madgik.catalogue.domain.ModelConfiguration;
import gr.uoa.di.madgik.catalogue.domain.Section;
import gr.uoa.di.madgik.catalogue.domain.Series;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.config.NodeProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Profile("beyond")
@Controller
@RequestMapping("/wizard")
@Tag(name = "wizard")
@Hidden
public class WizardController {

    private static final Logger logger = LoggerFactory.getLogger(WizardController.class);

    private final NodeProperties nodeProperties;
    private final VocabularyService vocabularyService;
    private final ModelService modelService;
    private final GenericResourceService genericService;
    private final ObjectMapper objectMapper;
    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(nodeProperties.getRegistry().getUrl())
                .build();
    }

    public WizardController(VocabularyService vocabularyService,
                            ModelService modelService,
                            GenericResourceService genericService,
                            ObjectMapper objectMapper,
                            NodeProperties nodeProperties) {
        this.vocabularyService = vocabularyService;
        this.modelService = modelService;
        this.genericService = genericService;
        this.objectMapper = objectMapper;
        this.nodeProperties = nodeProperties;
    }

    public enum LoadStatus { NEW, CHANGED, UP_TO_DATE }

    private record VocabularyContent(String name, String description, String parentId,
                                      SortedMap<String, String> extras) {
        static VocabularyContent of(Vocabulary v) {
            return new VocabularyContent(v.getName(), v.getDescription(), v.getParentId(),
                    v.getExtras() != null ? new TreeMap<>(v.getExtras()) : new TreeMap<>());
        }
    }

    private record ModelContent(String name, String description, String notice, String type,
                                 String subType, Series series, String resourceType,
                                 Instant submissionStartAt, Instant submissionCloseAt,
                                 List<Section> sections, ModelConfiguration configuration) {
        static ModelContent of(gr.uoa.di.madgik.catalogue.domain.Model m) {
            return new ModelContent(m.getName(), m.getDescription(), m.getNotice(), m.getType(),
                    m.getSubType(), m.getSeries(), m.getResourceType(), m.getSubmissionStartAt(),
                    m.getSubmissionCloseAt(), m.getSections(), m.getConfiguration());
        }
    }

    private String contentHash(Vocabulary v) {
        return sha256Hex(objectMapper.writeValueAsString(VocabularyContent.of(v)));
    }

    private String contentHash(gr.uoa.di.madgik.catalogue.domain.Model m) {
        return sha256Hex(objectMapper.writeValueAsString(ModelContent.of(m)));
    }

    private static String sha256Hex(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private Vocabulary tryGetVocabulary(String id) {
        try {
            return vocabularyService.get(id);
        } catch (ResourceNotFoundException | NoSuchElementException e) {
            return null;
        }
    }

    private gr.uoa.di.madgik.catalogue.domain.Model tryGetModel(String id) {
        try {
            return modelService.get(id);
        } catch (ResourceNotFoundException | NoSuchElementException e) {
            return null;
        }
    }

    @Operation(summary = "Check Vocabularies Existence")
    @GetMapping("/step1")
    public String checkVocabulariesExistence(Model model) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] vocabFiles = resolver.getResources("classpath:vocabularies/*.json");

        Map<String, LoadStatus> vocabStatus = new TreeMap<>();
        boolean allLoaded = true;

        for (Resource resource : vocabFiles) {
            List<Vocabulary> vocabularies = objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {
            });

            if (!vocabularies.isEmpty()) {
                String type = vocabularies.getFirst().getType();
                List<Vocabulary> inDb = vocabularyService.getByType(Vocabulary.Type.fromString(type));

                Map<String, Vocabulary> inDbById = new HashMap<>();
                for (Vocabulary v : inDb) {
                    inDbById.put(v.getId(), v);
                }

                LoadStatus status;
                if (inDb.isEmpty()) {
                    status = LoadStatus.NEW;
                } else {
                    status = LoadStatus.UP_TO_DATE;
                    for (Vocabulary v : vocabularies) {
                        Vocabulary existing = inDbById.get(v.getId());
                        if (existing == null || !contentHash(existing).equals(contentHash(v))) {
                            status = LoadStatus.CHANGED;
                            break;
                        }
                    }
                }
                vocabStatus.put(type, status);

                if (status != LoadStatus.UP_TO_DATE) {
                    allLoaded = false;
                }
            }
        }

        model.addAttribute("vocabStatus", vocabStatus);
        model.addAttribute("allVocabLoaded", allLoaded);
        return "wizard-step1";
    }

    @Operation(summary = "Load Vocabularies")
    @PostMapping("/step1/loadVocabularies")
    public String loadVocabularies(Authentication authentication) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] vocabularyFiles = resolver.getResources("classpath:vocabularies/*.json");

        for (Resource resource : vocabularyFiles) {
            List<Vocabulary> vocabularies = objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {
            });

            for (Vocabulary v : vocabularies) {
                Vocabulary existing = tryGetVocabulary(v.getId());
                if (existing == null) {
                    vocabularyService.add(v, authentication);
                    logger.info("Vocabulary [{}] added", v.getId());
                } else if (!contentHash(existing).equals(contentHash(v))) {
                    vocabularyService.update(v, authentication);
                    logger.info("Vocabulary [{}] updated", v.getId());
                }
            }
        }
        return "redirect:/wizard/step1";
    }

    @Operation(summary = "Check Models Existence")
    @GetMapping("/step2")
    public String checkModelsExistence(Model model) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] modelFiles = resolver.getResources("classpath:models/*.json");

        Map<String, LoadStatus> modelStatus = new TreeMap<>();

        for (Resource resource : modelFiles) {
            try {
                gr.uoa.di.madgik.catalogue.domain.Model m = objectMapper.readValue(resource.getInputStream(), gr.uoa.di.madgik.catalogue.domain.Model.class);

                gr.uoa.di.madgik.catalogue.domain.Model existing = tryGetModel(m.getId());
                LoadStatus status;
                if (existing == null) {
                    status = LoadStatus.NEW;
                } else if (!contentHash(existing).equals(contentHash(m))) {
                    status = LoadStatus.CHANGED;
                } else {
                    status = LoadStatus.UP_TO_DATE;
                }
                modelStatus.put(m.getName() != null ? m.getName() : m.getId(), status);
            } catch (Exception e) {
                logger.warn("Skipping model file [{}]: {}", resource.getFilename(), e.getMessage());
            }
        }

        boolean allLoaded = modelStatus.values().stream().allMatch(status -> status == LoadStatus.UP_TO_DATE);

        model.addAttribute("modelStatus", modelStatus);
        model.addAttribute("allModelsLoaded", allLoaded);

        return "wizard-step2";
    }

    @Operation(summary = "Load Models")
    @PostMapping("/step2/loadModels")
    public String loadModels(Authentication authentication) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] modelFiles = resolver.getResources("classpath:models/*.json");

        for (Resource resource : modelFiles) {
            try {
                gr.uoa.di.madgik.catalogue.domain.Model m = objectMapper.readValue(resource.getInputStream(), gr.uoa.di.madgik.catalogue.domain.Model.class);
                gr.uoa.di.madgik.catalogue.domain.Model existing = tryGetModel(m.getId());

                if (existing == null) {
                    modelService.add(m);
                    logger.info("Model [{}] added", m.getId());
                } else if (!contentHash(existing).equals(contentHash(m))) {
                    modelService.update(m.getId(), m);
                    logger.info("Model [{}] updated", m.getId());
                } else {
                    logger.debug("Model [{}] up to date, skipping.", m.getId());
                }

            } catch (Exception e) {
                logger.error("Failed to process model file [{}]: {}", resource.getFilename(), e.getMessage());
            }
        }

        return "redirect:/wizard/step2";
    }

    @Operation(summary = "Node Registry Information")
    @GetMapping("/step3")
    public String nodeRegistryInfo(Model model) {
        boolean isRegistered = false;
        try {
            List<Map<String, Object>> nodes = webClient.get()
                    .header("x-api-key", nodeProperties.getRegistry().getKey())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    })
                    .block();
            if (nodes != null) {
                isRegistered = nodes.stream()
                        .anyMatch(node -> nodeProperties.getPid().getValue().equals(node.get("pid")));
            }
        } catch (Exception e) {
            logger.warn("Could not reach node registry to check registration status: {}", e.getMessage());
        }
        model.addAttribute("isRegistered", isRegistered);
        return "wizard-step3";
    }

    @GetMapping("/success")
    public String wizardSuccess() {
        return "wizard-success";
    }

    @GetMapping("/403")
    public String wizardAccessDenied() {
        return "wizard-403";
    }

}
