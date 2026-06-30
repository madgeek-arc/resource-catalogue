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

import gr.uoa.di.madgik.resourcecatalogue.dto.DuplicatePair;
import gr.uoa.di.madgik.resourcecatalogue.service.DeduplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;

@Profile("beyond")
@RestController
@RequestMapping(path = "dedup", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "deduplication")
public class DeduplicationController {

    private final DeduplicationService deduplicationService;

    public DeduplicationController(DeduplicationService deduplicationService) {
        this.deduplicationService = deduplicationService;
    }

    @Operation(summary = "Scan all published resources of the given type and return similar pairs.")
    @GetMapping(path = "{resourceType}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<List<DuplicatePair>> findDuplicates(
            @PathVariable String resourceType,
            @RequestParam(defaultValue = "5") int quantity,
            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(deduplicationService.findDuplicates(resourceType, quantity));
    }

    @Operation(summary = "Find published resources similar to the one identified by {prefix}/{suffix}.")
    @GetMapping(path = "{resourceType}/{prefix}/{suffix}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<List<LinkedHashMap<String, Object>>> findSimilar(
            @PathVariable String resourceType,
            @PathVariable String prefix,
            @PathVariable String suffix,
            @RequestParam(defaultValue = "5") int quantity,
            @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(deduplicationService.findSimilar(resourceType, prefix + "/" + suffix, quantity));
    }
}
