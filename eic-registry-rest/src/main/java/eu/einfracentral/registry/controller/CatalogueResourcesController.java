package eu.einfracentral.registry.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.ResourceBundle;
import eu.einfracentral.service.GenericResourceService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("catalogue-resources")
public class CatalogueResourcesController {

    private static final Logger logger = LogManager.getLogger(CatalogueResourcesController.class);
    private final GenericResourceService genericResourceService;
    private final ObjectMapper objectMapper;


    public CatalogueResourcesController(GenericResourceService genericResourceService) {
        this.genericResourceService = genericResourceService;
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @GetMapping("{id}")
    public <T extends Bundle<?>> Object get(@PathVariable("id") String id) {
        T bundle = genericResourceService.get("resources", id);
        return bundle.getPayload();
    }

    @GetMapping("{id}/resourceType")
    public Map.Entry<String, String> getResourceType(@PathVariable("id") String id) {
        Resource resource = genericResourceService.searchResource("resources", id, true);
        return new AbstractMap.SimpleEntry<>("resourceType", resource.getResourceTypeName());
    }

    @ApiOperation(value = "Browse Catalogue Resources.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataTypeClass = String.class, paramType = "query")
    })
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Paging<?> getCatalogueResources(@ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = FacetFilterUtils.createFacetFilter(allRequestParams);
        filter.setResourceType("resources");
        Paging<?> paging = genericResourceService.getResults(filter)/*.map(r -> ((ResourceBundle<?>) r).getPayload())*/;
        return paging;
    }

    @ApiOperation(value = "Get all Resources in the catalogue organized by an attribute, e.g. get Resources organized in categories.")
    @GetMapping(path = "by/{field}")
    public <T extends ResourceBundle<? extends eu.einfracentral.domain.Service>> Map<String, List<?>> getBy(@PathVariable(value = "field") String field) {
        Map<String, List<T>> results;
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10_000);
        filter.setResourceType("resources");
        results = genericResourceService.getResultsGrouped(filter, field);
        Map<String, List<?>> resources = new TreeMap<>();
        results.forEach((key, value) ->
                resources.put(getResourceName(key), value
                        .stream()
                        .map(Bundle::getPayload)
                        .sorted(Comparator.comparing(eu.einfracentral.domain.Service::getName))
                        .collect(Collectors.toList())
                )
        );
        return resources;
    }

    private String getResourceName(String key) {
        String name = key;
        try {
            Object result = genericResourceService.get("resourceTypes", key);
            IdName idName = objectMapper.convertValue(result, IdName.class);
            name = idName.getName();
        } catch (Exception e) {
            logger.warn("Could not find resource name. Using id instead.", e);
        }
        return name;
    }

    private static class IdName {
        String id;
        String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
