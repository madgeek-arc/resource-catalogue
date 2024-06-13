package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.DraftResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Profile("crud")
@RestController
@RequestMapping(path = "providers", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "providers")
public class ProviderCrudController extends ResourceCrudController<ProviderBundle> {

    private static final Logger logger = LogManager.getLogger(ProviderCrudController.class);
    private final ProviderService providerService;
    private final DraftResourceService<ProviderBundle> draftProviderService;
    private final GenericResourceService genericResourceService;

    ProviderCrudController(ProviderService providerService,
                           DraftResourceService<ProviderBundle> draftProviderService,
                           GenericResourceService genericResourceService) {
        super(providerService);
        this.providerService = providerService;
        this.draftProviderService = draftProviderService;
        this.genericResourceService = genericResourceService;
    }

    @PostMapping(path = "/bulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ProviderBundle> bundles, @Parameter(hidden = true) Authentication auth) {
        providerService.addBulk(bundles, auth);
    }

    @Override
    @PostMapping()
    public ResponseEntity<ProviderBundle> add(@RequestBody ProviderBundle providerBundle,
                                              @Parameter(hidden = true) Authentication auth) {
        if (providerBundle.isDraft()) {
            return draftProviderService.addCrud(providerBundle, auth);
        }
        return super.add(providerBundle, auth);
    }

    @Browse
    @GetMapping()
    public ResponseEntity<Paging<ProviderBundle>> getAll(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams,
                                                         @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("providers");
        return new ResponseEntity<>(genericResourceService.getResults(ff), HttpStatus.OK);
    }

}
