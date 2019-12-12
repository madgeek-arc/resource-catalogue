package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.utils.ObjectUtils;
import eu.einfracentral.validator.FieldValidator;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static eu.einfracentral.config.CacheConfig.CACHE_FEATURED;

@org.springframework.stereotype.Service("infraServiceService")
public class InfraServiceManager extends AbstractServiceManager implements InfraServiceService<InfraService, InfraService> {

    private static final Logger logger = LogManager.getLogger(InfraServiceManager.class);

    private ProviderManager providerManager;
    private Random randomNumberGenerator;
    private FieldValidator fieldValidator;

    @Value("${project.name:}")
    private String projectName;

    @Autowired
    public InfraServiceManager(ProviderManager providerManager, Random randomNumberGenerator,
                               @Lazy FieldValidator fieldValidator) {
        super(InfraService.class);
        this.providerManager = providerManager;
        this.randomNumberGenerator = randomNumberGenerator;
        this.fieldValidator = fieldValidator;
    }

    @Override
    public String getResourceType() {
        return "infra_service";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.providerCanAddServices(#auth, #infraService)")
    public InfraService addService(InfraService infraService, Authentication auth) {
        if ((infraService.getService().getId() == null) || ("".equals(infraService.getService().getId()))) {
            String id = Service.createId(infraService.getService());
            infraService.getService().setId(id);
        }
        validate(infraService);
        infraService.setActive(providerManager.get(infraService.getService().getProviders().get(0)).isActive());

        infraService.setLatest(true);

        if (infraService.getMetadata() == null) {
            infraService.setMetadata(Metadata.createMetadata(new User(auth).getFullName()));
        }

        logger.info("Adding Service: {}", infraService);
        InfraService ret;
        ret = super.add(infraService, auth);

        providerManager.verifyNewProviders(infraService.getService().getProviders(), auth);

        return ret;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or " +
            "@securityService.providerCanAddServices(#auth, #infraService)")
    public InfraService updateService(InfraService infraService, Authentication auth) {
        InfraService ret;
        validate(infraService);
        InfraService existingService;

        // if service version is empty set it null
        if ("".equals(infraService.getService().getVersion())) {
            infraService.getService().setVersion(null);
        }

        try { // try to find a service with the same id and version
            existingService = get(infraService.getService().getId(), infraService.getService().getVersion());
        } catch (ResourceNotFoundException e) {
            // if a service with version = infraService.getVersion() does not exist, get the latest service
            existingService = get(infraService.getService().getId());
        }

        // update existing service serviceMetadata
        infraService.setMetadata(
                Metadata.updateMetadata(existingService.getMetadata(), new User(auth).getFullName()));
        infraService.setActive(existingService.isActive());

        if ((infraService.getService().getVersion() == null && existingService.getService().getVersion() == null)
                || infraService.getService().getVersion() != null
                && infraService.getService().getVersion().equals(existingService.getService().getVersion())) {
            infraService.setLatest(existingService.isLatest());
            infraService.setStatus(existingService.getStatus());
            ret = super.update(infraService, auth);
            logger.info("Updating Service without version change: {}", infraService);
            logger.info("Service Version: {}", infraService.getService().getVersion());

        } else {
            // create new service and AFTERWARDS update the previous one (in case the new service cannot be created)
//                infraService.setStatus(); // TODO: enable this when services support the Status field

            // set previous service not latest
            existingService.setLatest(false);
            super.update(existingService, auth);
            logger.info("Updating Service with version change (super.update): {}", existingService);
            logger.info("Service Version: {}", existingService.getService().getVersion());

            // set new service as latest
            infraService.setLatest(true);
            ret = super.add(infraService, auth);
            logger.info("Updating Service with version change (super.add): {}", infraService);
        }

        return ret;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or " +
            "@securityService.userIsServiceProviderAdmin(#auth, #infraService.id)")
    public void delete(InfraService infraService) {
        super.delete(infraService);
        logger.info("Deleting Service: {}", infraService);
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
        List<ProviderBundle> providers = providerManager.getAll(ff, null).getResults();
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
                InfraService existingService = get(infraService.getService().getId());

                // update existing service serviceMetadata
                infraService.setMetadata(Metadata.updateMetadata(existingService.getMetadata(), projectName));

                super.update(infraService, null);
                logger.info("Updating Service through merging: {}", infraService);
                ret.add(infraService);

            } catch (Exception e) {
                logger.error(e);
            }
        }
        return ret;
    }

    @Override
    public boolean validate(InfraService infraService) {
        Service service = infraService.getService();
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        //just check if validateVocabularies did anything or not
        logger.debug("Validating Service with id: {}", service.getId());

        try {
            fieldValidator.validateFields(infraService.getService());
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }

        validateExtraFields(service);

        return true;
    }

    // Validate the correctness of Service Aggregator Information
    private void validateExtraFields(Service service) {
        if (service.getAggregatedServices() == null) {
            service.setAggregatedServices(1);
        } else if (service.getAggregatedServices() < 1) {
            throw new ValidationException("Aggregated services cannot be less than 1");
        }
        if (service.getPublications() == null) {
            service.setPublications(0);
        } else if (service.getPublications() < 0) {
            throw new ValidationException("Publications number cannot be negative");
        }
        if (service.getDatasets() == null) {
            service.setDatasets(0);
        } else if (service.getDatasets() < 0) {
            throw new ValidationException("Data(sets) number cannot be negative");
        }
        if (service.getSoftware() == null) {
            service.setSoftware(0);
        } else if (service.getSoftware() < 0) {
            throw new ValidationException("Software number cannot be negative");
        }
        if (service.getApplications() == null) {
            service.setApplications(0);
        } else if (service.getApplications() < 0) {
            throw new ValidationException("Applications number cannot be negative");
        }
        if (service.getOtherProducts() == null) {
            service.setOtherProducts(0);
        } else if (service.getOtherProducts() < 0) {
            throw new ValidationException("Other products number cannot be negative");
        }
    }

    //logic for migrating our data to release schema; can be a no-op when outside of migratory period
//    private InfraService migrate(InfraService service) throws MalformedURLException {
//        return service;
//    }

}
