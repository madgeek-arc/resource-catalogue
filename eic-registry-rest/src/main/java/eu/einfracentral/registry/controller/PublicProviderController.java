package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
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
    private final SecurityService securityService;

    @Autowired
    PublicProviderController(@Qualifier("publicProviderManager") ResourceService<ProviderBundle, Authentication> publicProviderManager,
                             SecurityService securityService) {
        this.publicProviderManager = publicProviderManager;
        this.securityService = securityService;
    }

    @ApiOperation(value = "Returns the published Provider with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Provider> get(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        Provider provider = publicProviderManager.get(id).getProvider();
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

//    @Cacheable(value = CACHE_PROVIDERS, key="#ff.hashCode()+(#auth!=null?#auth.hashCode():0)")
//    public Browsing<ProviderBundle> getAll(FacetFilter ff, Authentication auth) {
//        List<ProviderBundle> userProviders = null;
//        List<ProviderBundle> retList = new ArrayList<>();
//
//        // if user is ADMIN or EPOT return everything
//        if (auth != null && auth.isAuthenticated()) {
//            if (securityService.hasRole(auth, "ROLE_ADMIN") ||
//                    securityService.hasRole(auth, "ROLE_EPOT")) {
//                return super.getAll(ff, auth);
//            }
//            // if user is PROVIDER ADMIN return all his Providers (rejected, pending) with their sensitive data (Users, MainContact) too
//            User user = User.of(auth);
//            Browsing<ProviderBundle> providers = super.getAll(ff, auth);
//            for (ProviderBundle providerBundle : providers.getResults()){
//                if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId()) ||
//                        securityService.userIsProviderAdmin(user, providerBundle.getId())) {
//                    retList.add(providerBundle);
//                }
//            }
//            providers.setResults(retList);
//            providers.setTotal(retList.size());
//            providers.setTo(retList.size());
//            userProviders = getMyServiceProviders(auth);
//            if (userProviders != null) {
//                // replace user providers having null users with complete provider entries
//                userProviders.forEach(x -> {
//                    providers.getResults().removeIf(provider -> provider.getId().equals(x.getId()));
//                    providers.getResults().add(x);
//                });
//            }
//            return providers;
//        }
//
//        // else return ONLY approved Providers
//        ff.addFilter("status", "approved provider");
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