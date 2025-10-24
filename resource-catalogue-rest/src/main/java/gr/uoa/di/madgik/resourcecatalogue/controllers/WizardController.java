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

package gr.uoa.di.madgik.resourcecatalogue.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Profile("beyond")
@Controller
@RequestMapping("/wizard")
@Tag(name = "wizard")
public class WizardController {

    @Value("${catalogue.id}")
    private String catalogueId;

    @Value("${catalogue.homepage}")
    private String homepage;

    private static final Logger logger = LoggerFactory.getLogger(WizardController.class);

    private final VocabularyService vocabularyService;
    private final ModelService modelService;
    private final GenericResourceService genericService;

    public WizardController(VocabularyService vocabularyService,
                            ModelService modelService,
                            GenericResourceService genericService) {
        this.vocabularyService = vocabularyService;
        this.modelService = modelService;
        this.genericService = genericService;
    }

    @Operation(summary = "Check Vocabularies Existence")
    @GetMapping("/step1")
    public String checkVocabulariesExistence(Model model) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ClassPathResource vocabDir = new ClassPathResource("vocabularies");
        File[] vocabFiles = vocabDir.getFile().listFiles((dir, name) -> name.endsWith(".json"));

        Map<String, Boolean> vocabStatus = new TreeMap<>();
        boolean allLoaded = true;

        for (File file : vocabFiles) {
            List<Vocabulary> vocabularies = objectMapper.readValue(file, new TypeReference<>() {
            });

            if (!vocabularies.isEmpty()) {
                String type = vocabularies.get(0).getType();
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
        ObjectMapper objectMapper = new ObjectMapper();
        ClassPathResource vocabDir = new ClassPathResource("vocabularies");
        File[] vocabularyFiles = vocabDir.getFile().listFiles((dir, name) -> name.endsWith(".json"));

        if (vocabularyFiles != null) {
            for (File file : vocabularyFiles) {
                List<Vocabulary> vocabularies = objectMapper.readValue(file, new TypeReference<>() {
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
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ClassPathResource modelDir = new ClassPathResource("models");
        File[] modelFiles = modelDir.getFile().listFiles((dir, name) -> name.endsWith(".json"));

        if (modelFiles == null || modelFiles.length == 0) {
            model.addAttribute("modelStatus", Collections.emptyMap());
            model.addAttribute("allModelsLoading", false);
            model.addAttribute("allModelsLoaded", true);
            return "wizard-step2";
        }

        Map<String, Boolean> modelStatus = new TreeMap<>();
        boolean nonePosted = true;

        for (File file : modelFiles) {
            try {
                gr.uoa.di.madgik.catalogue.ui.domain.Model m = objectMapper.readValue(file, gr.uoa.di.madgik.catalogue.ui.domain.Model.class);
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
                logger.warn("Skipping model file [{}]: {}", file.getName(), e.getMessage());
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
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ClassPathResource modelDir = new ClassPathResource("models");
        File[] modelFiles = modelDir.getFile().listFiles((dir, name) -> name.endsWith(".json"));

        for (File file : modelFiles) {
            try {
                gr.uoa.di.madgik.catalogue.ui.domain.Model m = objectMapper.readValue(file, gr.uoa.di.madgik.catalogue.ui.domain.Model.class);

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
                logger.error("Failed to process model file [{}]: {}", file.getName(), e.getMessage());
            }
        }

        return "redirect:/wizard/step2";
    }


    @Operation(summary = "Create main Catalogue")
    @GetMapping("/step3")
    public String createCatalogue(Model model) {
        Catalogue catalogue = new Catalogue();
        catalogue.setId(catalogueId);
        catalogue.setLocation(new ProviderLocation());
        catalogue.setMainContact(new ProviderMainContact());
        catalogue.setPublicContacts(new ArrayList<>(List.of(new ProviderPublicContact())));
        catalogue.setUsers(new ArrayList<>(List.of(new User())));

        // add country vocabularies
        List<String> countries = vocabularyService.getByType(Vocabulary.Type.COUNTRY).stream().map(Vocabulary::getId).toList();

        model.addAttribute("countries", countries);
        model.addAttribute("catalogue", catalogue);
        model.addAttribute("id", catalogueId);
        return "wizard-step3";
    }

    @PostMapping("/step3/loadCatalogue")
    public String loadCatalogue(@ModelAttribute Catalogue catalogue, Model model) {
        try {
            logger.info("Loading main Catalogue with ID [{}]", catalogue.getId());
            addCatalogue(new CatalogueBundle(catalogue));
            model.addAttribute("successMessage", "Catalogue saved successfully!");
        } catch (Exception e) {
            logger.error("Failed to save Catalogue [{}]: {}", catalogue.getId(), e.getMessage());
            model.addAttribute("errorMessage", "Error saving catalogue: " + e.getMessage());
            model.addAttribute("catalogue", catalogue);
        }

        model.addAttribute("id", catalogue.getId());
        model.addAttribute("homepage", homepage);
        return "wizard-step3";
    }

    private void addCatalogue(CatalogueBundle catalogue) {

        catalogue.setMetadata(Metadata.createMetadata("system", "system"));
        List<LoggingInfo> loggingInfoList = createLoggingInfoList();
        catalogue.setLoggingInfo(loggingInfoList);
        catalogue.setActive(true);
        catalogue.setStatus(vocabularyService.get("approved catalogue").getId());
        catalogue.setAuditState(Auditable.NOT_AUDITED);

        // latestOnboardingInfo
        catalogue.setLatestOnboardingInfo(loggingInfoList.getFirst());

        genericService.add("catalogue", catalogue);
    }

    private static List<LoggingInfo> createLoggingInfoList() {
        String currentTime = String.valueOf(System.currentTimeMillis());
        String system = "system";
        String type = LoggingInfo.Types.ONBOARD.getKey();

        return Stream.of(LoggingInfo.ActionType.REGISTERED, LoggingInfo.ActionType.APPROVED)
                .map(action -> {
                    LoggingInfo info = new LoggingInfo();
                    info.setDate(currentTime);
                    info.setType(type);
                    info.setActionType(action.getKey());
                    info.setUserEmail(system);
                    info.setUserFullName(system);
                    info.setUserRole(system);
                    return info;
                })
                .collect(Collectors.toList());
    }
}
