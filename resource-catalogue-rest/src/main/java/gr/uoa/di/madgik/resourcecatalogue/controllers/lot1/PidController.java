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

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.service.PidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Profile("crud")
@RestController
@RequestMapping("pids")
@Tag(name = "pids", description = "PID related operations")
public class PidController {

    private final PidService pidService;

    public PidController(PidService pidService) {
        this.pidService = pidService;
    }

    @Operation(summary = "Returns the Resource with the given PID.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> get(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                 @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        Bundle<?> bundle = pidService.get(prefix, suffix);
        if (bundle != null) {
            return new ResponseEntity<>(bundle.getPayload(), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @Operation(summary = "Register a resource on the PID service")
    @PostMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> register(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                      @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                      @Parameter(description = "A list of resolve endpoints") @RequestParam(value = "resolveEndpoints", required = false) List<String> resolveEndpoints) {
        Bundle<?> bundle = pidService.get(prefix, suffix);
        if (bundle != null) {
            pidService.register(bundle.getId(), resolveEndpoints);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }
}
