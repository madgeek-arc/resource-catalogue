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
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.CatalogueService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
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

@Profile("beyond")
@Controller
@RequestMapping("/wizard")
@Tag(name = "wizard")
public class WizardController {

    @Value("${catalogue.id}")
    private String catalogueId;

    private static final Logger logger = LoggerFactory.getLogger(WizardController.class);

    private final VocabularyService vocabularyService;
    private final ModelService modelService;

    public WizardController(VocabularyService vocabularyService,
                            ModelService modelService) {
        this.vocabularyService = vocabularyService;
        this.modelService = modelService;
    }

    @GetMapping("/step1")
    public String step1(Model model) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ClassPathResource vocabDir = new ClassPathResource("vocabularies");
        File[] vocabFiles = vocabDir.getFile().listFiles((dir, name) -> name.endsWith(".json"));

        Map<String, Boolean> vocabStatus = new TreeMap<>();
        boolean allPosted = true;

        for (File file : vocabFiles) {
            List<Vocabulary> vocabularies = objectMapper.readValue(
                    file,
                    new TypeReference<>() {
                    }
            );

            if (!vocabularies.isEmpty()) {
                String type = vocabularies.get(0).getType();
                int countInJson = vocabularies.size();
                int countInDb = vocabularyService.getByType(Vocabulary.Type.fromString(type)).size();
                boolean fullyPosted = countInDb >= countInJson;
                vocabStatus.put(type, fullyPosted);
                if (!fullyPosted) {
                    allPosted = false;
                }
            }
        }

        model.addAttribute("vocabStatus", vocabStatus);
        model.addAttribute("allVocabPosted", allPosted);
        return "wizard-step1";
    }

    @PostMapping("/step1/load")
    public String loadVocabularies() throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        ClassPathResource vocabDir = new ClassPathResource("vocabularies");
        File[] vocabFiles = vocabDir.getFile().listFiles((dir, name) -> name.endsWith(".json"));

        for (File file : vocabFiles) {
            List<Vocabulary> vocabularies = objectMapper.readValue(
                    file,
                    new TypeReference<>() {
                    }
            );

            if (!vocabularies.isEmpty()) {
                String type = vocabularies.get(0).getType();
                List<Vocabulary> vocs = vocabularyService.getByType(Vocabulary.Type.fromString(type));
                int countInDb = vocs.size();

                if (countInDb != vocabularies.size()) {
                    vocabularyService.deleteByType(Vocabulary.Type.fromString(type));
                }

                if (countInDb <  vocabularies.size()) {
                    logger.info("Loading vocabularies for type [{}]", type);
                    vocabularyService.addBulk(vocabularies, null);
                }
            }
        }
        return "redirect:/wizard/step1";
    }

    @Operation(summary = "Load models")
    @GetMapping("/step2")
    public String loadModels(Model model) throws IOException {
        // Load model files dynamically
        ClassPathResource modelDir = new ClassPathResource("models");
        File[] modelFiles = modelDir.getFile().listFiles((dir, name) -> name.endsWith(".json"));

        List<String> models = Arrays.stream(modelFiles)
                .map(File::getName)
                .sorted()
                .collect(Collectors.toList());

        model.addAttribute("models", models);
        return "wizard-step2";
    }

    @PostMapping("/step2/load")
    public String loadModelsPost(Model model) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ClassPathResource modelDir = new ClassPathResource("models");
        File[] modelFiles = modelDir.getFile().listFiles((dir, name) -> name.endsWith(".json"));

        for (File file : modelFiles) {
            gr.uoa.di.madgik.catalogue.ui.domain.Model singleModel = objectMapper.readValue(
                    file,
                    gr.uoa.di.madgik.catalogue.ui.domain.Model.class
            );
            modelService.add(singleModel);
        }

        model.addAttribute("message", "Models loaded successfully!");
        return "wizard-step2";
    }


    @Operation(summary = "Create main Catalogue")
    @GetMapping("/step3")
    public String createMainCatalogue(Model model) {
        Catalogue catalogue = new Catalogue();
        catalogue.setLocation(new ProviderLocation());
        catalogue.setMainContact(new ProviderMainContact());
        catalogue.setPublicContacts(new ArrayList<>(List.of(new ProviderPublicContact())));
        catalogue.setUsers(new ArrayList<>(List.of(new User())));
        model.addAttribute("catalogue", catalogue);
        model.addAttribute("id", catalogueId);
        return "wizard-step3";
    }

    @PostMapping("/save")
    public String saveCatalogue(@ModelAttribute Catalogue catalogue, Model model) {
        logger.info("Main Catalogue submitted");
        model.addAttribute("message", "Catalogue saved successfully!");
        return "wizard-finished";
    }
}
