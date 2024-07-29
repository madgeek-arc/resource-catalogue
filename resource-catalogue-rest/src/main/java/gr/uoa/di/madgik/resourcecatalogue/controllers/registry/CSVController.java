package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

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

    @Operation(summary = "Downloads a csv file with Provider entries")
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

    @Operation(summary = "Downloads a csv file with Service entries")
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

    @Operation(summary = "Downloads a csv file with Vocabulary entries")
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

    @Operation(summary = "Downloads a csv file with the number of approved services per provider and country, before a specific date")
    @GetMapping(path = "approvedServicesByProviderAndCountry", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void servicesPerCountry(@Parameter(description = "Before date (format yyyy-MM-dd)", example = "2023-01-01")
                                   @RequestParam String date,
                                   @Parameter(hidden = true) Authentication auth,
                                   HttpServletResponse response) throws IOException {
        long timestamp;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = LocalDate.parse(date, formatter);
            timestamp = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Please use yyyy-MM-dd.");
        }
        Map<String, String> providerIdToCountry = new TreeMap<>();
        Map<String, String> providerIdToName = new TreeMap<>();
        Map<String, Integer> providerToServiceCountApprovedBeforeTimestamp = new TreeMap<>();
        List<String> providerOfServicesWithMalformedLoggingInfo = new ArrayList<>();
        Paging<ProviderBundle> providers = providerService.getAll(createFacetFilter(false), auth);
        for (ProviderBundle provider : providers.getResults()) {
            providerIdToName.put(provider.getId(), provider.getProvider().getName());
            providerIdToCountry.put(provider.getId(), provider.getProvider().getLocation().getCountry());
        }
        Paging<ServiceBundle> services = serviceBundleService.getAll(createFacetFilter(false), auth);
        for (ServiceBundle service : services.getResults()) {
            boolean approvedFound = false;
            boolean rejectedFound = false;
            String resourceOrganisation = service.getService().getResourceOrganisation();
            List<LoggingInfo> loggingInfoList = service.getLoggingInfo();
            for (LoggingInfo loggingInfo : loggingInfoList) {
                try {
                    if (loggingInfo.getActionType().equals("rejected")) {
                        rejectedFound = true;
                    }
                    if (loggingInfo.getActionType().equals("approved")) {
                        approvedFound = true;
                        long dateInMillis = Long.parseLong(loggingInfo.getDate());
                        if (dateInMillis < timestamp) {
                            providerToServiceCountApprovedBeforeTimestamp.put(resourceOrganisation,
                                    providerToServiceCountApprovedBeforeTimestamp.getOrDefault(resourceOrganisation, 0) + 1);
                        }
                        break;
                    }
                } catch (Exception e) {
                    if (!providerOfServicesWithMalformedLoggingInfo.contains(resourceOrganisation)
                            && service.getStatus().equals("approved resource")) {
                        providerOfServicesWithMalformedLoggingInfo.add(resourceOrganisation);
                    }
                }
            }
            // for old registries with no or malformed status
            if (!approvedFound && !rejectedFound) {
                providerToServiceCountApprovedBeforeTimestamp.put(resourceOrganisation,
                        providerToServiceCountApprovedBeforeTimestamp.getOrDefault(resourceOrganisation, 0) + 1);
            }
        }

        // for old registries with no or malformed logging info
        for (String resourceOrganisation : providerOfServicesWithMalformedLoggingInfo) {
            providerToServiceCountApprovedBeforeTimestamp.put(resourceOrganisation,
                    providerToServiceCountApprovedBeforeTimestamp.getOrDefault(resourceOrganisation, 0) + 1);
        }

        listNumberOfServicesPerProviderCountryToCSV(providerIdToCountry, providerIdToName,
                providerToServiceCountApprovedBeforeTimestamp, response);

//        for (Map.Entry<String, Integer> entry : providerToServiceCountApprovedBeforeTimestamp.entrySet()) {
//            logger.info(providerIdToName.get(entry.getKey()) + " : " + providerIdToCountry.get(entry.getKey()) + " : " + entry.getValue());
//        }
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

    private static void listNumberOfServicesPerProviderCountryToCSV(Map<String, String> providerIdToCountry,
                                                                    Map<String, String> providerIdToName,
                                                                    Map<String, Integer> providerToServiceCountApprovedBeforeTimestamp,
                                                                    HttpServletResponse response) throws IOException {
        // Sort the entries by country
        List<Map.Entry<String, Integer>> sortedEntries = providerToServiceCountApprovedBeforeTimestamp.entrySet().stream()
                .sorted((entry1, entry2) -> {
                    String country1 = providerIdToCountry.get(entry1.getKey());
                    String country2 = providerIdToCountry.get(entry2.getKey());
                    return country1.compareTo(country2);
                })
                .toList();

        // Create CSV content
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Provider Name,Country,Service Count\n");
        for (Map.Entry<String, Integer> entry : providerToServiceCountApprovedBeforeTimestamp.entrySet()) {
            String providerName = formatCSVField(providerIdToName.get(entry.getKey()));
            String country = formatCSVField(providerIdToCountry.get(entry.getKey()));
            String serviceCount = entry.getValue().toString();

            csvContent.append(providerName)
                    .append(',')
                    .append(country)
                    .append(',')
                    .append(serviceCount)
                    .append('\n');
        }

        // Set the response headers
        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"services_per_country.csv\"");

        // Write the CSV content to the response
        response.getWriter().write(csvContent.toString());
        response.getWriter().flush();
    }

    private static String formatCSVField(String field) {
        if (field == null) return "";
        if (field.contains("\"")) {
            field = field.replace("\"", "\"\"");
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            field = "\"" + field + "\"";
        }
        return field;
    }
}
