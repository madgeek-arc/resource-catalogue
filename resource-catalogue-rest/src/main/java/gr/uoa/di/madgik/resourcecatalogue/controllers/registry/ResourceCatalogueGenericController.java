package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceCatalogueGenericService;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

//TODO: populated with common controller methods
//TODO: @Override on child methods that needs to override (eg. ServiceController full of @Tags)
public abstract class ResourceCatalogueGenericController<T extends Bundle, S extends ResourceCatalogueGenericService<T>> {

    protected final S service;
    protected final String resourceName;

    private static final Logger logger = LoggerFactory.getLogger(ResourceCatalogueGenericController.class);

    @org.springframework.beans.factory.annotation.Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    public ResourceCatalogueGenericController(S service, String resourceName) {
        this.service = service;
        this.resourceName = resourceName;
    }

    @Hidden
    @GetMapping(path = "idToNameMap")
    public List<ParentValue> idToNameMap(@RequestParam String catalogueId,
                                         @RequestParam String resourceType) {
        List<Bundle> bundles = Stream.concat(
                service.getAll(createFacetFilter(catalogueId, false, resourceType))
                        .getResults()
                        .stream()
                        .filter(Objects::nonNull)
                        .map(c -> (Bundle) c),
                service.getAll(createFacetFilter(catalogueId, true, resourceType))
                        .getResults()
                        .stream()
                        .filter(Objects::nonNull)
                        .map(c -> (Bundle) c)
                        .filter(b -> !b.getCatalogueId().equals(catalogueId))
        ).toList();

        List<gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue> allResources = bundles.stream()
                .map(b -> new gr.uoa.di.madgik.resourcecatalogue.dto.ParentValue(
                        b.getId(),
                        b.getPayload().get("name").toString(),
                        resourceType
                ))
                .toList();

        return allResources;
    }

    private FacetFilter createFacetFilter(String catalogueId, boolean isPublic, String resourceType) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("status", "approved");
        ff.addFilter("active", true);
        ff.addFilter("draft", false);
        if (isPublic) {
            ff.addFilter("published", true);
        } else {
            ff.addFilter("catalogue_id", catalogueId);
            ff.addFilter("published", false);
        }
        ff.setResourceType(resourceType);
        return ff;
    }

//    @Operation(summary = "Get a list of Resources based on a list of filters.")
//    @BrowseParameters
//    @BrowseCatalogue
//    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false", nullable = true)))
//    @GetMapping(path = "all")
//    public ResponseEntity<Paging<?>> getAll(@Parameter(hidden = true)
//                                            @RequestParam MultiValueMap<String, Object> params,
//                                            @Parameter(hidden = true) Authentication auth) {
//        FacetFilter ff = FacetFilter.from(params);
//        ff.addFilter("published", false);
//        ff.addFilter("draft", false);
//        Paging<T> paging = service.getAll(ff, auth);
//        logger.info("generic and provider");
//        return ResponseEntity.ok(paging.map(T::getPayload));
//    }
}
