package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.domain.User;
import eu.einfracentral.registry.service.ResourceService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ResourceCRUDService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("publicProvider")
@Api(value = "Get information about a published Provider")
public class PublicProviderController {

    private static final Logger logger = LogManager.getLogger(PublicProviderController.class);
    private final ResourceService<ProviderBundle, Authentication> publicProviderManager;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final SecurityService securityService;

    @Autowired
    PublicProviderController(@Qualifier("publicProviderManager") ResourceService<ProviderBundle, Authentication> publicProviderManager,
                             ProviderService<ProviderBundle, Authentication> providerService, SecurityService securityService) {
        this.publicProviderManager = publicProviderManager;
        this.providerService = providerService;
        this.securityService = securityService;
    }

    @ApiOperation(value = "Returns the published Provider with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Provider> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        Provider provider = providerService.get(id).getProvider();
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

//    @ApiOperation(value = "Returns all published Providers.")
//    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
//    public Browsing<ProviderBundle> getAll(FacetFilter ff, Authentication auth) {
//        List<ProviderBundle> retList = new ArrayList<>();
//        ff.addFilter("published", true);
//        Browsing<ProviderBundle> providers = super.getAll(ff, auth);
//        retList.addAll(providers.getResults());
//        providers.setResults(retList);
//
//        return providers;
//    }

    @GetMapping(path = "getMyPublishedProviders", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ProviderBundle>> getMyPublishedProviders(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.setOrderBy(FacetFilterUtils.createOrderBy("name", "asc"));
        return new ResponseEntity<>(publicProviderManager.getMy(ff, auth).getResults(), HttpStatus.OK);
    }
}