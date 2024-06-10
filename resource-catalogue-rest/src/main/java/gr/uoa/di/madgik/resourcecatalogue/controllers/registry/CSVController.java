package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import com.google.gson.Gson;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.CDL;
import org.json.JSONArray;
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
import java.util.List;

@Profile("beyond")
@RestController
@RequestMapping("exportToCSV")
@Tag(name = "csv", description = "Download Providers and/or Services to CSV")
public class CSVController {

    private static Logger logger = LogManager.getLogger(CSVController.class);
    private final ServiceBundleService serviceBundleService;
    private final ProviderService providerService;

    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    @Autowired
    CSVController(ServiceBundleService service, ProviderService provider) {
        this.serviceBundleService = service;
        this.providerService = provider;
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

    private FacetFilter createFacetFilter(Boolean published) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        if (published != null) {
            ff.addFilter("published", published);
        }
        return ff;
    }

    private static String listToCSV(List<?> list) {
        String json = new Gson().toJson(list);
        JSONArray results = new JSONArray(json);
        return CDL.toString(results);
    }

    private static String listProvidersToCSV(List<ProviderBundle> list) {
        String resultCsv = listToCSV(list);
        String[] rows = resultCsv.split("\n");
        String[] header = rows[0].split(",");
        rows[0] = "id;abbreviation;name;" + String.join(";", header);
        for (int i = 1; i < rows.length; i++) {
            rows[i] = replaceDelimiters(rows[i], ',', ';');
            rows[i] = String.format("%s;%s;%s;%s", list.get(i - 1).getId(), list.get(i - 1).getProvider().getAbbreviation(),
                    list.get(i - 1).getProvider().getName(), rows[i]);
        }
        return String.join("\n", rows);
    }

    private static String listServicesToCSV(List<ServiceBundle> list) {
        String resultCsv = listToCSV(list);
        String[] rows = resultCsv.split("\n");
        String[] header = rows[0].split(",");
        rows[0] = "id;name;" + String.join(";", header);
        for (int i = 1; i < rows.length; i++) {
            rows[i] = replaceDelimiters(rows[i], ',', ';');
            rows[i] = String.format("%s;%s;%s", list.get(i - 1).getId(), list.get(i - 1).getService().getName(), rows[i]);
        }
        return String.join("\n", rows);
    }

    private static String replaceDelimiters(String row, char delimiter, char newDelimiter) {
        char[] rowInChars = row.toCharArray();

        // when encountering "{ treat it as a field opening and do not replace symbols as delimiters
        // instead wait for the closing counterpart }" and after that replace all delimiters found.
        boolean openedField = false;
        for (int i = 1; i < row.length(); i++) {
            if (!openedField && rowInChars[i - 1] == '"' && rowInChars[i] == '{') {
                openedField = true;
            } else if (openedField && rowInChars[i - 1] == '}' && rowInChars[i] == '"') {
                openedField = false;
            } else if (!openedField && rowInChars[i] == delimiter) {
                rowInChars[i] = newDelimiter;
            }
        }

        return new String(rowInChars);
    }
}
