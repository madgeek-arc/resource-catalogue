package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Profile("beyond")
@RestController
@RequestMapping("exportToCSV")
@Tag(name = "csv", description = "Download Providers and/or Services to CSV")
public class CSVController {

    private static Logger logger = LogManager.getLogger(CSVController.class);
    private final ServiceBundleService serviceBundleService;
    private final ProviderService providerService;
    private final VocabularyService vocabularyService;

    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    @Autowired
    CSVController(ServiceBundleService service, ProviderService provider, VocabularyService vocabulary) {
        this.serviceBundleService = service;
        this.providerService = provider;
        this.vocabularyService = vocabulary;
    }

    // Downloads a csv file with Provider entries
    @GetMapping(path = "providers", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<String> providersToCSV(@RequestParam(required = false) Boolean published,
                                                 @Parameter(hidden = true) Authentication auth,
                                                 HttpServletResponse response) {
        Paging<ProviderBundle> providers = providerService.getAll(createFacetFilter(published), auth);
        String csvData = listProvidersToCSV(providers.getResults());
        response.setHeader("Content-disposition", "attachment; filename=" + "providers.csv");
        logger.info("User {} downloaded Providers CSV list", User.of(auth).getEmail());
        return ResponseEntity.ok(csvData);
    }

    // Downloads a csv file with Service entries
    @GetMapping(path = "services", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<String> servicesToCSV(@RequestParam(required = false) Boolean published,
                                                @Parameter(hidden = true) Authentication auth,
                                                HttpServletResponse response) {
        Paging<ServiceBundle> serviceBundles = serviceBundleService.getAll(createFacetFilter(published), auth);
        String csvData = listServicesToCSV(serviceBundles.getResults());
        response.setHeader("Content-disposition", "attachment; filename=" + "services.csv");
        logger.info("User {} downloaded Services CSV list", User.of(auth).getEmail());
        return ResponseEntity.ok(csvData);
    }

    // Downloads a csv file with Provider entries
    @GetMapping(path = "vocabularies", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<String> vocabulariesToCSV(@Parameter(hidden = true) Authentication auth,
                                                    HttpServletResponse response) {
        Paging<Vocabulary> vocabularies = vocabularyService.getAll(createFacetFilter(null), auth);
        String csvData = listVocabulariesToCSV(vocabularies.getResults());
        response.setHeader("Content-disposition", "attachment; filename=" + "vocabularies.csv");
        logger.info("User {} downloaded Vocabularies CSV list", User.of(auth).getEmail());
        return ResponseEntity.ok(csvData);
    }

    private FacetFilter createFacetFilter(Boolean published) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        if (published != null) {
            ff.addFilter("published", published);
        }
        return ff;
    }

    private static String listProvidersToCSV(List<ProviderBundle> list) {
        List<Provider> providers = new ArrayList<>();
        for (ProviderBundle providerBundle : list) {
            providers.add(providerBundle.getProvider());
        }
        providers.sort(Comparator.comparing(Provider::getId));

        StringBuilder csv = new StringBuilder();
        csv.append("id,name\n"); // CSV header

        for (Provider provider : providers) {
            csv.append(formatCSVField(provider.getId())).append(",");
            csv.append(formatCSVField(provider.getName())).append("\n");
        }

        return csv.toString();
    }

    private static String listServicesToCSV(List<ServiceBundle> list) {
        List<Service> services = new ArrayList<>();
        for (ServiceBundle serviceBundle : list) {
            services.add(serviceBundle.getService());
        }
        services.sort(Comparator.comparing(Service::getId));

        StringBuilder csv = new StringBuilder();
        csv.append("id,name\n"); // CSV header

        for (Service service : services) {
            csv.append(formatCSVField(service.getId())).append(",");
            csv.append(formatCSVField(service.getName())).append("\n");
        }

        return csv.toString();
    }

    private static String listVocabulariesToCSV(List<Vocabulary> list) {
        list.sort(Comparator.comparing(Vocabulary::getId));

        StringBuilder csv = new StringBuilder();
        csv.append("id,name,description,parentId,type,extras\n"); // CSV header

        for (Vocabulary vocabulary : list) {
            csv.append(formatCSVField(vocabulary.getId())).append(",");
            csv.append(formatCSVField(vocabulary.getName())).append(",");
            csv.append(formatCSVField(vocabulary.getDescription())).append(",");
            csv.append(formatCSVField(vocabulary.getParentId())).append(",");

            csv.append(formatCSVField(vocabulary.getType())).append("\n");
        }

        return csv.toString();
    }

    private static String formatCSVField(String field) {
        if (field == null) return "";
        if (field.contains(",")) {
            // If the field contains commas, enclose it in double quotes
            return "\"" + field + "\"";
        } else {
            return field;
        }
    }
}
