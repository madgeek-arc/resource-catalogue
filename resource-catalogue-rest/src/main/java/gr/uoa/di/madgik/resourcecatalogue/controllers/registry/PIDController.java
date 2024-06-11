package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//@Profile("beyond")
@RestController
@RequestMapping("pid")
@Tag(name = "pid", description = "Get information about a specific resource via its PID")
public class PIDController {

    private final PIDService pidService;
    private final ProviderService providerService;
    private final ServiceBundleService serviceService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;

    @Autowired
    SecurityService securityService;

    public PIDController(PIDService pidService, ProviderService providerService,
                         ServiceBundleService serviceService,
                         TrainingResourceService trainingResourceService,
                         InteroperabilityRecordService interoperabilityRecordService) {
        this.pidService = pidService;
        this.providerService = providerService;
        this.serviceService = serviceService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
    }

    @Operation(summary = "Returns the Resource with the given PID.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> get(@PathVariable("id") String pid, @RequestParam String resourceType) {
        Bundle<?> bundle = pidService.get(pid, resourceType);
        if (bundle != null) {
            return new ResponseEntity<>(bundle.getPayload(), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @Operation(summary = "Register a resource on the PID service")
    @PostMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> registerPid(@PathVariable("id") String resourceId, @RequestParam String resourceType) {
        Bundle<?> bundle = pidService.get(resourceId, resourceType);
        String resourceTypePath = pidService.determineResourceTypePath(resourceType);
        if (bundle != null) {
            pidService.updatePID(bundle.getId(), resourceTypePath);
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @Parameter(hidden = true)
    @PutMapping(path = "updateAllHandles", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void updateAllHandles() {
        List<ProviderBundle> allProviders = getAllApprovedProviders();
        for (ProviderBundle providerBundle : allProviders) {
            pidService.updatePID(providerBundle.getId(), "providers/");
        }
        List<ServiceBundle> allServices = getAllApprovedServices();
        for (ServiceBundle serviceBundle : allServices) {
            pidService.updatePID(serviceBundle.getId(), "services/");
        }
        List<TrainingResourceBundle> allTrainingResources = getAllApprovedTrainingResources();
        for (TrainingResourceBundle trainingResourceBundle : allTrainingResources) {
            pidService.updatePID(trainingResourceBundle.getId(), "trainings/");
        }
        List<InteroperabilityRecordBundle> allInteroperabilityRecords = getAllApprovedInteroperabilityRecords();
        for (InteroperabilityRecordBundle interoperabilityRecordBundle : allInteroperabilityRecords) {
            pidService.updatePID(interoperabilityRecordBundle.getId(), "guidelines/");
        }
    }

    private List<ProviderBundle> getAllApprovedProviders() {
        FacetFilter ff = createFacetFilter();
        ff.addFilter("status", "approved provider");
        return providerService.getAll(ff, securityService.getAdminAccess()).getResults();
    }

    private List<ServiceBundle> getAllApprovedServices() {
        FacetFilter ff = createFacetFilter();
        ff.addFilter("status", "approved resource");
        return serviceService.getAll(ff, securityService.getAdminAccess()).getResults();
    }

    private List<TrainingResourceBundle> getAllApprovedTrainingResources() {
        FacetFilter ff = createFacetFilter();
        ff.addFilter("status", "approved resource");
        return trainingResourceService.getAll(ff, securityService.getAdminAccess()).getResults();
    }

    private List<InteroperabilityRecordBundle> getAllApprovedInteroperabilityRecords() {
        FacetFilter ff = createFacetFilter();
        ff.addFilter("status", "approved interoperability record");
        return interoperabilityRecordService.getAll(ff, securityService.getAdminAccess()).getResults();
    }

    private FacetFilter createFacetFilter() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        return ff;
    }
}
