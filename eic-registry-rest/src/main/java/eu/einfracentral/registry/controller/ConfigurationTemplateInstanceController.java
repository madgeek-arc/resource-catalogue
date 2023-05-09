package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstance;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstanceBundle;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstanceDto;
import eu.einfracentral.registry.service.ConfigurationTemplateInstanceService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("configurationTemplateInstance")
@Api(value = "Operations for Configuration Template Instances")
public class ConfigurationTemplateInstanceController {

    private static final Logger logger = LogManager.getLogger(ConfigurationTemplateInstanceController.class);
    private final ConfigurationTemplateInstanceService<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceService;

    public ConfigurationTemplateInstanceController(ConfigurationTemplateInstanceService<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceService) {
        this.configurationTemplateInstanceService = configurationTemplateInstanceService;
    }

    @ApiOperation(value = "Returns the ConfigurationTemplateInstance with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<ConfigurationTemplateInstance> getConfigurationTemplateInstance(@PathVariable("id") String id) {
        ConfigurationTemplateInstance configurationTemplateInstance = configurationTemplateInstanceService.get(id).getConfigurationTemplateInstance();
//        ConfigurationTemplateInstanceDto ret = configurationTemplateInstanceService.createConfigurationTemplateInstanceDto(configurationTemplateInstance);
        return new ResponseEntity<>(configurationTemplateInstance, HttpStatus.OK);
    }

    @ApiOperation(value = "Filter a list of ConfigurationTemplateInstances based on a set of filters or get a list of all ConfigurationTemplateInstances in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ConfigurationTemplateInstance>> getAllConfigurationTemplateInstances(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                      @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", false);
        List<ConfigurationTemplateInstance> configurationTemplateInstanceList = new LinkedList<>();
        Paging<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundlePaging = configurationTemplateInstanceService.getAll(ff, auth);
        for (ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle : configurationTemplateInstanceBundlePaging.getResults()) {
            configurationTemplateInstanceList.add(configurationTemplateInstanceBundle.getConfigurationTemplateInstance());
        }
        Paging<ConfigurationTemplateInstance> configurationTemplateInstancePaging = new Paging<>(configurationTemplateInstanceBundlePaging.getTotal(), configurationTemplateInstanceBundlePaging.getFrom(),
                configurationTemplateInstanceBundlePaging.getTo(), configurationTemplateInstanceList, configurationTemplateInstanceBundlePaging.getFacets());
        return new ResponseEntity<>(configurationTemplateInstancePaging, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a List of ConfigurationTemplateInstance associated with the given 'resourceId'")
    @GetMapping(path = "getAllByResourceId/{resourceId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ConfigurationTemplateInstance>> getConfigurationTemplateInstancesByResourceId(@PathVariable("resourceId") String resourceId) {
        List<ConfigurationTemplateInstance> configurationTemplateInstances = configurationTemplateInstanceService.getConfigurationTemplateInstancesByResourceId(resourceId);
//        List<ConfigurationTemplateInstanceDto> ret = new ArrayList<>();
//        for (ConfigurationTemplateInstance configurationTemplateInstance : configurationTemplateInstances){
//            ret.add(configurationTemplateInstanceService.createConfigurationTemplateInstanceDto(configurationTemplateInstance));
//        }
        return new ResponseEntity<>(configurationTemplateInstances, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a List of ConfigurationTemplateInstance associated with the given 'configurationTemplateId'")
    @GetMapping(path = "getAllByConfigurationTemplateId/{configurationTemplateId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ConfigurationTemplateInstance>> getConfigurationTemplateInstancesByConfigurationTemplateId(@PathVariable("configurationTemplateId") String configurationTemplateId) {
        List<ConfigurationTemplateInstance> configurationTemplateInstances = configurationTemplateInstanceService.getConfigurationTemplateInstancesByConfigurationTemplateId(configurationTemplateId);
//        List<ConfigurationTemplateInstanceDto> ret = new ArrayList<>();
//        for (ConfigurationTemplateInstance configurationTemplateInstance : configurationTemplateInstances){
//            ret.add(configurationTemplateInstanceService.createConfigurationTemplateInstanceDto(configurationTemplateInstance));
//        }
        return new ResponseEntity<>(configurationTemplateInstances, HttpStatus.OK);
    }

    @ApiOperation(value = "Create a new ConfigurationTemplateInstance.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #configurationTemplateInstance)")
    public ResponseEntity<ConfigurationTemplateInstance> addConfigurationTemplateInstance(@Valid @RequestBody ConfigurationTemplateInstance configurationTemplateInstance,
                                                                          @ApiIgnore Authentication auth) {
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = configurationTemplateInstanceService.add(new ConfigurationTemplateInstanceBundle(configurationTemplateInstance), auth);
        logger.info("User '{}' added the Configuration Template Instance with id '{}'", auth.getName(), configurationTemplateInstance.getId());
        return new ResponseEntity<>(configurationTemplateInstanceBundle.getConfigurationTemplateInstance(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the ConfigurationTemplateInstance with the given id.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #configurationTemplate)")
    public ResponseEntity<ConfigurationTemplateInstance> updateConfigurationTemplateInstance(@Valid @RequestBody ConfigurationTemplateInstance configurationTemplateInstance,
                                                                             @ApiIgnore Authentication auth) throws ResourceNotFoundException, ResourceNotFoundException {
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = configurationTemplateInstanceService.get(configurationTemplateInstance.getId());
        configurationTemplateInstanceBundle.setConfigurationTemplateInstance(configurationTemplateInstance);
        configurationTemplateInstanceBundle = configurationTemplateInstanceService.update(configurationTemplateInstanceBundle, auth);
        logger.info("User '{}' updated the Configuration Template Instance with id '{}'", auth.getName(), configurationTemplateInstanceBundle.getId());
        return new ResponseEntity<>(configurationTemplateInstanceBundle.getConfigurationTemplateInstance(), HttpStatus.OK);
    }

    @DeleteMapping(path = "{configurationTemplateInstanceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #configurationTemplateId)")
    public ResponseEntity<ConfigurationTemplateInstance> deleteConfigurationTemplateInstance(@PathVariable("configurationTemplateInstanceId") String configurationTemplateInstanceId,
                                                                             @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = configurationTemplateInstanceService.get(configurationTemplateInstanceId);
        if (configurationTemplateInstanceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Configuration Template Instance: {}", configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId());
        configurationTemplateInstanceService.delete(configurationTemplateInstanceBundle);
        logger.info("User '{}' deleted the Configuration Template Instance with id '{}'", auth.getName(),
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId());
        return new ResponseEntity<>(configurationTemplateInstanceBundle.getConfigurationTemplateInstance(), HttpStatus.OK);
    }
}
