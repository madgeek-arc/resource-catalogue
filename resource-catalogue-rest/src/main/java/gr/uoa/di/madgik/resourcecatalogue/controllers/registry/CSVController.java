package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import gr.uoa.di.madgik.resourcecatalogue.utils.CSVService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Hidden
@Profile("beyond")
@RestController
@RequestMapping("exportToCSV")
@Tag(name = "csv", description = "Export information related to various Catalogue resources to CSV")
public class CSVController {

    private static Logger logger = LogManager.getLogger(CSVController.class);
    private final ServiceBundleService serviceBundleService;
    private final ProviderService providerService;
    private final VocabularyService vocabularyService;
    private final CSVService csvService;

    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    @Autowired
    CSVController(ServiceBundleService service, ProviderService provider, VocabularyService vocabulary, CSVService csvService) {
        this.serviceBundleService = service;
        this.providerService = provider;
        this.vocabularyService = vocabulary;
        this.csvService = csvService;
    }

    @Hidden
    @Operation(summary = "Downloads a csv file with Provider entries.")
    @GetMapping(path = "providers", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<String> providersToCSV(@RequestParam(required = false) Boolean published,
                                                 @Parameter(hidden = true) Authentication auth,
                                                 HttpServletResponse response) {
        Paging<ProviderBundle> providers = providerService.getAll(createFacetFilter(published), auth);
        String csvData = csvService.listProvidersToCSV(providers.getResults());
        response.setHeader("Content-disposition", "attachment; filename=" + "providers.csv");
        logger.info("User {} downloaded Providers CSV list", User.of(auth).getEmail());
        return ResponseEntity.ok(csvData);
    }

    @Hidden
    @Operation(summary = "Downloads a csv file with Service entries.")
    @GetMapping(path = "services", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<String> servicesToCSV(@RequestParam(required = false) Boolean published,
                                                @Parameter(hidden = true) Authentication auth,
                                                HttpServletResponse response) {
        Paging<ServiceBundle> serviceBundles = serviceBundleService.getAll(createFacetFilter(published), auth);
        String csvData = csvService.listServicesToCSV(serviceBundles.getResults());
        response.setHeader("Content-disposition", "attachment; filename=" + "services.csv");
        logger.info("User {} downloaded Services CSV list", User.of(auth).getEmail());
        return ResponseEntity.ok(csvData);
    }

    @Hidden
    @Operation(summary = "Downloads a csv file with Vocabulary entries.")
    @GetMapping(path = "vocabularies", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<String> vocabulariesToCSV(@Parameter(hidden = true) Authentication auth,
                                                    HttpServletResponse response) {
        Paging<Vocabulary> vocabularies = vocabularyService.getAll(createFacetFilter(null), auth);
        String csvData = csvService.listVocabulariesToCSV(vocabularies.getResults());
        response.setHeader("Content-disposition", "attachment; filename=" + "vocabularies.csv");
        logger.info("User {} downloaded Vocabularies CSV list", User.of(auth).getEmail());
        return ResponseEntity.ok(csvData);
    }

    @Hidden
    @Operation(summary = "Downloads a csv file with the number of approved services per provider and country, before a specific date.")
    @GetMapping(path = "approvedServicesByProviderAndCountry", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void numberOfServicesPerProviderCountryToCSV(@Parameter(description = "Before date (format yyyy-MM-dd)", example = "2023-01-01")
                                                        @RequestParam String date,
                                                        @Parameter(hidden = true) Authentication auth,
                                                        HttpServletResponse response) throws IOException {
        long timestamp = csvService.generateTimestampFromDate(date);
        List<ProviderBundle> providers = providerService.getAll(createFacetFilter(false), auth).getResults();
        List<ServiceBundle> services = serviceBundleService.getAll(createFacetFilter(false), auth).getResults();
        csvService.computeApprovedServicesBeforeTimestampAndGenerateCSV(timestamp, providers, services, response);
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
