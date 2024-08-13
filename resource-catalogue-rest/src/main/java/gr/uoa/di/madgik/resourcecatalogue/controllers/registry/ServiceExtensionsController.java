package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.MonitoringStatus;
import gr.uoa.di.madgik.resourcecatalogue.dto.ServiceType;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.HelpdeskService;
import gr.uoa.di.madgik.resourcecatalogue.service.MonitoringService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.utils.CreateArgoGrnetHttpRequest;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import gr.uoa.di.madgik.resourcecatalogue.validators.HelpdeskValidator;
import gr.uoa.di.madgik.resourcecatalogue.validators.MonitoringValidator;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping("service-extensions")
@Tag(name = "service extensions", description = "Operations about Services' Helpdesks/Monitorings")
public class ServiceExtensionsController {

    private static final Logger logger = LogManager.getLogger(ServiceExtensionsController.class);
    private final HelpdeskService helpdeskService;
    private final MonitoringService monitoringService;
    private final ServiceBundleService serviceBundleService;
    @Value("${argo.grnet.monitoring.availability}")
    private String monitoringAvailability;
    @Value("${argo.grnet.monitoring.status}")
    private String monitoringStatus;
    @Value("${argo.grnet.monitoring.token}")
    private String monitoringToken;
    private final GenericResourceService genericResourceService;

    @InitBinder("helpdesk")
    protected void initHelpdeskBinder(WebDataBinder binder) {
        binder.addValidators(new HelpdeskValidator());
    }

    @InitBinder("monitoring")
    protected void initMonitoringBinder(WebDataBinder binder) {
        binder.addValidators(new MonitoringValidator());
    }

    @Autowired
    ServiceExtensionsController(HelpdeskService helpdeskService,
                                MonitoringService monitoringService,
                                ServiceBundleService serviceBundleService,
                                GenericResourceService genericResourceService) {
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.serviceBundleService = serviceBundleService;
        this.genericResourceService = genericResourceService;
    }

    //SECTION: HELPDESK
    @Operation(summary = "Returns the Helpdesk with the given id.")
    @GetMapping(path = "/helpdesk/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Helpdesk> getHelpdesk(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        Helpdesk helpdesk = helpdeskService.get(id).getHelpdesk();
        return new ResponseEntity<>(helpdesk, HttpStatus.OK);
    }

    @GetMapping(path = "/helpdesk/bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<HelpdeskBundle> getHelpdeskBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                            @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        HelpdeskBundle helpdesk = helpdeskService.get(id);
        return new ResponseEntity<>(helpdesk, HttpStatus.OK);
    }

