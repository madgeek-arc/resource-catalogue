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

import gr.uoa.di.madgik.resourcecatalogue.config.NodeProperties;
import gr.uoa.di.madgik.resourcecatalogue.service.OnboardingPolicyProvider;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Hidden
@RestController
@RequestMapping(path = "config", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "config")
public class CatalogueInfoController {

    @Value("${catalogue.emails.support}")
    private String catalogueSupportEmail;

    private final NodeProperties nodeProperties;
    private final ObjectProvider<OnboardingPolicyProvider> onboardingPolicyProvider;

    /**
     * @param onboardingPolicy the onboarding policy ({@code review-all}, {@code review-first} or
     *                         {@code review-none}) by resource type, empty if the node has no configurable policy
     */
    public record CatalogueConfiguration(String catalogueSupportEmail, String nodePid,
                                         boolean nodePidFixed, Map<String, String> onboardingPolicy) {
    }

    public CatalogueInfoController(NodeProperties nodeProperties,
                                   ObjectProvider<OnboardingPolicyProvider> onboardingPolicyProvider) {
        this.nodeProperties = nodeProperties;
        this.onboardingPolicyProvider = onboardingPolicyProvider;
    }

    @Hidden
    @Operation(summary = "Returns important application properties.")
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<CatalogueConfiguration> get() {
        CatalogueConfiguration conf = new CatalogueConfiguration(
                catalogueSupportEmail,
                nodeProperties.getPid().getValue(),
                nodeProperties.getPid().isFixed(),
                onboardingPolicyProvider.stream().findFirst().map(OnboardingPolicyProvider::getPolicies)
                        .orElse(Map.of())
        );
        return new ResponseEntity<>(conf, HttpStatus.OK);
    }
}
