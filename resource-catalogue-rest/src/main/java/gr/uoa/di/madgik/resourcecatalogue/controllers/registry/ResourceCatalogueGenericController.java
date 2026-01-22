package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceCatalogueGenericService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

//TODO: populated with common controller methods
//TODO: @Override on child methods that needs to override (eg. ServiceController full of @Tags)
public abstract class ResourceCatalogueGenericController<T extends Bundle, S extends ResourceCatalogueGenericService<T>> {

    protected final S service;
    protected final String resourceName;

    private static final Logger logger = LoggerFactory.getLogger(ResourceCatalogueGenericController.class);

    public ResourceCatalogueGenericController(S service, String resourceName) {
        this.service = service;
        this.resourceName = resourceName;
    }

//    @Operation(summary = "Get a list of Resources based on a list of filters.")
//    @BrowseParameters
//    @BrowseCatalogue
//    @Parameter(name = "suspended", content = @Content(schema = @Schema(type = "boolean", nullable = true)))
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
