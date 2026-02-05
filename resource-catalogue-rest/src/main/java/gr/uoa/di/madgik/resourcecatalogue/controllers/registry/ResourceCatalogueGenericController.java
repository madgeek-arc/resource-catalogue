package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.Value;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceCatalogueGenericService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

//TODO: populated with common controller methods
//TODO: @Override on child methods that needs to override (eg. ServiceController full of @Tags)
public abstract class ResourceCatalogueGenericController<T extends Bundle, S extends ResourceCatalogueGenericService<T>> {

    protected final S service;
    protected final String resourceName;

    public ResourceCatalogueGenericController(S service, String resourceName) {
        this.service = service;
        this.resourceName = resourceName;
    }

    @GetMapping(path = "list")
    public List<Value> listResources(@RequestParam(required = false) String catalogueId) {
        return service.listResources(catalogueId);
    }
}
