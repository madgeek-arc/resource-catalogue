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

package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Profile("crud")
@RestController
@RequestMapping(path = "datasources")
@Tag(name = "datasources")
public class DatasourceCrudController extends ResourceCrudController<DatasourceBundle> {

    public DatasourceCrudController(GenericResourceService service) {
        super(service);
    }

    @Override
    protected String getResourceTypeName() {
        return "datasource";
    }

    @Operation(summary = "Returns the Datasource of the given Service of the given Catalogue.")
    @GetMapping(path = "/byService/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> getDatasourceByServiceId(@PathVariable("serviceId") String serviceId,
                                                           @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                           @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("service_id", serviceId);
        Paging<DatasourceBundle> page = service.getResults(ff);
        List<DatasourceBundle> allDatasources = page.getResults();
        if (!allDatasources.isEmpty()) {
            return new ResponseEntity<>(allDatasources.getFirst().getDatasource(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
