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

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import gr.uoa.di.madgik.resourcecatalogue.utils.CSVService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Hidden
@Profile("beyond")
@RestController
@RequestMapping("exportToCSV")
@Tag(name = "csv", description = "Export information related to various Catalogue resources to CSV")
public class CSVController {

    private static final Logger logger = LoggerFactory.getLogger(CSVController.class);
    private final ServiceService service;
    private final OrganisationService organisationService;
    private final VocabularyService vocabularyService;
    private final CSVService csvService;
    private final ServiceService serviceService;

    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    CSVController(ServiceService service, OrganisationService provider,
                  VocabularyService vocabulary, CSVService csvService, ServiceService serviceService) {
        this.service = service;
        this.organisationService = provider;
        this.vocabularyService = vocabulary;
        this.csvService = csvService;
        this.serviceService = serviceService;
    }

    @Hidden
    @Operation(summary = "Downloads a csv file with Organisation entries.")
    @GetMapping(path = "providers", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<String> providersToCSV(@RequestParam(required = false) Boolean published,
                                                 @Parameter(hidden = true) Authentication auth,
                                                 HttpServletResponse response) {
        Paging<OrganisationBundle> providers = organisationService.getAll(createFacetFilter(published), auth);
        String csvData = csvService.listProvidersToCSV(providers.getResults());
        response.setHeader("Content-disposition", "attachment; filename=" + "providers.csv");
        logger.info("Downloaded Providers CSV list");
        return ResponseEntity.ok(csvData);
    }

    @Hidden
    @Operation(summary = "Downloads a csv file with Service entries.")
    @GetMapping(path = "services", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<String> servicesToCSV(@RequestParam(required = false) Boolean published,
                                                @Parameter(hidden = true) Authentication auth,
                                                HttpServletResponse response) {
        Paging<ServiceBundle> serviceBundles = serviceService.getAll(createFacetFilter(published), auth);
        String csvData = csvService.listServicesToCSV(serviceBundles.getResults());
        response.setHeader("Content-disposition", "attachment; filename=" + "services.csv");
        logger.info("Downloaded Services CSV list");
        return ResponseEntity.ok(csvData);
    }

    @Hidden
    @Operation(summary = "Downloads a csv file with Vocabulary entries.")
    @GetMapping(path = "vocabularies", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public ResponseEntity<String> vocabulariesToCSV(@Parameter(hidden = true) Authentication auth,
                                                    HttpServletResponse response) {
        Paging<Vocabulary> vocabularies = vocabularyService.getAll(createFacetFilter(null), auth);
        String csvData = csvService.listVocabulariesToCSV(vocabularies.getResults());
        response.setHeader("Content-disposition", "attachment; filename=" + "vocabularies.csv");
        logger.info("Downloaded Vocabularies CSV list");
        return ResponseEntity.ok(csvData);
    }

    @Hidden
    @Operation(summary = "Downloads a csv file with the number of approved services per provider and country, before a specific date.")
    @GetMapping(path = "approvedServicesByProviderAndCountry", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EPOT')")
    public void numberOfServicesPerProviderCountryToCSV(@Parameter(description = "Before date (format yyyy-MM-dd)",
                                                                example = "2023-01-01")
                                                        @RequestParam String date,
                                                        @Parameter(hidden = true) Authentication auth,
                                                        HttpServletResponse response) throws IOException {
        long timestamp = csvService.generateTimestampFromDate(date);
        List<OrganisationBundle> providers = organisationService.getAll(createFacetFilter(false), auth).getResults();
        List<ServiceBundle> services = serviceService.getAll(createFacetFilter(false), auth).getResults();
        String csv = csvService.computeApprovedServicesBeforeTimestampAndGenerateCSV(timestamp, providers, services);

        // Set the response headers
        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"services_per_country.csv\"");

        // Write the CSV content to the response
        response.getWriter().write(csv);
        response.getWriter().flush();
    }

    private FacetFilter createFacetFilter(Boolean published) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        if (published != null) {
            ff.addFilter("published", published);
        }
        return ff;
    }
}
