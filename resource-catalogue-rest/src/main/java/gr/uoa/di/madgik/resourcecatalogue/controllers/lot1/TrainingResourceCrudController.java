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

package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Profile("crud")
@RestController
@RequestMapping(path = "training-resources")
@Tag(name = "training resources")
public class TrainingResourceCrudController extends ResourceCrudController<TrainingResourceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(TrainingResourceCrudController.class.getName());
    private final TrainingResourceService trainingResourceService;

    TrainingResourceCrudController(TrainingResourceService trainingResourceService) {
        super(trainingResourceService);
        this.trainingResourceService = trainingResourceService;
    }

    @PostMapping(path = "/bulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<TrainingResourceBundle> bundles, @Parameter(hidden = true) Authentication auth) {
        trainingResourceService.addBulk(bundles, auth);
    }
}
