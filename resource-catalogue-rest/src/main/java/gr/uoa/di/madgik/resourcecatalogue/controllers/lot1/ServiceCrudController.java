package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.DraftResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Profile("crud")
@RestController
@RequestMapping(path = "services")
@Tag(name = "services")
public class ServiceCrudController extends ResourceCrudController<ServiceBundle> {

    private static final Logger logger = LogManager.getLogger(ServiceCrudController.class.getName());
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final DraftResourceService<ServiceBundle> draftServiceService;
    private final GenericResourceService genericResourceService;

    ServiceCrudController(ServiceBundleService<ServiceBundle> serviceBundleService,
                          DraftResourceService<ServiceBundle> draftServiceService,
                          GenericResourceService genericResourceService) {
        super(serviceBundleService);
        this.serviceBundleService = serviceBundleService;
        this.draftServiceService = draftServiceService;
        this.genericResourceService = genericResourceService;
    }

    @PostMapping(path = "/bulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ServiceBundle> bundles, @Parameter(hidden = true) Authentication auth) {
        serviceBundleService.addBulk(bundles, auth);
    }

    @Override
    @PostMapping()
    public ResponseEntity<ServiceBundle> add(@RequestBody ServiceBundle serviceBundle,
                                             @Parameter(hidden = true) Authentication auth) {
        if (serviceBundle.isDraft()) {
            return new ResponseEntity<>(draftServiceService.save(serviceBundle), HttpStatus.CREATED);
        }
        return super.add(serviceBundle, auth);
    }

    @Browse
    @GetMapping()
    public ResponseEntity<Paging<ServiceBundle>> getAll(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams,
                                                        @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("services");
        return new ResponseEntity<>(genericResourceService.getResults(ff), HttpStatus.OK);
    }

}
