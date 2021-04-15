package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.utils.ObjectUtils;
import eu.einfracentral.validator.FieldValidator;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static eu.einfracentral.config.CacheConfig.CACHE_FEATURED;

@org.springframework.stereotype.Service("infraServiceService")
public class InfraServiceManager extends AbstractServiceManager implements InfraServiceService<InfraService, InfraService> {

    private static final Logger logger = LogManager.getLogger(InfraServiceManager.class);

    private final ProviderManager providerManager;
    private final Random randomNumberGenerator;
    private final FieldValidator fieldValidator;
    private final IdCreator idCreator;

    @Value("${project.name:}")
    private String projectName;

    @Autowired
    public InfraServiceManager(ProviderManager providerManager, Random randomNumberGenerator,
                               @Lazy FieldValidator fieldValidator, IdCreator idCreator) {
        super(InfraService.class);
        this.providerManager = providerManager;
        this.randomNumberGenerator = randomNumberGenerator;
        this.fieldValidator = fieldValidator;
        this.idCreator = idCreator;
    }

    @Override
    public String getResourceType() {
        return "infra_service";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.providerCanAddServices(#auth, #infraService)")
    public InfraService addService(InfraService infraService, Authentication auth) {
        if ((infraService.getService().getId() == null) || ("".equals(infraService.getService().getId()))) {
            String id = idCreator.createServiceId(infraService.getService());
            infraService.getService().setId(id);
        }
        validate(infraService);
        validateCategories(infraService.getService().getCategories());
        validateScientificDomains(infraService.getService().getScientificDomains());
        infraService.setActive(providerManager.get(infraService.getService().getResourceOrganisation()).isActive());

        infraService.setLatest(true);

        if (infraService.getMetadata() == null) {
            infraService.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }

        logger.info("Adding Service: {}", infraService);
        InfraService ret;
        ret = super.add(infraService, auth);

        return ret;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or " +
            "@securityService.isServiceProviderAdmin(#auth, #infraService)")
    public InfraService updateService(InfraService infraService, Authentication auth) {
        InfraService ret;
        validate(infraService);
        validateCategories(infraService.getService().getCategories());
        validateScientificDomains(infraService.getService().getScientificDomains());
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
        if ("".equals(existingService.getService().getVersion())) {
            existingService.getService().setVersion(null);
        }

        // update existing service serviceMetadata
        infraService.setMetadata(Metadata.updateMetadata(existingService.getMetadata(), User.of(auth).getFullName()));
        infraService.setActive(existingService.isActive());

        // if a user updates a service with version to a service with null version then while searching for the service
        // you get a "Service already exists" error.
        if (existingService.getService().getVersion() != null && infraService.getService().getVersion() == null){
            throw new ServiceException("You cannot update a Service registered with version to a Service with null version");
        }

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
    public void delete(InfraService infraService) {
        logger.info("Deleting Service: {}", infraService);
        super.delete(infraService);
    }

    @Override
    public Paging<InfraService> getInactiveServices() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        return getAll(ff, null);
    }

    @Override
    @Cacheable(CACHE_FEATURED)
    public List<Service> createFeaturedServices() {
        logger.info("Creating and caching 'featuredServices'");
        // TODO: return featured services (for now, it returns a random infraService for each provider)
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
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
        ff.setQuantity(maxQuantity);
        List<InfraService> services = getAll(ff, null).getResults();
        List<InfraService> ret = new ArrayList<>();
        for (InfraService infraService : services) {
            try {
//                migrate(infraService); // use this to make custom changes
                ObjectUtils.merge(infraService, service); // use this to make bulk changes FIXME: this method does not work as expected
                validate(infraService);
                validateCategories(infraService.getService().getCategories());
                validateScientificDomains(infraService.getService().getScientificDomains());
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

        return true;
    }

    @Override
    public InfraService publish(String serviceId, String version, boolean active, Authentication auth) {
        InfraService service;
        String activeProvider = "";
        if (version == null || "".equals(version)) {
            service = this.get(serviceId);
        } else {
            service = this.get(serviceId, version);
        }

        ProviderBundle providerBundle = providerManager.get(service.getService().getResourceOrganisation());
        if (providerBundle.getStatus().equals(Provider.States.APPROVED.getKey()) && providerBundle.isActive()) {
            activeProvider = service.getService().getResourceOrganisation();
        }
        if (active && activeProvider.equals("")) {
            throw new ResourceException("Service does not have active Providers", HttpStatus.CONFLICT);
        }
        service.setActive(active);
        this.update(service, auth);
        return service;
    }

    public void validateCategories(List<ServiceCategory> categories){
        for (ServiceCategory serviceCategory : categories){
            String[] parts = serviceCategory.getSubcategory().split("-");
            String category = "category-" + parts[1] + "-" + parts[2];
            if (!serviceCategory.getCategory().equals(category)){
                throw new ValidationException("Subcategory '" + serviceCategory.getSubcategory() + "' should have as Category the value '"
                        + category +"'");
            }
        }
    }

    public void validateScientificDomains(List<ServiceProviderDomain> scientificDomains){
        for (ServiceProviderDomain serviceScientificDomain : scientificDomains){
            String[] parts = serviceScientificDomain.getScientificSubdomain().split("-");
            String scientificDomain = "scientific_domain-" + parts[1];
            if (!serviceScientificDomain.getScientificDomain().equals(scientificDomain)){
                throw new ValidationException("Scientific Subdomain '" + serviceScientificDomain.getScientificSubdomain() +
                        "' should have as Scientific Domain the value '" + scientificDomain +"'");
            }
        }
    }

    //logic for migrating our data to release schema; can be a no-op when outside of migratory period
//    private InfraService migrate(InfraService service) throws MalformedURLException {
//        return service;
//    }

}
