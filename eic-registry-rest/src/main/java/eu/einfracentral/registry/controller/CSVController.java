package eu.einfracentral.registry.controller;

import com.google.gson.Gson;
import eu.einfracentral.domain.InfraService;
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
    private InfraServiceService<InfraService, InfraService> infraService;
    private ProviderService<ProviderBundle, Authentication> providerService;

    @Autowired
    CSVController(InfraServiceService<InfraService, InfraService> service, ProviderService<ProviderBundle, Authentication> provider) {
        this.infraService = service;
        this.providerService = provider;
    }

    // Downloads a csv file with Service entries
    @GetMapping(path = "services", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<String> servicesToCSV(@ApiIgnore Authentication auth, HttpServletResponse response) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("latest", true);
        Paging<InfraService> infraServices = infraService.getAll(ff, auth);
        String csvData = listToCSV(infraServices.getResults());
        response.setHeader("Content-disposition", "attachment; filename=" + "services.csv");
        return ResponseEntity.ok(csvData);
    }

    // Downloads a csv file with Provider entries
    @GetMapping(path = "providers", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<String> providersToCSV(@ApiIgnore Authentication auth, HttpServletResponse response) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        Paging<ProviderBundle> providers = providerService.getAll(ff, auth);
        String csvData = listToCSV(providers.getResults());
        response.setHeader("Content-disposition", "attachment; filename=" + "providers.csv");
        return ResponseEntity.ok(csvData);
    }

    private static String listToCSV(List<?> list) {
        String json = new Gson().toJson(list);
        JSONArray results = new JSONArray(json);
        return CDL.toString(results);
    }
}
