package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstance;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceDto;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateInstanceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping("configurationTemplateInstance")
@Tag(name = "configuration template instance")
public class ConfigurationTemplateInstanceController {

    private static final Logger logger = LogManager.getLogger(ConfigurationTemplateInstanceController.class);
    private final ConfigurationTemplateInstanceService configurationTemplateInstanceService;

    public ConfigurationTemplateInstanceController(ConfigurationTemplateInstanceService configurationTemplateInstanceService) {
        this.configurationTemplateInstanceService = configurationTemplateInstanceService;
    }

    @Operation(summary = "Returns the ConfigurationTemplateInstance with the given id.")
    @GetMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<ConfigurationTemplateInstanceDto> getConfigurationTemplateInstance(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                                             @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstance configurationTemplateInstance = configurationTemplateInstanceService.get(id).getConfigurationTemplateInstance();
        ConfigurationTemplateInstanceDto ret = configurationTemplateInstanceService.createConfigurationTemplateInstanceDto(configurationTemplateInstance);
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @Operation(summary = "Filter a list of ConfigurationTemplateInstances based on a set of filters or get a list of all ConfigurationTemplateInstances in the Catalogue.")
    @Browse
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ConfigurationTemplateInstanceDto>> getAllConfigurationTemplateInstances(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams,
                                                                                                         @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", false);
        List<ConfigurationTemplateInstanceDto> configurationTemplateInstanceList = new LinkedList<>();
        Paging<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundlePaging = configurationTemplateInstanceService.getAll(ff, auth);
        for (ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle : configurationTemplateInstanceBundlePaging.getResults()) {
            configurationTemplateInstanceList.add(configurationTemplateInstanceService.createConfigurationTemplateInstanceDto(configurationTemplateInstanceBundle.getConfigurationTemplateInstance()));
        }
        Paging<ConfigurationTemplateInstanceDto> configurationTemplateInstancePaging = new Paging<>(configurationTemplateInstanceBundlePaging.getTotal(), configurationTemplateInstanceBundlePaging.getFrom(),
                configurationTemplateInstanceBundlePaging.getTo(), configurationTemplateInstanceList, configurationTemplateInstanceBundlePaging.getFacets());
        return new ResponseEntity<>(configurationTemplateInstancePaging, HttpStatus.OK);
    }

    @Operation(summary = "Returns a List of ConfigurationTemplateInstance associated with the given 'resourceId'")
    @GetMapping(path = "getAllByResourceId/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ConfigurationTemplateInstanceDto>> getConfigurationTemplateInstancesByResourceId(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                                                                @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        List<ConfigurationTemplateInstance> configurationTemplateInstances = configurationTemplateInstanceService.getConfigurationTemplateInstancesByResourceId(id);
        List<ConfigurationTemplateInstanceDto> ret = new ArrayList<>();
        for (ConfigurationTemplateInstance configurationTemplateInstance : configurationTemplateInstances) {
            ret.add(configurationTemplateInstanceService.createConfigurationTemplateInstanceDto(configurationTemplateInstance));
        }
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    //    @Operation(summary = "Returns a List of ConfigurationTemplateInstance associated with the given 'configurationTemplateId'")
    @GetMapping(path = "getAllByConfigurationTemplateId/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ConfigurationTemplateInstanceDto>> getConfigurationTemplateInstancesByConfigurationTemplateId(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                                                                             @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        List<ConfigurationTemplateInstance> configurationTemplateInstances = configurationTemplateInstanceService.getConfigurationTemplateInstancesByConfigurationTemplateId(id);
        List<ConfigurationTemplateInstanceDto> ret = new ArrayList<>();
        for (ConfigurationTemplateInstance configurationTemplateInstance : configurationTemplateInstances) {
            ret.add(configurationTemplateInstanceService.createConfigurationTemplateInstanceDto(configurationTemplateInstance));
        }
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    //    @Operation(summary = "Create a new ConfigurationTemplateInstance.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateInstance> addConfigurationTemplateInstance(@RequestBody ConfigurationTemplateInstance configurationTemplateInstance,
                                                                                          @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = configurationTemplateInstanceService.add(new ConfigurationTemplateInstanceBundle(configurationTemplateInstance), auth);
        logger.info("Added the Configuration Template Instance with id '{}'", configurationTemplateInstance.getId());
        return new ResponseEntity<>(configurationTemplateInstanceBundle.getConfigurationTemplateInstance(), HttpStatus.CREATED);
    }

    //    @Operation(summary = "Add a List of ConfigurationTemplateInstances.")
    @PostMapping(path = "addAll", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<ConfigurationTemplateInstance>> addConfigurationTemplateInstances(@RequestBody List<ConfigurationTemplateInstance> configurationTemplateInstances,
                                                                                                 @Parameter(hidden = true) Authentication auth) {
        for (ConfigurationTemplateInstance configurationTemplateInstance : configurationTemplateInstances) {
            configurationTemplateInstanceService.add(new ConfigurationTemplateInstanceBundle(configurationTemplateInstance), auth);
            logger.info("Added the Configuration Template Instance with id '{}'", configurationTemplateInstance.getId());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    //    @Operation(summary = "Updates the ConfigurationTemplateInstance with the given id.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateInstance> updateConfigurationTemplateInstance(@RequestBody ConfigurationTemplateInstance configurationTemplateInstance,
                                                                                             @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = configurationTemplateInstanceService.get(configurationTemplateInstance.getId());
        configurationTemplateInstanceBundle.setConfigurationTemplateInstance(configurationTemplateInstance);
        configurationTemplateInstanceBundle = configurationTemplateInstanceService.update(configurationTemplateInstanceBundle, auth);
        logger.info("Updated the Configuration Template Instance with id '{}'", configurationTemplateInstanceBundle.getId());
        return new ResponseEntity<>(configurationTemplateInstanceBundle.getConfigurationTemplateInstance(), HttpStatus.OK);
    }

    //    @DeleteMapping(path = "{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateInstance> deleteConfigurationTemplateInstance(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                                             @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                                             @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        String id = prefix + "/" + suffix;
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = configurationTemplateInstanceService.get(id);
        if (configurationTemplateInstanceBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Configuration Template Instance: {}", configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId());
        configurationTemplateInstanceService.delete(configurationTemplateInstanceBundle);
        logger.info("Deleted the Configuration Template Instance with id '{}'",
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId());
        return new ResponseEntity<>(configurationTemplateInstanceBundle.getConfigurationTemplateInstance(), HttpStatus.OK);
    }

    @GetMapping(path = "getBundle/{prefix}/{suffix}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateInstanceBundle> getBundle(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                         @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(configurationTemplateInstanceService.get(id), HttpStatus.OK);
    }

    //    @PutMapping(path = "updateBundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplateInstanceBundle> updateBundle(@RequestBody ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, @Parameter(hidden = true) Authentication authentication) throws ResourceNotFoundException {
        ResponseEntity<ConfigurationTemplateInstanceBundle> ret = new ResponseEntity<>(configurationTemplateInstanceService.update(configurationTemplateInstanceBundle, authentication), HttpStatus.OK);
        logger.info("Updated ConfigurationTemplateInstanceBundle with id: {}", configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId());
        return ret;
    }
}