    @Operation(summary = "Returns the Helpdesk of the given Service of the given Catalogue.")
    @GetMapping(path = "/helpdesk/byService/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Helpdesk> getHelpdeskByServiceId(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                           @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                           @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                           @Parameter(hidden = true) Authentication auth) {
        String serviceId = prefix + "/" + suffix;
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<HelpdeskBundle> allHelpdesks = helpdeskService.getAll(ff, auth).getResults();
        for (HelpdeskBundle helpdesk : allHelpdesks) {
            if (helpdesk.getCatalogueId().equals(catalogueId) && (helpdesk.getHelpdesk().getServiceId().equals(serviceId)
                    || (catalogueId + '.' + helpdesk.getHelpdesk().getServiceId()).equals(serviceId))) {
                return new ResponseEntity<>(helpdesk.getHelpdesk(), HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @Operation(summary = "Filter a list of Helpdesks based on a set of filters or get a list of all Helpdesks in the Catalogue.")
    @Browse
    @BrowseCatalogue
    @GetMapping(path = "/helpdesk/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Helpdesk>> getAllHelpdesks(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("helpdesk");
        ff.addFilter("published", false);
        Paging<Helpdesk> paging = genericResourceService.getResults(ff).map(r -> ((HelpdeskBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @BrowseCatalogue
    @GetMapping(path = "/helpdesk/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<HelpdeskBundle>> getAllHelpdeskBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("helpdesk");
        ff.addFilter("published", false);
        Paging<HelpdeskBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Creates a new Helpdesk.")
    @PostMapping(path = "/helpdesk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #helpdesk.serviceId, #catalogueId)")
    public ResponseEntity<Helpdesk> addHelpdesk(@Valid @RequestBody Helpdesk helpdesk,
                                                @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                @RequestParam String resourceType,
                                                @Parameter(hidden = true) Authentication auth) {
        HelpdeskBundle helpdeskBundle = helpdeskService.add(new HelpdeskBundle(helpdesk, catalogueId), resourceType, auth);
        logger.info("Added the Helpdesk with id '{}'", helpdesk.getId());
        return new ResponseEntity<>(helpdeskBundle.getHelpdesk(), HttpStatus.CREATED);
    }

    @Operation(summary = "Updates the Helpdesk with the given id.")
    @PutMapping(path = "/helpdesk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Helpdesk> updateHelpdesk(@Valid @RequestBody Helpdesk helpdesk,
                                                   @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                   @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        HelpdeskBundle helpdeskBundle = helpdeskService.get(helpdesk.getId());
        helpdeskBundle.setHelpdesk(helpdesk);
        helpdeskBundle = helpdeskService.update(helpdeskBundle, auth);
        logger.info("Updated the Helpdesk with id '{}'", helpdesk.getId());
        return new ResponseEntity<>(helpdeskBundle.getHelpdesk(), HttpStatus.OK);
    }

    // Deletes the Helpdesk with the specific ID.
    @DeleteMapping(path = "/helpdesk/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Helpdesk> deleteHelpdeskById(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                       @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        String id = prefix + "/" + suffix;
        HelpdeskBundle helpdeskBundle = helpdeskService.get(id);
        if (helpdeskBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Helpdesk: {} of the Catalogue: {}", helpdeskBundle.getHelpdesk().getId(), helpdeskBundle.getCatalogueId());
        // delete Helpdesk
        helpdeskService.delete(helpdeskBundle);
        logger.info("Deleted the Helpdesk with id '{}' of the Catalogue '{}'", helpdeskBundle.getHelpdesk().getId(), helpdeskBundle.getCatalogueId());
        return new ResponseEntity<>(helpdeskBundle.getHelpdesk(), HttpStatus.OK);
    }

    // Deletes the Helpdesk of the specific Service of the specific Catalogue.
    @Operation(summary = "Deletes the Helpdesk of the specific Service of the specific Catalogue.")
    @DeleteMapping(path = "/helpdesk/{catalogueId}/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #prefix+'/'+#suffix, #catalogueId)")
    public ResponseEntity<Helpdesk> deleteHelpdesk(@PathVariable("catalogueId") String catalogueId,
                                                   @Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                   @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                   @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        Helpdesk helpdesk = getHelpdeskByServiceId(prefix, suffix, catalogueId, auth).getBody();
        assert helpdesk != null;
        HelpdeskBundle helpdeskBundle = helpdeskService.get(helpdesk.getId());
        if (helpdeskBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Helpdesk: {} of the Catalogue: {}", helpdeskBundle.getHelpdesk().getId(), helpdeskBundle.getCatalogueId());
        // delete Helpdesk
        helpdeskService.delete(helpdeskBundle);
        logger.info("Deleted the Helpdesk with id '{}' of the Catalogue '{}'", helpdeskBundle.getHelpdesk().getId(), helpdeskBundle.getCatalogueId());
        return new ResponseEntity<>(helpdeskBundle.getHelpdesk(), HttpStatus.OK);
    }

    // Create a Public HelpdeskBundle if something went bad during its creation
    @Hidden
    @PostMapping(path = "createPublicHelpdesk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<HelpdeskBundle> createPublicHelpdesk(@RequestBody HelpdeskBundle helpdeskBundle, @Parameter(hidden = true) Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Helpdesk from Helpdesk '{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail(), helpdeskBundle.getId(), helpdeskBundle.getCatalogueId());
        return ResponseEntity.ok(helpdeskService.createPublicResource(helpdeskBundle, auth));
    }

    @Hidden
    @PostMapping(path = "createPublicHelpdesks", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void createPublicHelpdesks(@Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        List<HelpdeskBundle> allHelpdesks = helpdeskService.getAll(ff, auth).getResults();
        for (HelpdeskBundle helpdeskBundle : allHelpdesks) {
            try {
                helpdeskService.createPublicResource(helpdeskBundle, auth);
            } catch (ResourceException e) {
                logger.info("Helpdesk with ID {} is already registered as Public", helpdeskBundle.getId());
            }
        }
    }


    //SECTION: MONITORING
    @Operation(summary = "Returns the Monitoring with the given id.")
    @GetMapping(path = "/monitoring/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Monitoring> getMonitoring(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                    @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        Monitoring monitoring = monitoringService.get(id).getMonitoring();
        return new ResponseEntity<>(monitoring, HttpStatus.OK);
    }

    @GetMapping(path = "/monitoring/bundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<MonitoringBundle> getMonitoringBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        MonitoringBundle monitoring = monitoringService.get(id);
        return new ResponseEntity<>(monitoring, HttpStatus.OK);
    }

    @Operation(summary = "Returns the Monitoring of the given Service of the given Catalogue.")
    @GetMapping(path = "/monitoring/byService/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Monitoring> getMonitoringByServiceId(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                               @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                               @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                               @Parameter(hidden = true) Authentication auth) {
        String serviceId = prefix + "/" + suffix;
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<MonitoringBundle> allMonitorings = monitoringService.getAll(ff, auth).getResults();
        for (MonitoringBundle monitoring : allMonitorings) {
            if (monitoring.getCatalogueId().equals(catalogueId) && (monitoring.getMonitoring().getServiceId().equals(serviceId)
                    || (catalogueId + '.' + monitoring.getMonitoring().getServiceId()).equals(serviceId))) {
                return new ResponseEntity<>(monitoring.getMonitoring(), HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @Operation(summary = "Filter a list of Monitorings based on a set of filters or get a list of all Monitorings in the Catalogue.")
    @Browse
    @GetMapping(path = "monitoring/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Monitoring>> getAllMonitorings(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("monitoring");
        ff.addFilter("published", false);
        Paging<Monitoring> paging = genericResourceService.getResults(ff).map(r -> ((MonitoringBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @BrowseCatalogue
    @GetMapping(path = "/monitoring/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<MonitoringBundle>> getAllMonitoringBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("monitoring");
        ff.addFilter("published", false);
        Paging<MonitoringBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Operation(summary = "Returns all the available Monitoring serviceTypes")
    @GetMapping(path = "/monitoring/serviceTypes", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ServiceType>> getAvailableServiceTypes() {
        return new ResponseEntity<>(monitoringService.getAvailableServiceTypes(), HttpStatus.OK);
    }

    @Operation(summary = "Creates a new Monitoring.")
    @PostMapping(path = "/monitoring", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #monitoring.serviceId, #catalogueId)")
    public ResponseEntity<Monitoring> addMonitoring(@Valid @RequestBody Monitoring monitoring,
                                                    @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                    @RequestParam String resourceType,
                                                    @Parameter(hidden = true) Authentication auth) {
        MonitoringBundle monitoringBundle = monitoringService.add(new MonitoringBundle(monitoring, catalogueId), resourceType, auth);
        logger.info("Added the Monitoring with id '{}'", monitoring.getId());
        return new ResponseEntity<>(monitoringBundle.getMonitoring(), HttpStatus.CREATED);
    }

    @Operation(summary = "Updates the Monitoring with the given id.")
    @PutMapping(path = "/monitoring", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Monitoring> updateMonitoring(@Valid @RequestBody Monitoring monitoring,
                                                       @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                       @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        MonitoringBundle monitoringBundle = monitoringService.get(monitoring.getId());
        monitoringBundle.setMonitoring(monitoring);
        monitoringBundle = monitoringService.update(monitoringBundle, auth);
        logger.info("Updated the Monitoring with id '{}'", monitoring.getId());
        return new ResponseEntity<>(monitoringBundle.getMonitoring(), HttpStatus.OK);
    }

    // Deletes the Helpdesk of the given Service ID of the given Catalogue.
    @DeleteMapping(path = "/monitoring/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Monitoring> deleteMonitoringById(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                           @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                           @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        String id = prefix + "/" + suffix;
        MonitoringBundle monitoringBundle = monitoringService.get(id);
        if (monitoringBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Monitoring: {} of the Catalogue: {}", monitoringBundle.getMonitoring().getId(), monitoringBundle.getCatalogueId());
        // delete Monitoring
        monitoringService.delete(monitoringBundle);
        logger.info("Deleted the Monitoring with id '{}' of the Catalogue '{}'", monitoringBundle.getMonitoring().getId(), monitoringBundle.getCatalogueId());
        return new ResponseEntity<>(monitoringBundle.getMonitoring(), HttpStatus.OK);
    }

    // Deletes the Monitoring of the specific Service of the specific Catalogue.
    @Operation(summary = "Deletes the Monitoring of the specific Service of the specific Catalogue.")
    @DeleteMapping(path = "/monitoring/{catalogueId}/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #prefix+'/'+#suffix, #catalogueId)")
    public ResponseEntity<Monitoring> deleteMonitoring(@PathVariable("catalogueId") String catalogueId,
                                                       @Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                       @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        Monitoring monitoring = getMonitoringByServiceId(prefix, suffix, catalogueId, auth).getBody();
        assert monitoring != null;
        MonitoringBundle monitoringBundle = monitoringService.get(monitoring.getId());
        if (monitoringBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Monitoring: {} of the Catalogue: {}", monitoringBundle.getMonitoring().getId(), monitoringBundle.getCatalogueId());
        // delete Monitoring
        monitoringService.delete(monitoringBundle);
        logger.info("Deleted the Monitoring with id '{}' of the Catalogue '{}'", monitoringBundle.getMonitoring().getId(), monitoringBundle.getCatalogueId());
        return new ResponseEntity<>(monitoringBundle.getMonitoring(), HttpStatus.OK);
    }


    // Argo GRNET Monitoring Status API calls
    @GetMapping(path = "/monitoring/monitoringAvailability/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<MonitoringStatus> getMonitoringAvailability(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                            @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                            @RequestParam String start_time,
                                                            @RequestParam String end_time) {
        String serviceId = prefix + "/" + suffix;
        //TODO: test if url works
        String url = monitoringAvailability + serviceId + "?start_time=" + start_time + "&end_time=" + end_time;
        String response = CreateArgoGrnetHttpRequest.createHttpRequest(url, monitoringToken);
        List<MonitoringStatus> serviceMonitoringStatuses;
        if (response != null) {
            JSONObject obj = new JSONObject(response);
            Gson gson = new Gson();
            JsonElement jsonObj = gson.fromJson(String.valueOf(obj), JsonElement.class);
            JsonArray results = jsonObj.getAsJsonObject().get("endpoints").getAsJsonArray().get(0).getAsJsonObject().get("results").getAsJsonArray();
            serviceMonitoringStatuses = monitoringService.createMonitoringAvailabilityObject(results);
            return serviceMonitoringStatuses;
        }
        return null;
    }

    @GetMapping(path = "/monitoring/monitoringStatus/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<MonitoringStatus> getMonitoringStatus(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                      @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                      @RequestParam(defaultValue = "false") Boolean allStatuses) {
        String serviceId = prefix + "/" + suffix;
        //TODO: test if url works
        String url = monitoringStatus + serviceId;
        if (allStatuses != null) {
            if (allStatuses) {
                url += "?view=details";
            }
        }
        String response = CreateArgoGrnetHttpRequest.createHttpRequest(url, monitoringToken);
        List<MonitoringStatus> serviceMonitoringStatuses;
        if (response != null) {
            JSONObject obj = new JSONObject(response);
            Gson gson = new Gson();
            JsonElement jsonObj = gson.fromJson(String.valueOf(obj), JsonElement.class);
            JsonArray statuses = jsonObj.getAsJsonObject().get("endpoints").getAsJsonArray().get(0).getAsJsonObject().get("statuses").getAsJsonArray();
            serviceMonitoringStatuses = monitoringService.createMonitoringStatusObject(statuses);
            return serviceMonitoringStatuses;
        }
        return null;
    }

    @GetMapping(path = "/monitoring/monitoringStatusOnSpecificPeriod/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<MonitoringStatus> getMonitoringStatusOnSpecificPeriod(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                      @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                      @RequestParam String from,
                                                                      @RequestParam String to) {
        String serviceId = prefix + "/" + suffix;
        OffsetDateTime odtFrom = OffsetDateTime.parse(from + "T00:00:01Z");
        OffsetDateTime odtTo = OffsetDateTime.parse(to + "T23:59:59Z");
        //TODO: test if url works
        String url = monitoringStatus + serviceId + "?start_time=" + odtFrom + "&end_time=" + odtTo;
        String response = CreateArgoGrnetHttpRequest.createHttpRequest(url, monitoringToken);
        List<MonitoringStatus> serviceMonitoringStatuses;
        if (response != null) {
            JSONObject obj = new JSONObject(response);
            Gson gson = new Gson();
            JsonElement jsonObj = gson.fromJson(String.valueOf(obj), JsonElement.class);
            JsonArray statuses = jsonObj.getAsJsonObject().get("endpoints").getAsJsonArray().get(0).getAsJsonObject().get("statuses").getAsJsonArray();
            serviceMonitoringStatuses = monitoringService.createMonitoringStatusObject(statuses);
            return serviceMonitoringStatuses;
        }
        return null;
    }

    @GetMapping(path = "/monitoring/monitoringStatus/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Map<String, String> getMonitoringStatusForAllServices() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        List<ServiceBundle> allServices = serviceBundleService.getAll(ff, null).getResults();
        Map<String, String> serviceStatusMap = new HashMap<>();
        for (ServiceBundle serviceBundle : allServices) {
            serviceStatusMap.put(serviceBundle.getId(), getServiceMonitoringStatusValue(serviceBundle.getId()));
        }
        return serviceStatusMap;
    }

    private String getServiceMonitoringStatusValue(String serviceId) {
        try {
            return getMonitoringStatus(serviceId.split("/")[0], serviceId.split("/")[1], false).get(0).getValue();
        } catch (NullPointerException e) {
            return "";
        }
    }

    // Create a Public MonitoringBundle if something went bad during its creation
    @Hidden
    @PostMapping(path = "createPublicMonitoring", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MonitoringBundle> createPublicMonitoring(@RequestBody MonitoringBundle monitoringBundle, @Parameter(hidden = true) Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Monitoring from Monitoring '{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail(), monitoringBundle.getId(), monitoringBundle.getCatalogueId());
        return ResponseEntity.ok(monitoringService.createPublicResource(monitoringBundle, auth));
    }

    @Hidden
    @PostMapping(path = "createPublicMonitorings", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void createPublicMonitorings(@Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        List<MonitoringBundle> allMonitorings = monitoringService.getAll(ff, auth).getResults();
        for (MonitoringBundle monitoringBundle : allMonitorings) {
            try {
                monitoringService.createPublicResource(monitoringBundle, auth);
            } catch (ResourceException e) {
                logger.info("Monitoring with ID {} is already registered as Public", monitoringBundle.getId());
            }
        }
    }

    @PostMapping(path = "/helpdesk/addBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulkHelpdesks(@RequestBody List<HelpdeskBundle> helpdeskList, @Parameter(hidden = true) Authentication auth) {
        helpdeskService.addBulk(helpdeskList, auth);
    }

    @PostMapping(path = "/monitoring/addBulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulkMonitorings(@RequestBody List<MonitoringBundle> monitoringList, @Parameter(hidden = true) Authentication auth) {
        monitoringService.addBulk(monitoringList, auth);
    }

}
