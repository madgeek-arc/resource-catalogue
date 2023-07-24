//package eu.einfracentral.controllers.publicresources;
//
//import com.google.gson.Gson;
//import eu.einfracentral.annotations.Browse;
//import eu.einfracentral.domain.DatasourceBundle;
//import eu.einfracentral.domain.ResourceBundle;
//import eu.einfracentral.domain.ServiceBundle;
//import eu.einfracentral.domain.TrainingResourceBundle;
//import eu.einfracentral.exception.ResourceNotFoundException;
//import eu.einfracentral.registry.service.ServiceBundleService;
//import eu.einfracentral.registry.service.TrainingResourceService;
//import eu.einfracentral.service.GenericResourceService;
//import eu.einfracentral.service.SecurityService;
//import eu.openminted.registry.core.domain.Browsing;
//import eu.openminted.registry.core.domain.FacetFilter;
//import eu.openminted.registry.core.domain.Paging;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiImplicitParam;
//import io.swagger.annotations.ApiImplicitParams;
//import io.swagger.annotations.ApiOperation;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
//import org.springframework.web.bind.annotation.*;
//import springfox.documentation.annotations.ApiIgnore;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping
//@Api(value = "Get information about a published Provider")
//public class PublicController {
//
//    private static final Logger logger = LogManager.getLogger(PublicController.class);
//    private static final Gson gson = new Gson();
//
//    private final ServiceBundleService<ServiceBundle> serviceBundleService;
//    private final TrainingResourceService<TrainingResourceBundle> trainingResourceBundleService;
//    private final SecurityService securityService;
//    private final GenericResourceService genericResourceService;
//
//    @Autowired
//    PublicController(SecurityService securityService,
//                     ServiceBundleService<ServiceBundle> serviceBundleService,
//                     TrainingResourceService<TrainingResourceBundle> trainingResourceBundleService,
//                     GenericResourceService genericResourceService) {
//        this.serviceBundleService = serviceBundleService;
//        this.trainingResourceBundleService = trainingResourceBundleService;
//        this.securityService = securityService;
//        this.genericResourceService = genericResourceService;
//    }
//
//    //SECTION: RESOURCE (TO BE DELETED)
//    @Deprecated
//    @ApiOperation(value = "Returns the Public Resource with the given id.")
//    @GetMapping(path = "public/resource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
//    @PreAuthorize("@securityService.resourceIsActive(#id, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
//    public ResponseEntity<?> getPublicResource(@PathVariable("id") String id,
//                                               @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
//                                               @ApiIgnore Authentication auth) {
//        try {
//            return serviceBundleService.get(id, catalogueId).getMetadata().isPublished() ? new ResponseEntity(serviceBundleService.get(id, catalogueId).getService(), HttpStatus.OK) : new ResponseEntity(gson.toJson("The specific Service does not consist a Public entity"), HttpStatus.NOT_FOUND);
//        } catch (eu.einfracentral.exception.ResourceNotFoundException e) {
//            return datasourceBundleService.get(id, catalogueId).getMetadata().isPublished() ? new ResponseEntity(datasourceBundleService.get(id, catalogueId).getDatasource(), HttpStatus.OK) : new ResponseEntity(gson.toJson("The specific Service does not consist a Public entity"), HttpStatus.NOT_FOUND);
//        }
//    }
//
//    //    @ApiOperation(value = "Returns the Public ServiceBundle with the given id.")
//    @Deprecated
//    @GetMapping(path = "public/resource/infraService/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id, #catalogueId)")
//    public ResponseEntity<?> getPublicResourceBundle(@PathVariable("id") String id,
//                                                     @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
//                                                     @ApiIgnore Authentication auth) {
//        try {
//            return serviceBundleService.get(id, catalogueId).getMetadata().isPublished() ? new ResponseEntity(serviceBundleService.get(id, catalogueId), HttpStatus.OK) : new ResponseEntity(gson.toJson("The specific Service does not consist a Public entity"), HttpStatus.NOT_FOUND);
//        } catch (eu.einfracentral.exception.ResourceNotFoundException e) {
//            return datasourceBundleService.get(id, catalogueId).getMetadata().isPublished() ? new ResponseEntity(datasourceBundleService.get(id, catalogueId), HttpStatus.OK) : new ResponseEntity(gson.toJson("The specific Service does not consist a Public entity"), HttpStatus.NOT_FOUND);
//        }
//    }
//
//    @Deprecated
//    @ApiOperation(value = "Filter a list of Public Resources based on a set of filters or get a list of all Public Resources in the Catalogue.")
//    @Browse
//    @ApiImplicitParam(name = "suspended", value = "Suspended", defaultValue = "false", dataType = "boolean", paramType = "query")
//    @GetMapping(path = "public/resource/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
//    public ResponseEntity<Paging<?>> getAllPublicResources(@RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
//                                                           @RequestParam(defaultValue = "service", name = "type") String type,
//                                                           @ApiIgnore @RequestParam Map<String, Object> allRequestParams,
//                                                           @ApiIgnore Authentication authentication) {
//        FacetFilter ff = serviceBundleService.createFacetFilterForFetchingServices(allRequestParams, catalogueId, type);
//        ff.getFilter().put("published", true);
//        serviceBundleService.updateFacetFilterConsideringTheAuthorization(ff, authentication);
//        Paging<?> paging = genericResourceService.getResults(ff).map(r -> ((eu.einfracentral.domain.ResourceBundle<?>) r).getPayload());
//        return ResponseEntity.ok(paging);
//    }
//
//    @Deprecated
//    @Browse
//    @ApiImplicitParam(name = "suspended", value = "Suspended", defaultValue = "false", dataType = "boolean", paramType = "query")
//    @GetMapping(path = "public/resource/adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
//    public ResponseEntity<Paging<?>> getAllPublicResourceBundles(@RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueId,
//                                                                 @RequestParam(defaultValue = "service", name = "type") String type,
//                                                                 @ApiIgnore @RequestParam Map<String, Object> allRequestParams,
//                                                                 @ApiIgnore Authentication authentication) {
//        FacetFilter ff = serviceBundleService.createFacetFilterForFetchingServices(allRequestParams, catalogueId, type);
//        ff.getFilter().put("published", true);
//        serviceBundleService.updateFacetFilterConsideringTheAuthorization(ff, authentication);
//        Paging<?> paging = genericResourceService.getResults(ff);
//        return ResponseEntity.ok(paging);
//    }
//
//    @Deprecated
//    @GetMapping(path = "public/resource/my", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
//    public ResponseEntity<List<Object>> getMyPublicResources(@ApiIgnore Authentication auth) {
//        FacetFilter ff = new FacetFilter();
//        ff.setQuantity(10000);
//        ff.addFilter("published", true);
//        ff.setResourceType("resources");
//        ff.addOrderBy("name", "asc");
//        if (auth == null) {
//            throw new UnauthorizedUserException("Please log in.");
//        }
//        List<Object> resourceBundleList = new ArrayList<>();
//        Paging<?> paging = genericResourceService.getResults(ff);
//        for (Object o : paging.getResults()) {
//            if (o instanceof ResourceBundle<?>) {
//                if (securityService.isResourceProviderAdmin(auth, ((ResourceBundle<?>) o).getId(), ((ResourceBundle<?>) o).getPayload().getCatalogueId()) && ((ResourceBundle<?>) o).getMetadata().isPublished()) {
//                    resourceBundleList.add(o);
//                }
//            }
//        }
//        Browsing<Object> browsing = new Browsing<>(paging.getTotal(), paging.getFrom(), paging.getTo(), resourceBundleList, paging.getFacets());
//        return new ResponseEntity<>(browsing.getResults(), HttpStatus.OK);
//    }
//
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "ids", value = "Comma-separated list of Resource ids", dataType = "string", paramType = "path")
//    })
//    @GetMapping(path = "public/resources/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
//    public ResponseEntity<List<?>> getSomeResources(@PathVariable("ids") String[] ids) {
//        String[] resourceTypeNames = new String[]{"service", "datasource", "training_resource"};
//        List<?> someResources = new ArrayList<>();
//        for (String id : ids) {
//            for (String resourceType : resourceTypeNames) {
//                try {
//                    someResources.add(genericResourceService.get(resourceType, id));
//                } catch (ResourceNotFoundException e) {
//                }
//            }
//        }
//        List<?> ret = someResources.stream().map(r -> ((eu.einfracentral.domain.Bundle<?>) r).getPayload()).collect(Collectors.toList());
//        return new ResponseEntity<>(ret, HttpStatus.OK);
//    }
//
//    //TODO: Remove after finalizing relations
////    @ApiOperation(value = "getInconsistentIdRelationsForServices")
//    @GetMapping(path = "public/getInconsistentIdRelationsForServices", produces = {MediaType.APPLICATION_JSON_VALUE})
//    public void getInconsistentIdRelationsForServices() {
//        FacetFilter ff = new FacetFilter();
//        ff.setQuantity(10000);
//        ff.addFilter("published", true);
//        List<ServiceBundle> allServices = serviceBundleService.getAll(ff, securityService.getAdminAccess()).getResults();
//        for (ServiceBundle serviceBundle : allServices) {
//            String serviceId = serviceBundle.getId();
//            String catalogueId = serviceBundle.getService().getCatalogueId();
//            String providerId = serviceBundle.getService().getResourceOrganisation();
//            List<String> resourceProviders = serviceBundle.getService().getResourceProviders();
//            List<String> relatedResources = serviceBundle.getService().getRelatedResources();
//            List<String> requiredResources = serviceBundle.getService().getRequiredResources();
//
//            logger.info(String.format("Service [%s] of the Provider [%s] of the Catalogue [%s] has the following related resources",
//                    serviceId, providerId, catalogueId));
//            if (!resourceProviders.isEmpty()) {
//                logger.info(String.format("Resource Providers [%s]", resourceProviders));
//            }
//            if (!relatedResources.isEmpty()) {
//                logger.info(String.format("Related Resources [%s]", relatedResources));
//            }
//            if (!requiredResources.isEmpty()) {
//                logger.info(String.format("Required Resources [%s]", requiredResources));
//            }
//        }
//    }
//
//    //    @ApiOperation(value = "getInconsistentIdRelationsForDatasources")
//    @GetMapping(path = "public/getInconsistentIdRelationsForDatasources", produces = {MediaType.APPLICATION_JSON_VALUE})
//    public void getInconsistentIdRelationsForDatasources() {
//        FacetFilter ff = new FacetFilter();
//        ff.setQuantity(10000);
//        ff.addFilter("published", true);
//        List<DatasourceBundle> allDatasources = datasourceBundleService.getAll(ff, securityService.getAdminAccess()).getResults();
//        for (DatasourceBundle datasourceBundle : allDatasources) {
//            String datasourceId = datasourceBundle.getId();
//            String catalogueId = datasourceBundle.getDatasource().getCatalogueId();
//            String providerId = datasourceBundle.getDatasource().getResourceOrganisation();
//            List<String> resourceProviders = datasourceBundle.getDatasource().getResourceProviders();
//            List<String> relatedResources = datasourceBundle.getDatasource().getRelatedResources();
//            List<String> requiredResources = datasourceBundle.getDatasource().getRequiredResources();
//
//            logger.info(String.format("Datasource [%s] of the Provider [%s] of the Catalogue [%s] has the following related resources",
//                    datasourceId, providerId, catalogueId));
//            if (!resourceProviders.isEmpty()) {
//                logger.info(String.format("Resource Providers [%s]", resourceProviders));
//            }
//            if (!relatedResources.isEmpty()) {
//                logger.info(String.format("Related Resources [%s]", relatedResources));
//            }
//            if (!requiredResources.isEmpty()) {
//                logger.info(String.format("Required Resources [%s]", requiredResources));
//            }
//        }
//    }
//
//    //    @ApiOperation(value = "getInconsistentIdRelationsForTrainingResources")
//    @GetMapping(path = "public/getInconsistentIdRelationsForTrainingResources", produces = {MediaType.APPLICATION_JSON_VALUE})
//    public void getInconsistentIdRelationsForTrainingResources() {
//        FacetFilter ff = new FacetFilter();
//        ff.setQuantity(10000);
//        ff.addFilter("published", true);
//        List<TrainingResourceBundle> allTrainingResources = trainingResourceBundleService.getAll(ff, securityService.getAdminAccess()).getResults();
//        for (TrainingResourceBundle trainingResourceBundle : allTrainingResources) {
//            String trainingResourceId = trainingResourceBundle.getId();
//            String catalogueId = trainingResourceBundle.getTrainingResource().getCatalogueId();
//            String providerId = trainingResourceBundle.getTrainingResource().getResourceOrganisation();
//            List<String> resourceProviders = trainingResourceBundle.getTrainingResource().getResourceProviders();
//            List<String> eoscRelatedServices = trainingResourceBundle.getTrainingResource().getEoscRelatedServices();
//
//            logger.info(String.format("Training Resource [%s] of the Provider [%s] of the Catalogue [%s] has the following related resources",
//                    trainingResourceId, providerId, catalogueId));
//            if (!resourceProviders.isEmpty()) {
//                logger.info(String.format("Resource Providers [%s]", resourceProviders));
//            }
//            if (!eoscRelatedServices.isEmpty()) {
//                logger.info(String.format("EOSC Related Services [%s]", eoscRelatedServices));
//            }
//        }
//    }
//}