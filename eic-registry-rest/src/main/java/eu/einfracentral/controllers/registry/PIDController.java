package eu.einfracentral.controllers.registry;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.*;
import eu.openminted.registry.core.domain.FacetFilter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;
import java.util.List;

@RestController
@RequestMapping("pid")
@Api(value = "Get information about a specific Resource via its PID")
public class PIDController {

    private final PIDService pidService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final ServiceBundleService<ServiceBundle> serviceService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService;
    private final DatasourceService datasourceService;
    private final HelpdeskService<HelpdeskBundle, Authentication> helpdeskService;
    private final MonitoringService<MonitoringBundle, Authentication> monitoringService;

    public PIDController(PIDService pidService, ProviderService<ProviderBundle, Authentication> providerService,
                         ServiceBundleService<ServiceBundle> serviceService,
                         TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                         InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService,
                         DatasourceService datasourceService,
                         HelpdeskService<HelpdeskBundle, Authentication> helpdeskService,
                         MonitoringService<MonitoringBundle, Authentication> monitoringService) {
        this.pidService = pidService;
        this.providerService = providerService;
        this.serviceService = serviceService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.datasourceService = datasourceService;
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
    }

    @ApiOperation(value = "Returns the Resource with the given PID.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> get(@RequestParam String resourceType, @PathVariable("id") String pid) {
        Bundle<?> bundle = pidService.get(resourceType, pid);
        if (bundle != null) {
            return new ResponseEntity<>(bundle.getPayload(), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @ApiIgnore
    @PutMapping(path = "updateAllHandles", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void updateAllHandles(@ApiIgnore Authentication auth) {
        FacetFilter ff = createFacetFilter();
        List<ProviderBundle> allPublishedProviders = providerService.getAll(ff, auth).getResults();
        for (ProviderBundle providerBundle : allPublishedProviders) {
            List<AlternativeIdentifier> alternativeIdentifiers = providerBundle.getIdentifiers().getAlternativeIdentifiers();
            for (AlternativeIdentifier alternativeIdentifier : alternativeIdentifiers) {
                if (alternativeIdentifier.getType().equalsIgnoreCase("PID")) {
                    pidService.updatePID(alternativeIdentifier.getValue(), providerBundle.getId(), "providers/");
                    break;
                }
            }
        }
        List<ServiceBundle> allPublishedServices = serviceService.getAll(ff, auth).getResults();
        for (ServiceBundle serviceBundle : allPublishedServices) {
            List<AlternativeIdentifier> alternativeIdentifiers = serviceBundle.getIdentifiers().getAlternativeIdentifiers();
            for (AlternativeIdentifier alternativeIdentifier : alternativeIdentifiers) {
                if (alternativeIdentifier.getType().equalsIgnoreCase("PID")) {
                    pidService.updatePID(alternativeIdentifier.getValue(), serviceBundle.getId(), "services/");
                    break;
                }
            }
        }
        List<DatasourceBundle> allPublishedDatasources = datasourceService.getAll(ff, auth).getResults();
        for (DatasourceBundle datasourceBundle : allPublishedDatasources) {
            List<AlternativeIdentifier> alternativeIdentifiers = datasourceBundle.getIdentifiers().getAlternativeIdentifiers();
            for (AlternativeIdentifier alternativeIdentifier : alternativeIdentifiers) {
                if (alternativeIdentifier.getType().equalsIgnoreCase("PID")) {
                    pidService.updatePID(alternativeIdentifier.getValue(), datasourceBundle.getId(), "services/");
                    break;
                }
            }
        }
        List<TrainingResourceBundle> allPublishedTrainingResources = trainingResourceService.getAll(ff, auth).getResults();
        for (TrainingResourceBundle trainingResourceBundle : allPublishedTrainingResources) {
            List<AlternativeIdentifier> alternativeIdentifiers = trainingResourceBundle.getIdentifiers().getAlternativeIdentifiers();
            for (AlternativeIdentifier alternativeIdentifier : alternativeIdentifiers) {
                if (alternativeIdentifier.getType().equalsIgnoreCase("PID")) {
                    pidService.updatePID(alternativeIdentifier.getValue(), trainingResourceBundle.getId(), "trainings/");
                    break;
                }
            }
        }
        List<InteroperabilityRecordBundle> allInteroperabilityRecords = interoperabilityRecordService.getAll(ff, auth).getResults();
        for (InteroperabilityRecordBundle interoperabilityRecordBundle : allInteroperabilityRecords) {
            List<AlternativeIdentifier> alternativeIdentifiers = interoperabilityRecordBundle.getIdentifiers().getAlternativeIdentifiers();
            for (AlternativeIdentifier alternativeIdentifier : alternativeIdentifiers) {
                if (alternativeIdentifier.getType().equalsIgnoreCase("PID")) {
                    pidService.updatePID(alternativeIdentifier.getValue(), interoperabilityRecordBundle.getId(), "guidelines/");
                    break;
                }
            }
        }
//        List<HelpdeskBundle> allPublishedHelpdesks = helpdeskService.getAll(ff, auth).getResults();
//        List<MonitoringBundle> allPublishedMonitorings = monitoringService.getAll(ff, auth).getResults();
    }

    private FacetFilter createFacetFilter() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        return ff;
    }
}
