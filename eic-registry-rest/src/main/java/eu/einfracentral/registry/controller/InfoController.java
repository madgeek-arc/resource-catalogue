package eu.einfracentral.registry.controller;


import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.openminted.registry.core.domain.Facet;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("info")
@Api(value = "Get General Information")
public class InfoController {

    private final ResourceBundleService<ServiceBundle> resourceBundleService;
    private final ProviderService<ProviderBundle, Authentication> providerService;

    @Autowired
    InfoController(ResourceBundleService<ServiceBundle> service, ProviderService<ProviderBundle, Authentication> provider) {
        this.resourceBundleService = service;
        this.providerService = provider;
    }

    // Get Info about #SPs, #Services etc.
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<Object, Object>> getAllServicesNumbers(@ApiIgnore Authentication authentication) {
        Map<Object, Object> servicesInfo = new HashMap<>();
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", true);
        servicesInfo.put("providers", (long) providerService.getAll(ff, authentication).getTotal());
        Paging<ServiceBundle> serviceBundles = resourceBundleService.getAll(ff, null);
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
