/**
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

package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Profile("beyond")
@RestController
@RequestMapping({"resourceTemplateBundles"})
@Tag(name = "resource template", description = "Operations for Provider Templates")
public class ResourceTemplateBundleController {

    private final ServiceBundleService serviceBundleService;
    private final TrainingResourceService trainingResourceService;

    public ResourceTemplateBundleController(ServiceBundleService serviceBundleService,
                                            TrainingResourceService trainingResourceService) {
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
    }

    // Get the Provider's Template (status = "pending provider" or "rejected provider")
    @GetMapping(path = {"templates"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Bundle<?> getProviderTemplate(@RequestParam String id, @Parameter(hidden = true) Authentication auth) {
        Bundle<?> template = serviceBundleService.getResourceTemplate(id, auth);
        if (template == null) {
            template = trainingResourceService.getResourceTemplate(id, auth);
        }
        return template;
    }
}
