package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.DatasourceBundle;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.domain.TrainingResourceBundle;
import eu.einfracentral.dto.ServicePaging;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.TrainingResourceService;
import eu.einfracentral.service.GenericResourceService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import springfox.documentation.annotations.ApiIgnore;

import javax.sql.DataSource;

@Controller
@RequestMapping("servicePaging")
public class ServicePagingController {

    private final ResourceBundleService<ServiceBundle> resourceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final DataSource commonDataSource;
    private final GenericResourceService genericResourceService;

    @Value("${auditing.interval:6}")
    private String auditingInterval;

    @Value("${project.catalogue.name}")
    private String catalogueName;


    @Autowired
    ServicePagingController(ResourceBundleService<ServiceBundle> service,
                      ProviderService<ProviderBundle, Authentication> provider,
                      ResourceBundleService<DatasourceBundle> datasourceBundleService,
                      TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                      DataSource commonDataSource, GenericResourceService genericResourceService) {
        this.resourceBundleService = service;
        this.providerService = provider;
        this.datasourceBundleService = datasourceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.commonDataSource = commonDataSource;
        this.genericResourceService = genericResourceService;
    }

    @ApiOperation(value = "Filter a list of Resources based on a set of filters or get a list of all Resources in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public String getAllServices(@RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueId,
                                            @RequestParam(defaultValue = "service", name = "type") String type,
                                            @ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                            @ApiIgnore Authentication authentication, Model model) {
        FacetFilter ff =  resourceBundleService.createFacetFilterForFetchingServicesAndDatasources(allRequestParams, catalogueId, type);
        resourceBundleService.updateFacetFilterConsideringTheAuthorization(ff, authentication);
        Paging<?> paging = genericResourceService.getResults(ff).map(r -> ((eu.einfracentral.domain.ResourceBundle<?>) r).getPayload());

        ServicePaging<?> servicePaging = new ServicePaging<>(paging);
//        return ResponseEntity.ok();

        model.addAttribute("servicePaging", servicePaging);
        return "xmlView";
    }

}
