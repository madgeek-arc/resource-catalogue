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

package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.service.TemplateOnboardingService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@Hidden
@Profile("beyond")
@RestController
@RequestMapping(path = "resourceTemplate", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "resource template", description = "Operations for Provider Templates")
public class ResourceTemplateController {

    private final List<TemplateOnboardingService> onboardingServices;

    public ResourceTemplateController(List<TemplateOnboardingService> onboardingServices) {
        this.onboardingServices = onboardingServices;
    }

    @GetMapping("templates")
    public Bundle getProviderTemplate(@RequestParam String id, Authentication auth) {
        return onboardingServices.stream()
                .map(s -> s.getTemplate(id, auth))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
