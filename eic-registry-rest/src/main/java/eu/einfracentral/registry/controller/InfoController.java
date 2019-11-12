package eu.einfracentral.registry.controller;


import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.openminted.registry.core.domain.Facet;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("info")
@Api(value = "Get General Information")
public class InfoController {

    private static final String INFO = "general_INFO";
    private InfraServiceService<InfraService, InfraService> infraService;
    private ProviderService<Provider, Authentication> providerService;

    @Autowired
    InfoController(InfraServiceService<InfraService, InfraService> service, ProviderService<Provider, Authentication> provider) {
        this.infraService = service;
        this.providerService = provider;
    }

//    @ApiOperation(value = "Get Info about #SPs, #Services etc.")
    @RequestMapping(path = "all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Map<Object, Object>> getAllServicesNumbers(@ApiIgnore Authentication authentication) {
        Map<Object, Object> servicesInfo = new HashMap<>();
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", "true");
        servicesInfo.put("providers", (long) providerService.getAll(ff, authentication).getTotal());
        ff.addFilter("latest", "true");
        Paging<InfraService> infraServices = infraService.getAll(ff, null);
        servicesInfo.put("services", (long) infraServices.getTotal());
        for (Facet f : infraServices.getFacets()) {
            if (f.getField().equals("resourceType")) {
                continue;
            }
            servicesInfo.putIfAbsent(f.getField(), (long) f.getValues().size());
        }
        return ResponseEntity.ok(servicesInfo);
    }

}
