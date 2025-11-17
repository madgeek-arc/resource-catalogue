package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@Profile("crud")
@RestController
@RequestMapping(path = "providertests", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "providertests")
public class ProviderTestCrudController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderTestCrudController.class);
    private final VocabularyService vocabularyService;
    private final GenericResourceService resourceService;

    ProviderTestCrudController(GenericResourceService resourceService, VocabularyService vocabularyService) {
        this.resourceService = resourceService;
        this.vocabularyService = vocabularyService;
    }

    @GetMapping(path = "all", produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<?> getAllTest(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        ff.setResourceType("providertest");
        return resourceService.getResults(ff);
    }

    @Operation(summary = "Returns the Provider with the given id.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> get(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                        @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                        @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                        @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        Object provider = resourceService.get("providertest", id);

        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public <T extends HashMap<String, Object>> T postTest(@RequestBody T provider) {
        NewBundle<T> bundleTest = new NewBundle<>();
        bundleTest.put("provider", provider);
//        bundleTest.put("id", provider.get("id"));
        bundleTest.setStatus(vocabularyService.get("pending provider").getId());
        NewBundle<T> obj = resourceService.add("providertest", bundleTest);
        return (T) obj.get("provider");
    }
}
