package eu.openminted.registry.service;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.domain.Browsing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RequestController {

    @Autowired
    ResourceService resourceService;

    @Autowired
    RequestService requestService;


    @RequestMapping(value = "/request", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    public ResponseEntity<Browsing> getResourceTypeByFilters(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "resourceType", required = false, defaultValue = "") String resourceType,
            @RequestParam(value = "language", required = false, defaultValue = "") String language,
            @RequestParam(value = "mediaType", required = false, defaultValue = "") String mediaType,
            @RequestParam(value = "rights", required = false, defaultValue = "") String rights,
            @RequestParam(value = "mimeType", required = false, defaultValue = "") String mimeType,
            @RequestParam(value = "dataFormatSpecific", required = false, defaultValue = "") String dataFormatSpecific,
            @RequestParam(value = "licence", required = false, defaultValue = "") String licence,
            @RequestParam(value = "advanced", required = false, defaultValue = "true") boolean advanced,
            @RequestParam(value = "from", required = false, defaultValue = "0") int from,
            @RequestParam(value = "quantity", required = false, defaultValue = "10") int quantity) {

        FacetFilter facetFilter = new FacetFilter();
        facetFilter.addFilter("resourceType",resourceType);
        facetFilter.addFilter("language",language);
        facetFilter.addFilter("mediaType",mediaType);
        facetFilter.addFilter("rights",rights);
        facetFilter.addFilter("mimeType",mimeType);
        facetFilter.addFilter("dataFormatSpecific",dataFormatSpecific);
        facetFilter.addFilter("licence",licence);
        facetFilter.addFilter("application",advanced);
        facetFilter.setKeyword(keyword);
        facetFilter.setFrom(from);
        facetFilter.setQuantity(quantity);
        Browsing browsing = requestService.getResponseByFiltersElastic(facetFilter);
        return new ResponseEntity<>(browsing, HttpStatus.OK);

    }


}
