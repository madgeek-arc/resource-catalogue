package eu.einfracentral.controllers.publicresources;

import com.google.gson.Gson;
import eu.einfracentral.annotations.Browse;
import eu.einfracentral.domain.User;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstanceBundle;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstanceDto;
import eu.einfracentral.registry.service.ConfigurationTemplateInstanceService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
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
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class PublicConfigurationTemplateInstanceController {

    private static final Logger logger = LogManager.getLogger(PublicConfigurationTemplateInstanceController.class);
    private static final Gson gson = new Gson();

    private final SecurityService securityService;
    private final ConfigurationTemplateInstanceService<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceService;


    PublicConfigurationTemplateInstanceController(SecurityService securityService,
                                                  ConfigurationTemplateInstanceService<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceService) {
        this.securityService = securityService;
        this.configurationTemplateInstanceService = configurationTemplateInstanceService;
    }

    @ApiOperation(value = "Returns the Public Configuration Template Instance with the given id.")
    @GetMapping(path = "public/configurationTemplateInstance/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> getPublicConfigurationTemplateInstance(@PathVariable("id") String id) {
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = configurationTemplateInstanceService.get(id);
        if (configurationTemplateInstanceBundle.getMetadata().isPublished()) {
            ConfigurationTemplateInstanceDto ret = configurationTemplateInstanceService.createConfigurationTemplateInstanceDto(configurationTemplateInstanceBundle.getConfigurationTemplateInstance());
            return new ResponseEntity<>(ret, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Configuration Template Instance."));
    }

    @GetMapping(path = "public/configurationTemplateInstance/bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<?> getPublicConfigurationTemplateInstanceBundle(@PathVariable("id") String id,
                                                                          @ApiIgnore Authentication auth) {
        ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle = configurationTemplateInstanceService.get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT")) {
                if (configurationTemplateInstanceBundle.getMetadata().isPublished()) {
                    return new ResponseEntity<>(configurationTemplateInstanceBundle, HttpStatus.OK);
                } else {
                    return ResponseEntity.status(HttpStatus.FOUND).body(gson.toJson("The specific Configuration Template Instance Bundle does not consist a Public entity"));
                }
            }
        }
        if (configurationTemplateInstanceBundle.getMetadata().isPublished()) {
            return new ResponseEntity<>(configurationTemplateInstanceBundle, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson("You cannot view the specific Configuration Template Instance."));
    }

    @ApiOperation(value = "Filter a list of Public Configuration Template Instances based on a set of filters or get a list of all Public Configuration Template Instances in the Catalogue.")
    @Browse
    @GetMapping(path = "public/configurationTemplateInstance/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ConfigurationTemplateInstanceDto>> getAllPublicConfigurationTemplateInstances(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                                               @ApiIgnore Authentication auth) {

        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        List<ConfigurationTemplateInstanceDto> configurationTemplateInstanceList = new LinkedList<>();
        Paging<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundlePaging = configurationTemplateInstanceService.getAll(ff, auth);
        for (ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle : configurationTemplateInstanceBundlePaging.getResults()) {
            configurationTemplateInstanceList.add(configurationTemplateInstanceService.createConfigurationTemplateInstanceDto(configurationTemplateInstanceBundle.getConfigurationTemplateInstance()));
        }
        Paging<ConfigurationTemplateInstanceDto> configurationTemplateInstancePaging = new Paging<>(configurationTemplateInstanceBundlePaging.getTotal(), configurationTemplateInstanceBundlePaging.getFrom(),
                configurationTemplateInstanceBundlePaging.getTo(), configurationTemplateInstanceList, configurationTemplateInstanceBundlePaging.getFacets());
        return new ResponseEntity<>(configurationTemplateInstancePaging, HttpStatus.OK);
    }

    @Browse
    @GetMapping(path = "public/configurationTemplateInstance/bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ConfigurationTemplateInstanceBundle>> getAllPublicConfigurationTemplateInstanceBundles(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                                                                        @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", true);
        Paging<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundlePaging = configurationTemplateInstanceService.getAll(ff, auth);
        List<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundleList = new LinkedList<>(configurationTemplateInstanceBundlePaging.getResults());
        Paging<ConfigurationTemplateInstanceBundle> configurationTemplateInstancePaging = new Paging<>(configurationTemplateInstanceBundlePaging.getTotal(), configurationTemplateInstanceBundlePaging.getFrom(),
                configurationTemplateInstanceBundlePaging.getTo(), configurationTemplateInstanceBundleList, configurationTemplateInstanceBundlePaging.getFacets());
        return new ResponseEntity<>(configurationTemplateInstancePaging, HttpStatus.OK);
    }

}
