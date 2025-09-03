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

import gr.uoa.di.madgik.resourcecatalogue.config.ResourceCatalogueInfo;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Hidden
@RestController
@RequestMapping("properties")
@Tag(name = "properties")
public class CatalogueInfoController {

    private final ResourceCatalogueInfo resourceCatalogueInfo;

    public record CatalogueConfiguration(String catalogueId, String catalogueName, String registrationEmail){}

    public CatalogueInfoController(ResourceCatalogueInfo resourceCatalogueInfo) {
        this.resourceCatalogueInfo = resourceCatalogueInfo;
    }

    @Hidden
    @Operation(summary = "Returns important application properties.")
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<CatalogueConfiguration> get() {
        CatalogueConfiguration conf = new CatalogueConfiguration(
                resourceCatalogueInfo.getCatalogueId(),
                resourceCatalogueInfo.getCatalogueName(),
                resourceCatalogueInfo.getCatalogueRegistrationEmail()
        );
        return new ResponseEntity<>(conf, HttpStatus.OK);
    }
}
