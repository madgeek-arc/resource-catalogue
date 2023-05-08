package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplate;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateBundle;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("configurationTemplate")
@Api(value = "Operations for Configuration Templates")
public class ConfigurationTemplateInstanceController {

    private static final Logger logger = LogManager.getLogger(ConfigurationTemplateInstanceController.class);
    private final ConfigurationTemplateInstanceService<ConfigurationTemplateBundle> configurationTemplateInstanceService;

    public ConfigurationTemplateInstanceController(ConfigurationTemplateInstanceService<ConfigurationTemplateBundle> configurationTemplateInstanceService) {
        this.configurationTemplateInstanceService = configurationTemplateInstanceService;
    }

    @ApiOperation(value = "Returns the ConfigurationTemplate with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<ConfigurationTemplate> getConfigurationTemplate(@PathVariable("id") String id) {
        ConfigurationTemplate configurationTemplate = configurationTemplateInstanceService.get(id).getConfigurationTemplate();
        return new ResponseEntity<>(configurationTemplate, HttpStatus.OK);
    }

    @ApiOperation(value = "Filter a list of ConfigurationTemplates based on a set of filters or get a list of all ConfigurationTemplates in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ConfigurationTemplate>> getAllConfigurationTemplates(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                      @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", false);
        List<ConfigurationTemplate> configurationTemplateList = new LinkedList<>();
        Paging<ConfigurationTemplateBundle> configurationTemplateBundlePaging = configurationTemplateInstanceService.getAll(ff, auth);
        for (ConfigurationTemplateBundle configurationTemplateBundle : configurationTemplateBundlePaging.getResults()) {
            configurationTemplateList.add(configurationTemplateBundle.getConfigurationTemplate());
        }
        Paging<ConfigurationTemplate> configurationTemplatePaging = new Paging<>(configurationTemplateBundlePaging.getTotal(), configurationTemplateBundlePaging.getFrom(),
                configurationTemplateBundlePaging.getTo(), configurationTemplateList, configurationTemplateBundlePaging.getFacets());
        return new ResponseEntity<>(configurationTemplatePaging, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the ConfigurationTemplates of the given Interoperability Record.")
    @GetMapping(path = "/byInteroperabilityRecord/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<ConfigurationTemplate> getConfigurationTemplateByInteroperabilityRecord(@PathVariable("interoperabilityRecordId") String interoperabilityRecordId,
                                                                                                  @ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        List<ConfigurationTemplateBundle> allConfigurationTemplateBundles = configurationTemplateInstanceService.getAll(ff, auth).getResults();
        for (ConfigurationTemplateBundle configurationTemplateBundle : allConfigurationTemplateBundles){
            if (configurationTemplateBundle.getConfigurationTemplate().getInteroperabilityRecordId().equals(interoperabilityRecordId)){
                return new ResponseEntity<>(configurationTemplateBundle.getConfigurationTemplate(), HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @ApiOperation(value = "Create a new ConfigurationTemplate.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #configurationTemplate)")
    public ResponseEntity<ConfigurationTemplate> addConfigurationTemplate(@Valid @RequestBody ConfigurationTemplate configurationTemplate,
                                                                          @ApiIgnore Authentication auth) {
        ConfigurationTemplateBundle configurationTemplateBundle = configurationTemplateInstanceService.add(new ConfigurationTemplateBundle(configurationTemplate), auth);
        logger.info("User '{}' added the Configuration Template with id '{}'", auth.getName(), configurationTemplate.getId());
        return new ResponseEntity<>(configurationTemplateBundle.getConfigurationTemplate(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the ConfigurationTemplate with the given id.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #configurationTemplate)")
    public ResponseEntity<ConfigurationTemplate> updateConfigurationTemplate(@Valid @RequestBody ConfigurationTemplate configurationTemplate,
                                                                             @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ConfigurationTemplateBundle configurationTemplateBundle = configurationTemplateInstanceService.get(configurationTemplate.getId());
        configurationTemplateBundle.setConfigurationTemplate(configurationTemplate);
        configurationTemplateBundle = configurationTemplateInstanceService.update(configurationTemplateBundle, auth);
        logger.info("User '{}' updated the Configuration Template with id '{}'", auth.getName(), configurationTemplateBundle.getId());
        return new ResponseEntity<>(configurationTemplateBundle.getConfigurationTemplate(), HttpStatus.OK);
    }

    @DeleteMapping(path = "{configurationTemplateId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #configurationTemplateId)")
    public ResponseEntity<ConfigurationTemplate> deleteConfigurationTemplate(@PathVariable("configurationTemplateId") String configurationTemplateId,
                                                                             @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ConfigurationTemplateBundle configurationTemplateBundle = configurationTemplateInstanceService.get(configurationTemplateId);
        if (configurationTemplateBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Configuration Template: {}", configurationTemplateBundle.getConfigurationTemplate().getId());
        configurationTemplateInstanceService.delete(configurationTemplateBundle);
        logger.info("User '{}' deleted the Configuration Template with id '{}'", auth.getName(),
                configurationTemplateBundle.getConfigurationTemplate().getId());
        return new ResponseEntity<>(configurationTemplateBundle.getConfigurationTemplate(), HttpStatus.OK);
    }
}
