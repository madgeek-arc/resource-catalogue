package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;


import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("info")
@Tag(name = "Info Controller", description = "Get General Information")
public class InfoController {

    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final ProviderService<ProviderBundle, Authentication> providerService;

    InfoController(ServiceBundleService<ServiceBundle> service, ProviderService<ProviderBundle, Authentication> provider) {
        this.serviceBundleService = service;
        this.providerService = provider;
    }

    // Get Info about #SPs, #Services etc.
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<Object, Object>> getAllServicesNumbers(@Parameter(hidden = true) Authentication authentication) {
        Map<Object, Object> servicesInfo = new HashMap<>();
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", true);
        servicesInfo.put("providers", (long) providerService.getAll(ff, authentication).getTotal());
        Paging<ServiceBundle> serviceBundles = serviceBundleService.getAll(ff, null);
        servicesInfo.put("services", (long) serviceBundles.getTotal());
        for (Facet f : serviceBundles.getFacets()) {
            if (f.getField().equals("resourceType")) {
                continue;
            }
            servicesInfo.putIfAbsent(f.getField(), (long) f.getValues().size());
        }
        return ResponseEntity.ok(servicesInfo);
    }

}