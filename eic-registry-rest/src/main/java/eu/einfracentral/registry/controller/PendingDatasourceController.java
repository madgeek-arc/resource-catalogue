package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ServiceException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping({"pendingDatasource"})
@Api(description = "Operations for Pending Datasources", tags = {"pending-datasource-controller"})
public class PendingDatasourceController extends ResourceController<DatasourceBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(PendingDatasourceController.class);
    private final PendingResourceService<DatasourceBundle> pendingDatasourceManager;
    private final ResourceBundleService<DatasourceBundle> resourceBundleService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final IdCreator idCreator;

    @Autowired
    PendingDatasourceController(PendingResourceService<DatasourceBundle> pendingDatasourceManager,
                             ResourceBundleService<DatasourceBundle> resourceBundleService,
                             ProviderService<ProviderBundle, Authentication> providerService,
                             IdCreator idCreator) {
        super(pendingDatasourceManager);
        this.pendingDatasourceManager = pendingDatasourceManager;
        this.resourceBundleService = resourceBundleService;
        this.providerService = providerService;
        this.idCreator = idCreator;
    }

    @DeleteMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<DatasourceBundle> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        DatasourceBundle datasourceBundle = pendingDatasourceManager.get(id);
        pendingDatasourceManager.delete(datasourceBundle);
        logger.info("User '{}' deleted Pending Datasource '{}' with id: '{}'", auth.getName(), datasourceBundle.getDatasource().getName(), datasourceBundle.getDatasource().getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(path = "/datasource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Datasource> getDatasource(@PathVariable String id) {
        return new ResponseEntity<>(pendingDatasourceManager.get(id).getDatasource(), HttpStatus.OK);
    }

    @GetMapping(path = "/rich/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RichResource> getPendingRich(@PathVariable("id") String id, Authentication auth) {
        return new ResponseEntity<>((RichResource) pendingDatasourceManager.getPendingRich(id, auth), HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "/byProvider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth,#id,true)")
    public ResponseEntity<Paging<DatasourceBundle>> getProviderPendingDatasources(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("resource_organisation", id);
        return new ResponseEntity<>(pendingDatasourceManager.getAll(ff, auth), HttpStatus.OK);
    }

    @PostMapping(path = "/addDatasource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Datasource> addDatasource(@RequestBody Datasource datasource, @ApiIgnore Authentication auth) {
        DatasourceBundle datasourceBundle = new DatasourceBundle(datasource);
        return new ResponseEntity<>(pendingDatasourceManager.add(datasourceBundle, auth).getDatasource(), HttpStatus.CREATED);
    }

    @PostMapping(path = "/updateDatasource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #datasource)")
    public ResponseEntity<Datasource> updateDatasource(@RequestBody Datasource datasource, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        DatasourceBundle datasourceBundle = pendingDatasourceManager.get(datasource.getId());
        datasourceBundle.setDatasource(datasource);
        return new ResponseEntity<>(pendingDatasourceManager.update(datasourceBundle, auth).getDatasource(), HttpStatus.OK);
    }

    @PostMapping("/transform/pending")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #datasourceId)")
    public void transformDatasourceToPending(@RequestParam String datasourceId, @ApiIgnore Authentication auth) {
        pendingDatasourceManager.transformToPending(datasourceId, auth);
    }

    @PostMapping("/transform/datasource")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #datasourceId)")
    public void transformDatasourceToInfra(@RequestParam String datasourceId, @ApiIgnore Authentication auth) {
        pendingDatasourceManager.transformToActive(datasourceId, auth);
    }

    @PutMapping(path = "/pending", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PostAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, returnObject.body)")
    public ResponseEntity<Datasource> temporarySavePending(@RequestBody Datasource datasource, @ApiIgnore Authentication auth) throws NoSuchAlgorithmException {
        // if Datasource has ID -> check if it exists in OpenAIRE Datasources list
        DatasourceBundle datasourceBundle = pendingDatasourceManager.getOpenAIREDatasource(datasource);
        DatasourceBundle toCreateId = new DatasourceBundle();
        toCreateId.setDatasource(datasource);
        datasource.setId(idCreator.createDatasourceId(toCreateId));

        try {
            datasourceBundle = pendingDatasourceManager.get(datasource.getId());
            datasourceBundle.setDatasource(datasource);
            datasourceBundle = pendingDatasourceManager.update(datasourceBundle, auth);
        } catch (ResourceException | ResourceNotFoundException e) {
            logger.debug("Pending Datasource with id '{}' does not exist. Creating it...", datasource.getId());
            datasourceBundle.setDatasource(datasource);
            datasourceBundle = pendingDatasourceManager.add(datasourceBundle, auth);
        }
        return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
    }

    @PutMapping(path = "/datasource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #datasource)")
    public ResponseEntity<Datasource> temporarySaveDatasource(@RequestBody Datasource datasource, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        pendingDatasourceManager.transformToPending(datasource.getId(), auth);
        DatasourceBundle datasourceBundle = pendingDatasourceManager.get(datasource.getId());
        datasourceBundle.setDatasource(datasource);
        return new ResponseEntity<>(pendingDatasourceManager.update(datasourceBundle, auth).getDatasource(), HttpStatus.OK);
    }

    @PutMapping(path = "/transform/datasource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #datasource)")
    public ResponseEntity<Datasource> pendingToInfra(@RequestBody Datasource datasource, @ApiIgnore Authentication auth) throws ResourceNotFoundException, NoSuchAlgorithmException {
        if (datasource == null) {
            throw new ServiceException("Cannot add a null Datasource");
        }
        DatasourceBundle datasourceBundle = null;

        try { // check if Datasource already exists
            DatasourceBundle toCreateId = new DatasourceBundle();
            toCreateId.setDatasource(datasource);
            datasource.setId(idCreator.createDatasourceId(toCreateId));
            datasourceBundle = this.pendingDatasourceManager.get(datasource.getId());
        } catch (ResourceException | eu.einfracentral.exception.ResourceNotFoundException e) {
            // continue with the creation of the service
        }

        // check Provider's template status -> block transform if it's on 'pending' state
        String resourceOrgranisation = datasource.getResourceOrganisation();
        ProviderBundle providerBundle = providerService.get(resourceOrgranisation);
        if (providerBundle.getTemplateStatus().equals("pending template")){
            throw new ValidationException(String.format("There is already a Resource waiting to be approved for the Provider [%s]", resourceOrgranisation));
        }

        if (datasourceBundle == null) { // if existing Pending Datasource is null, create a new Active Datasource
            datasourceBundle = resourceBundleService.addResource(new DatasourceBundle(datasource), auth);
            logger.info("User '{}' added Datasource:\n{}", auth.getName(), datasourceBundle);
        } else { // else update Pending Datasource and transform it to Active Datasource
            if (datasourceBundle.getDatasource().getVersion() != null && datasourceBundle.getDatasource().getVersion().equals("")){
                datasourceBundle.getDatasource().setVersion(null);
            }
            datasourceBundle.setDatasource(datasource); // important to keep other fields of DatasourceBundle
            datasourceBundle = pendingDatasourceManager.update(datasourceBundle, auth);
            logger.info("User '{}' updated Pending Datasource:\n{}", auth.getName(), datasourceBundle);

            // transform to active
            datasourceBundle = pendingDatasourceManager.transformToActive(datasourceBundle.getId(), auth);
        }

        return new ResponseEntity<>(datasourceBundle.getDatasource(), HttpStatus.OK);
    }
}
