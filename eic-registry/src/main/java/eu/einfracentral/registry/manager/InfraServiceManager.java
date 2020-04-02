package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.utils.ObjectUtils;
import eu.einfracentral.utils.ServiceValidators;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static eu.einfracentral.config.CacheConfig.CACHE_FEATURED;

@org.springframework.stereotype.Service("infraServiceService")
public class InfraServiceManager extends ServiceResourceManager implements InfraServiceService<InfraService, InfraService> {

    private static final Logger logger = LogManager.getLogger(InfraServiceManager.class);

    private ServiceValidators serviceValidators;
    private ProviderManager providerManager;
    private Random randomNumberGenerator;


    @Autowired
    public InfraServiceManager(ServiceValidators serviceValidators, ProviderManager providerManager,
                               Random randomNumberGenerator) {
        super(InfraService.class);
        this.serviceValidators = serviceValidators;
        this.providerManager = providerManager;
        this.randomNumberGenerator = randomNumberGenerator;
    }

    @Override
    public String getResourceType() {
        return "infra_service";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') and " +
            "@securityService.providerCanAddServices(#authentication, #infraService)")
    public InfraService addService(InfraService infraService, Authentication authentication) {
        InfraService ret;
        validate(infraService);
        infraService.setActive(providerManager.get(infraService.getProviders().get(0)).getActive());
        if ((infraService.getId() == null) || ("".equals(infraService.getId()))) {
            String id = createServiceId(infraService);
            infraService.setId(id);
        }
        infraService.setLatest(true);

        if (infraService.getServiceMetadata() == null) {
            ServiceMetadata serviceMetadata = createServiceMetadata(new User(authentication).getFullName());
            infraService.setServiceMetadata(serviceMetadata);
        }

        ret = super.add(infraService, authentication);
        logger.info(String.format("Adding Service %s", infraService));

        providerManager.verifyNewProviders(infraService.getProviders(), authentication);

        return ret;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or " +
            "@securityService.userIsServiceProviderAdmin(#authentication, #infraService)")
    public InfraService updateService(InfraService infraService, Authentication authentication) {
        InfraService ret;
        validate(infraService);
        InfraService existingService;

        try { // try to find a service with the same id and version
            existingService = get(infraService.getId(), infraService.getVersion());
        } catch (ResourceNotFoundException e) {
            // if a service with version = infraService.getVersion() does not exist, get the latest service
            existingService = get(infraService.getId());
        }

        // update existing service serviceMetadata
        ServiceMetadata serviceMetadata = updateServiceMetadata(existingService.getServiceMetadata(), new User(authentication).getFullName());
        infraService.setServiceMetadata(serviceMetadata);
        infraService.setActive(existingService.isActive());

        if (infraService.getVersion().equals(existingService.getVersion())) {
            infraService.setLatest(existingService.isLatest());
            infraService.setStatus(existingService.getStatus());
            ret = super.update(infraService, authentication);
            logger.info(String.format("Updating Service %s with no version changes", infraService));
            logger.info(String.format("Service Version: %s", infraService.getVersion()));

        } else {
            // create new service and AFTERWARDS update the previous one (in case the new service cannot be created)
//                infraService.setStatus(); // TODO: enable this when services support the Status field
            // set new service as latest
            infraService.setLatest(true);
            ret = super.add(infraService, authentication);
            logger.info(String.format("Updating Service %s with version changes (super.add)", infraService));

            // set previous service not latest
            existingService.setLatest(false);
            super.update(existingService, authentication);
            logger.info(String.format("Updating Service %s with version changes (super.update)", infraService));
            logger.info(String.format("Service Version: %s", infraService.getVersion()));
        }

        return ret;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') and " +
            "@securityService.userIsServiceProviderAdmin(#authentication, #infraService.id)")
    public void delete(InfraService infraService) {
        super.delete(infraService);
        logger.info("Deleting Service " + infraService);
    }

    @Override
    public Paging<InfraService> getInactiveServices() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", "false");
        ff.setFrom(0);
        ff.setQuantity(10000);
        return getAll(ff, null);
    }

    @Override
    @Cacheable(CACHE_FEATURED)
    public List<Service> createFeaturedServices() {
        logger.info("Creating and caching 'featuredServices'");
        // TODO: return featured services (for now, it returns a random infraService for each provider)
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<Provider> providers = providerManager.getAll(ff, null).getResults();
        List<Service> featuredServices = new ArrayList<>();
        List<Service> services;
        for (int i = 0; i < providers.size(); i++) {
            int rand = randomNumberGenerator.nextInt(providers.size());
            services = providerManager.getActiveServices(providers.get(rand).getId());
            providers.remove(rand); // remove provider from list to avoid duplicate provider highlights
            if (!services.isEmpty()) {
                featuredServices.add(services.get(randomNumberGenerator.nextInt(services.size())));
            }
        }
        return featuredServices;
    }

    @Override
    public List<InfraService> eInfraCentralUpdate(InfraService service) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<InfraService> services = getAll(ff, null).getResults();
        List<InfraService> ret = new ArrayList<>();
        for (InfraService infraService : services) {
            try {
//                migrate(infraService); // use this to make custom changes
                ObjectUtils.merge(infraService, service); // use this to make bulk changes FIXME: this method does not work as expected
                validate(infraService);
                InfraService existingService = get(infraService.getId());

                // update existing service serviceMetadata
                ServiceMetadata serviceMetadata = updateServiceMetadata(existingService.getServiceMetadata(), "eInfraCentral");
                infraService.setServiceMetadata(serviceMetadata);

                super.update(infraService, null);
                logger.info(String.format("Updating Service %s through merging", infraService));
                ret.add(infraService);

            } catch (Exception e) {
                logger.error(e);
            }
        }
        return ret;
    }

    @Override
    public boolean validate(InfraService service) {
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        //just check if validateVocabularies did anything or not
        logger.debug(String.format("Validating Service with id: %s", service.getId()));
        serviceValidators.validateServices(service);
        serviceValidators.validateVocabularies(service);
        serviceValidators.validateName(service);
        serviceValidators.validateURL(service);
        serviceValidators.validateDescription(service);
        serviceValidators.validateSymbol(service);
        serviceValidators.validateVersion(service);
        serviceValidators.validateLastUpdate(service);
        serviceValidators.validateOrder(service);
        serviceValidators.validateSLA(service);
        serviceValidators.validateMaxLength(service);
        serviceValidators.validateProviders(service);
        serviceValidators.validateExtraFields(service);
        return true;
    }

    //logic for migrating our data to release schema; can be a no-op when outside of migratory period
//    private InfraService migrate(InfraService service) throws MalformedURLException {
//        return service;
//    }

    private ServiceMetadata updateServiceMetadata(ServiceMetadata serviceMetadata, String modifiedBy) {
        ServiceMetadata ret;
        if (serviceMetadata != null) {
            ret = new ServiceMetadata(serviceMetadata);
            ret.setModifiedAt(String.valueOf(System.currentTimeMillis()));
            ret.setModifiedBy(modifiedBy);
        } else {
            ret = createServiceMetadata(modifiedBy);
        }
        return ret;
    }

    private ServiceMetadata createServiceMetadata(String registeredBy) {
        ServiceMetadata ret = new ServiceMetadata();
        ret.setRegisteredBy(registeredBy);
        ret.setRegisteredAt(String.valueOf(System.currentTimeMillis()));
        ret.setModifiedBy(registeredBy);
        ret.setModifiedAt(ret.getRegisteredAt());
        return ret;
    }
}
