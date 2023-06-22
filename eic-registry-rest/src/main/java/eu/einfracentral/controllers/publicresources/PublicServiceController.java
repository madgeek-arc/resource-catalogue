package eu.einfracentral.controllers.publicresources;

import com.google.gson.Gson;
import eu.einfracentral.annotations.Browse;
import eu.einfracentral.domain.DatasourceBundle;
import eu.einfracentral.domain.ResourceBundle;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.service.GenericResourceService;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class PublicServiceController {

    private static final Logger logger = LogManager.getLogger(PublicServiceController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final GenericResourceService genericResourceService;
    private final ResourceBundleService<ServiceBundle> resourceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;

    public PublicServiceController(SecurityService securityService,
                                   GenericResourceService genericResourceService,
                                   ResourceBundleService<ServiceBundle> resourceBundleService,
                                   ResourceBundleService<DatasourceBundle> datasourceBundleService) {
        this.securityService = securityService;
        this.genericResourceService = genericResourceService;
        this.resourceBundleService = resourceBundleService;
        this.datasourceBundleService = datasourceBundleService;
    }

    @ApiOperation(value = "Returns the Public Resource with the given id.")
    @GetMapping(path = "public/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.resourceOrDatasourceIsActive(#id, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<?> getPublicService(@PathVariable("id") String id,
                                              @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                              @ApiIgnore Authentication auth) {
        try {
            return resourceBundleService.get(id, catalogueId).getMetadata().isPublished() ? new ResponseEntity(resourceBundleService.get(id, catalogueId).getService(), HttpStatus.OK) : new ResponseEntity(gson.toJson("The specific Service does not consist a Public entity"), HttpStatus.NOT_FOUND);
        } catch (eu.einfracentral.exception.ResourceNotFoundException e) {
            return datasourceBundleService.get(id, catalogueId).getMetadata().isPublished() ? new ResponseEntity(datasourceBundleService.get(id, catalogueId).getDatasource(), HttpStatus.OK) : new ResponseEntity(gson.toJson("The specific Service does not consist a Public entity"), HttpStatus.NOT_FOUND);
        }
    }

    //    @ApiOperation(value = "Returns the Public ServiceBundle with the given id.")
    @GetMapping(path = "public/service/infraService/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id, #catalogueId)")
    public ResponseEntity<?> getPublicServiceBundle(@PathVariable("id") String id,
                                                    @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                    @ApiIgnore Authentication auth) {
        try {
            return resourceBundleService.get(id, catalogueId).getMetadata().isPublished() ? new ResponseEntity(resourceBundleService.get(id, catalogueId), HttpStatus.OK) : new ResponseEntity(gson.toJson("The specific Service does not consist a Public entity"), HttpStatus.NOT_FOUND);
        } catch (eu.einfracentral.exception.ResourceNotFoundException e) {
            return datasourceBundleService.get(id, catalogueId).getMetadata().isPublished() ? new ResponseEntity(datasourceBundleService.get(id, catalogueId), HttpStatus.OK) : new ResponseEntity(gson.toJson("The specific Service does not consist a Public entity"), HttpStatus.NOT_FOUND);
        }
    }

    @ApiOperation(value = "Filter a list of Public Resources based on a set of filters or get a list of all Public Resources in the Catalogue.")
    @Browse
    @GetMapping(path = "public/service/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<?>> getAllPublicServices(@RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                          @RequestParam(defaultValue = "service", name = "type") String type,
                                                          @ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                          @ApiIgnore Authentication authentication) {
        FacetFilter ff = resourceBundleService.createFacetFilterForFetchingServicesAndDatasources(allRequestParams, catalogueId, type);
        ff.getFilter().put("published", true);
        resourceBundleService.updateFacetFilterConsideringTheAuthorization(ff, authentication);
        Paging<?> paging = genericResourceService.getResults(ff).map(r -> ((eu.einfracentral.domain.ResourceBundle<?>) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @Browse
    @GetMapping(path = "public/service/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<?>> getAllPublicServiceBundles(@RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
                                                                @RequestParam(defaultValue = "service", name = "type") String type,
                                                                @ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                @ApiIgnore Authentication authentication) {
        FacetFilter ff = resourceBundleService.createFacetFilterForFetchingServicesAndDatasources(allRequestParams, catalogueId, type);
        ff.getFilter().put("published", true);
        resourceBundleService.updateFacetFilterConsideringTheAuthorization(ff, authentication);
        Paging<?> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @GetMapping(path = "public/service/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<Object>> getMyPublicServices(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", true);
        ff.setResourceType("resources");
        ff.addOrderBy("name", "asc");
        if (auth == null) {
            throw new UnauthorizedUserException("Please log in.");
        }
        List<Object> resourceBundleList = new ArrayList<>();
        Paging<?> paging = genericResourceService.getResults(ff);
        for (Object o : paging.getResults()) {
            if (o instanceof ResourceBundle<?>) {
                if (securityService.isResourceProviderAdmin(auth, ((ResourceBundle<?>) o).getId(), ((ResourceBundle<?>) o).getPayload().getCatalogueId()) && ((ResourceBundle<?>) o).getMetadata().isPublished()) {
                    resourceBundleList.add(o);
                }
            }
        }
        Browsing<Object> browsing = new Browsing<>(paging.getTotal(), paging.getFrom(), paging.getTo(), resourceBundleList, paging.getFacets());
        return new ResponseEntity<>(browsing.getResults(), HttpStatus.OK);
    }
}
