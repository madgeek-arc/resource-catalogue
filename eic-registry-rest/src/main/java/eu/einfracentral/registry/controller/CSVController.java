package eu.einfracentral.registry.controller;

import com.google.gson.Gson;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.CDL;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("exportToCSV")
public class CSVController {

    private static Logger logger = LogManager.getLogger(CSVController.class);
    private final InfraServiceService<ServiceBundle, ServiceBundle> infraService;
    private final ProviderService<ProviderBundle, Authentication> providerService;

    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    @Autowired
    CSVController(InfraServiceService<ServiceBundle, ServiceBundle> service, ProviderService<ProviderBundle, Authentication> provider) {
        this.infraService = service;
        this.providerService = provider;
    }

    // Downloads a csv file with Service entries
    @GetMapping(path = "services", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<String> servicesToCSV(@ApiIgnore Authentication auth, HttpServletResponse response) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        Paging<ServiceBundle> infraServices = infraService.getAll(ff, auth);
        String csvData = listServicesToCSV(infraServices.getResults());
        response.setHeader("Content-disposition", "attachment; filename=" + "services.csv");
        return ResponseEntity.ok(csvData);
    }

    // Downloads a csv file with Provider entries
    @GetMapping(path = "providers", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<String> providersToCSV(@ApiIgnore Authentication auth, HttpServletResponse response) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        Paging<ProviderBundle> providers = providerService.getAll(ff, auth);
        String csvData = listProvidersToCSV(providers.getResults());
        response.setHeader("Content-disposition", "attachment; filename=" + "providers.csv");
        return ResponseEntity.ok(csvData);
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
            rows[i] = String.format("%s;%s;%s;%s", list.get(i-1).getId(), list.get(i-1).getProvider().getAbbreviation(),
                    list.get(i-1).getProvider().getName(), rows[i]);
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
            rows[i] = String.format("%s;%s;%s", list.get(i-1).getId(), list.get(i-1).getService().getName(), rows[i]);
        }
        return String.join("\n", rows);
    }

    private static String replaceDelimiters(String row, char delimiter, char newDelimiter) {
        char[] rowInChars = row.toCharArray();

        // when encountering "{ treat it as a field opening and do not replace symbols as delimiters
        // instead wait for the closing counterpart }" and after that replace all delimiters found.
        boolean openedField = false;
        for (int i = 1; i < row.length(); i++) {
            if (!openedField && rowInChars[i-1] == '"' && rowInChars[i] == '{') {
                openedField = true;
            } else if (openedField && rowInChars[i-1] == '}' && rowInChars[i] == '"') {
                openedField = false;
            } else if (!openedField && rowInChars[i] == delimiter) {
                rowInChars[i] = newDelimiter;
            }
        }

        return new String(rowInChars);
    }
}
