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

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Profile("beyond")
@RestController
@RequestMapping(path = "reference", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "reference")
public class ReferenceDataController<T extends Bundle> {

    GenericResourceService genericResourceService;

    @org.springframework.beans.factory.annotation.Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    public ReferenceDataController(GenericResourceService genericResourceService) {
        this.genericResourceService = genericResourceService;
    }

    @Hidden
    @GetMapping(path = "idToNameMap")
    public Map<String, List<gr.uoa.di.madgik.resourcecatalogue.dto.Value>> idToNameMap(@RequestParam String catalogueId,
                                                                                       @RequestParam String resourceType) {
        List<Bundle> bundles = Stream.concat(
                genericResourceService.getResults(createFacetFilter(catalogueId, false, resourceType))
                        .getResults()
                        .stream()
                        .filter(c -> c instanceof Bundle)
                        .map(c -> (Bundle) c),
                genericResourceService.getResults(createFacetFilter(catalogueId, true, resourceType))
                        .getResults()
                        .stream()
                        .filter(c -> c instanceof Bundle)
                        .map(c -> (Bundle) c)
                        .filter(b -> !b.getCatalogueId().equals(catalogueId))
        ).toList();

        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> allResources = bundles.stream()
                .map(b -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(
                        b.getId(),
                        b.getPayload().get("name").toString()
                ))
                .toList();

        return Map.of(resourceType + "_voc", allResources);
    }

    //TODO: transition to this
//    @Hidden
//    @GetMapping(path = "idToNameMap")
//    public List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> idToNameMap(@RequestParam String catalogueId,
//                                                                          @RequestParam List<String> resourceTypes) {
//        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> allResources = new ArrayList<>();
//
//        for (String resourceType : resourceTypes) {
//            List<Bundle> bundles = Stream.concat(
//                    genericResourceService.getResults(createFacetFilter(catalogueId, false, resourceType))
//                            .getResults()
//                            .stream()
//                            .filter(c -> c instanceof Bundle)
//                            .map(c -> (Bundle) c),
//                    genericResourceService.getResults(createFacetFilter(catalogueId, true, resourceType))
//                            .getResults()
//                            .stream()
//                            .filter(c -> c instanceof Bundle)
//                            .map(c -> (Bundle) c)
//                            .filter(b -> !b.getCatalogueId().equals(catalogueId))
//            ).toList();
//
//            allResources = bundles.stream()
//                    .map(b -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(
//                            b.getId(),
//                            b.getPayload().get("name").toString()
//                    ))
//                    .toList();
//        }
//
//        return allResources;
//    }

    private FacetFilter createFacetFilter(String catalogueId, boolean isPublic, String resourceType) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("status", "approved");
        ff.addFilter("active", true);
        ff.addFilter("draft", false);
        if (isPublic) {
            ff.addFilter("published", true);
        } else {
            ff.addFilter("catalogue_id", catalogueId);
            ff.addFilter("published", false);
        }
        ff.setResourceType(resourceType);
        return ff;
    }
}