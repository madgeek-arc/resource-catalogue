package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.ConfigurationTemplateInstance;
import eu.einfracentral.domain.ConfigurationTemplateInstanceBundle;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateBundle;
import eu.einfracentral.registry.service.ConfigurationTemplateInstanceService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
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

    @ApiOperation(value = "Create a new ConfigurationTemplateInstance.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #configurationTemplateInstance)")
    public ResponseEntity<ConfigurationTemplateInstance> addConfigurationTemplateInstance(@Valid @RequestBody ConfigurationTemplateInstance configurationTemplateInstance,
                                                                          @ApiIgnore Authentication auth) {
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = configurationTemplateInstanceService.add(new ConfigurationTemplateInstanceBundle(configurationTemplateInstance), auth);
        logger.info("User '{}' added the Configuration Template Instance with id '{}'", auth.getName(), configurationTemplateInstance.getId());
        return new ResponseEntity<>(configurationTemplateInstanceBundle.getConfigurationTemplateInstance(), HttpStatus.CREATED);
    }

//    @ApiOperation(value = "Updates the ConfigurationTemplateInstance with the given id.")
//    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #configurationTemplate)")
//    public ResponseEntity<ConfigurationTemplateInstance> updateConfigurationTemplate(@Valid @RequestBody ConfigurationTemplateInstance configurationTemplate,
//                                                                             @ApiIgnore Authentication auth) throws ResourceNotFoundException {
//        ConfigurationTemplateBundle configurationTemplateBundle = configurationTemplateInstanceService.get(configurationTemplate.getId());
//        configurationTemplateBundle.setConfigurationTemplate(configurationTemplate);
//        configurationTemplateBundle = configurationTemplateInstanceService.update(configurationTemplateBundle, auth);
//        logger.info("User '{}' updated the Configuration Template with id '{}'", auth.getName(), configurationTemplateBundle.getId());
//        return new ResponseEntity<>(configurationTemplateBundle.getConfigurationTemplate(), HttpStatus.OK);
//    }
//
//    @DeleteMapping(path = "{configurationTemplateId}", produces = {MediaType.APPLICATION_JSON_VALUE})
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #configurationTemplateId)")
//    public ResponseEntity<ConfigurationTemplateInstance> deleteConfigurationTemplate(@PathVariable("configurationTemplateId") String configurationTemplateId,
//                                                                             @ApiIgnore Authentication auth) throws ResourceNotFoundException {
//        ConfigurationTemplateBundle configurationTemplateBundle = configurationTemplateInstanceService.get(configurationTemplateId);
//        if (configurationTemplateBundle == null) {
//            return new ResponseEntity<>(HttpStatus.GONE);
//        }
//        logger.info("Deleting Configuration Template: {}", configurationTemplateBundle.getConfigurationTemplate().getId());
//        configurationTemplateInstanceService.delete(configurationTemplateBundle);
//        logger.info("User '{}' deleted the Configuration Template with id '{}'", auth.getName(),
//                configurationTemplateBundle.getConfigurationTemplate().getId());
//        return new ResponseEntity<>(configurationTemplateBundle.getConfigurationTemplate(), HttpStatus.OK);
//    }
}
