/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.controllers.publicresources;

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.PublicResourceService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@Profile("beyond")
@RestController
@RequestMapping(path = "public/trainingResource", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "public training resource")
public class PublicTrainingResourceController extends BasePublicController<TrainingResourceBundle> {

    public PublicTrainingResourceController(PublicResourceService<TrainingResourceBundle> service) {
        super(service);
    }

    @Deprecated
    @GetMapping(path = "trainingResourceBundle/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT') or " +
            "@securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> getBundleDeprecated(@PathVariable String prefix,
                                                 @PathVariable String suffix,
                                                 @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity
                .ok()
                .header("Deprecation", "true")
                .header("Link", "</public/trainingResource/bundle/{prefix}/{suffix}>; rel=\"successor-version\"")
                .body(service.get(prefix + "/" + suffix));
    }

    @Deprecated
    @BrowseParameters
    @BrowseCatalogue
    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
    @GetMapping(path = "adminPage/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<Paging<TrainingResourceBundle>> getAllBundlesDeprecated(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("active", true);
        return ResponseEntity
                .ok()
                .header("Deprecation", "true")
                .header("Link", "</public/trainingResource/bundle/all>; rel=\"successor-version\"")
                .body(service.getAll(ff));
    }
}
