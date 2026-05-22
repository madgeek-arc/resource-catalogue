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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
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

    @Operation(summary = "Check Vocabularies Existence")
    @GetMapping("/step1")
    public String checkVocabulariesExistence(Model model) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] vocabFiles = resolver.getResources("classpath:vocabularies/*.json");

        Map<String, Boolean> vocabStatus = new TreeMap<>();
        boolean allLoaded = true;

        for (Resource resource : vocabFiles) {
            List<Vocabulary> vocabularies = objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {
            });

            if (!vocabularies.isEmpty()) {
                String type = vocabularies.getFirst().getType();
                int countInJson = vocabularies.size();
                int countInDb = vocabularyService.getByType(Vocabulary.Type.fromString(type)).size();
                boolean fullyPosted = countInDb >= countInJson;
                vocabStatus.put(type, fullyPosted);

                if (!fullyPosted) {
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
    public String loadVocabularies() throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] vocabularyFiles = resolver.getResources("classpath:vocabularies/*.json");

        if (vocabularyFiles.length > 0) {
            for (Resource resource : vocabularyFiles) {
                List<Vocabulary> vocabularies = objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {
                });

                if (!vocabularies.isEmpty()) {
                    String type = vocabularies.getFirst().getType();
                    int countInDb = vocabularyService.getByType(Vocabulary.Type.fromString(type)).size();

                    if (countInDb != vocabularies.size() && countInDb < vocabularies.size()) {
                        vocabularyService.deleteByType(Vocabulary.Type.fromString(type));
                    }

                    if (countInDb < vocabularies.size()) {
                        logger.info("Loading vocabularies for type [{}]", type);
                        vocabularyService.addBulk(vocabularies, null);
                    }
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

        if (modelFiles.length == 0) {
            model.addAttribute("modelStatus", Collections.emptyMap());
            model.addAttribute("allModelsLoading", false);
            model.addAttribute("allModelsLoaded", true);
            return "wizard-step2";
        }

        Map<String, Boolean> modelStatus = new TreeMap<>();
        boolean nonePosted = true;

        for (Resource resource : modelFiles) {
            try {
                gr.uoa.di.madgik.catalogue.domain.Model m = objectMapper.readValue(resource.getInputStream(), gr.uoa.di.madgik.catalogue.domain.Model.class);
                boolean exists;
                try {
                    exists = modelService.get(m.getId()) != null;
                } catch (ResourceNotFoundException e) {
                    exists = false;
                } catch (Exception e) {
                    logger.error("Error checking existence of model [{}]: {}", m.getId(), e.getMessage());
                    continue;
                }
                modelStatus.put(m.getName() != null ? m.getName() : m.getId(), exists);

                if (exists) {
                    nonePosted = false;
                }
            } catch (Exception e) {
                logger.warn("Skipping model file [{}]: {}", resource.getFilename(), e.getMessage());
            }
        }

        boolean allLoaded = modelStatus.values().stream().allMatch(Boolean::booleanValue);
        boolean isLoading = !nonePosted && !allLoaded;

        model.addAttribute("modelStatus", modelStatus);
        model.addAttribute("allModelsLoading", isLoading);
        model.addAttribute("allModelsLoaded", allLoaded);

        return "wizard-step2";
    }

    @Operation(summary = "Load Models")
    @PostMapping("/step2/loadModels")
    public String loadModels() throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] modelFiles = resolver.getResources("classpath:models/*.json");

        for (Resource resource : modelFiles) {
            try {
                gr.uoa.di.madgik.catalogue.domain.Model m = objectMapper.readValue(resource.getInputStream(), gr.uoa.di.madgik.catalogue.domain.Model.class);

                boolean exists;
                try {
                    exists = modelService.get(m.getId()) != null;
                } catch (ResourceNotFoundException | NoSuchElementException e) {
                    exists = false;
                } catch (Exception e) {
                    logger.error("Error checking existence of model [{}]: {}", m.getId(), e.getMessage());
                    continue;
                }

                if (!exists) {
                    logger.info("Loading missing model [{}]", m.getId());
                    modelService.add(m);
                } else {
                    logger.debug("Model [{}] already exists, skipping.", m.getId());
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

}
