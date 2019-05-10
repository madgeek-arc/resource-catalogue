package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.service.SynchronizerService;
import eu.einfracentral.utils.ObjectUtils;
import eu.einfracentral.utils.ServiceValidators;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.service.SearchService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;

import java.util.*;

@org.springframework.stereotype.Service("infraServiceService")
public class InfraServiceManager extends ServiceResourceManager implements InfraServiceService<InfraService, InfraService> {

    private static final Logger logger = LogManager.getLogger(InfraServiceManager.class);

    private VocabularyManager vocabularyManager;
    private ProviderManager providerManager;
    private Random randomNumberGenerator;
    private SynchronizerService synchronizerService;


    @Autowired
    public InfraServiceManager(VocabularyManager vocabularyManager, ProviderManager providerManager,
                               Random randomNumberGenerator, SynchronizerService synchronizerService) {
        super(InfraService.class);
        this.vocabularyManager = vocabularyManager;
        this.providerManager = providerManager;
        this.randomNumberGenerator = randomNumberGenerator;
        this.synchronizerService = synchronizerService;
    }

    @Override
    public String getResourceType() {
        return "infra_service";
    }

    @Override
    public InfraService addService(InfraService infraService, Authentication authentication) {
        InfraService ret;
        try {
            validate(infraService);
            infraService.setActive(providerManager.get(infraService.getProviders().get(0)).getActive());
            if ((infraService.getId() == null) || ("".equals(infraService.getId()))) {
                String id = createServiceId(infraService);
                infraService.setId(id);
            }
            infraService.setLatest(true);
            logger.info("Creating service with id: " + infraService.getId() + " and version:" + infraService.getVersion());
            logger.info("Providers: " + infraService.getProviders());

            if (infraService.getServiceMetadata() == null) {
                ServiceMetadata serviceMetadata = createServiceMetadata(new User(authentication).getFullName());
                infraService.setServiceMetadata(serviceMetadata);
            }

            ret = super.add(infraService, authentication);
            synchronizerService.syncAdd(infraService);

            // search if there are other provider services
            FacetFilter ff = new FacetFilter();
            ff.addFilter("providers", infraService.getProviders().get(0));
            if (this.getAll(ff, null).getTotal() == 1) { // user just added the service
                providerManager.verifyProvider(infraService.getProviders().get(0), Provider.States.PENDING_2, null, authentication);
            }
        } catch (Exception e) {
            logger.error(e);
            throw e;
        }
        return ret;
    }

    @Override
    public InfraService updateService(InfraService infraService, Authentication authentication) {
        InfraService ret;
        validate(infraService);
        InfraService existingService = get(infraService.getId());
        if (authentication != null) {
            logger.info("User: " + authentication.getDetails());
        }

        // update existing service serviceMetadata
        ServiceMetadata serviceMetadata = updateServiceMetadata(existingService.getServiceMetadata(), new User(authentication).getFullName());
        infraService.setServiceMetadata(serviceMetadata);

        if (infraService.getVersion().equals(existingService.getVersion())) {
            infraService.setActive(existingService.isActive());
            infraService.setLatest(existingService.isLatest());
            infraService.setStatus(existingService.getStatus());
            ret = super.update(infraService, authentication);

        } else {
            // create new service and AFTERWARDS update the previous one (in case the new service cannot be created)
//                infraService.setStatus(); // TODO: enable this when services support the Status field
            // set new service as latest
            infraService.setLatest(true);
            ret = super.add(infraService, authentication);

            // set previous service not latest
            existingService.setLatest(false);
            super.update(existingService, authentication);

        }
        synchronizerService.syncUpdate(infraService);
        return ret;
    }

    @Override
    public void delete(InfraService infraService) {
        synchronizerService.syncDelete(infraService);
        super.delete(infraService);
    }

    @Override
    public Paging<InfraService> getInactiveServices() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", "false");
        ff.setFrom(0);
        ff.setQuantity(10000);
        return getAll(ff, null);
    }

    @Scheduled(cron = "0 0 12 1/1 * ?") // daily at 12:00 PM
    @CacheEvict(value = "featuredServices", allEntries = true)
    public void refreshFeatured() {
    }

    @Override
    @Cacheable("featuredServices")
    public List<Service> createFeaturedServices() {
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
        validateServices(service);
        validateVocabularies(service);
        ServiceValidators.validateName(service);
        ServiceValidators.validateURL(service);
        ServiceValidators.validateDescription(service);
        ServiceValidators.validateSymbol(service);
        ServiceValidators.validateVersion(service);
        ServiceValidators.validateLastUpdate(service);
        ServiceValidators.validateOrder(service);
        ServiceValidators.validateSLA(service);
        ServiceValidators.validateMaxLength(service);
        validateProviders(service);
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


    private void validateVocabularies(InfraService service) {

        //Validate Categories/Subcategories
        if (service.getCategory() != null) {
            Vocabulary categories = vocabularyManager.get("categories");
            VocabularyEntry category = categories.getEntries().get(service.getCategory());
            if (category == null)
                throw new ValidationException(String.format("category '%s' does not exist.", service.getCategory()));
            List<VocabularyEntry> subcategory = category.getChildren();
            if (service.getSubcategory() == null) {
                throw new ValidationException("Field 'subcategory' is mandatory.");
            }
            boolean flag = false;
            for (VocabularyEntry aSubcategory : subcategory) {
                if (aSubcategory.getId().equals(service.getSubcategory())) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                throw new ValidationException(String.format("subcategory '%s' does not exist.", service.getSubcategory()));
            }
        } else throw new ValidationException("Field 'category' is mandatory.");

        //Validate Places
        if (service.getPlaces() != null && !service.getPlaces().isEmpty()) {
            Map<String, VocabularyEntry> places = vocabularyManager.get("places").getEntries();
            List<String> servicePlaces = service.getPlaces();
            List<String> notFoundPlaces = new ArrayList<>();
            List<String> foundPlaces = new ArrayList<>();
            for (String place : servicePlaces) {
                VocabularyEntry placeFound = places.get(place);
                if (placeFound == null) {
                    notFoundPlaces.add(place);
                }
                if (places.containsKey(place) && !foundPlaces.contains(place)){
                    foundPlaces.add(place);
                }
            }
            if (!notFoundPlaces.isEmpty()) {
                throw new ValidationException(String.format("Places not found: %s", String.join(", ", notFoundPlaces)));
            }
            service.setPlaces(foundPlaces);
        } else throw new ValidationException("Field 'places' is mandatory.");

        //Validate Languages
        if (service.getLanguages() != null && !service.getLanguages().isEmpty()) {
            Map<String, VocabularyEntry> languages = vocabularyManager.get("languages").getEntries();
            List<String> serviceLanguages = service.getLanguages();
            List<String> notFoundLanguages = new ArrayList<>();
            List<String> foundLanguages = new ArrayList<>();
            for (String language : serviceLanguages) {
                VocabularyEntry languageFound = languages.get(language);
                if (languageFound == null) {
                    notFoundLanguages.add(language);
                }
                if (languages.containsKey(language) && !foundLanguages.contains(language)){
                    foundLanguages.add(language);
                }
            }
            if (!notFoundLanguages.isEmpty()) {
                throw new ValidationException(String.format("Languages not found: %s", String.join(", ", notFoundLanguages)));
            }
            service.setLanguages(foundLanguages);
        } else throw new ValidationException("Field 'languages' is mandatory.");

        //Validate LifeCycleStatus
        if (service.getLifeCycleStatus() != null) {
            Vocabulary lifecyclestatus = vocabularyManager.get("lifecyclestatus");
            VocabularyEntry lfc = lifecyclestatus.getEntries().get(service.getLifeCycleStatus());
            if (lfc == null)
                throw new ValidationException(String.format("LifeCycleStatus '%s' does not exist.", service.getLifeCycleStatus()));
        } else throw new ValidationException("Field 'lifeCycleStatus' is mandatory.");

        //Validate TRL
        if (service.getTrl() != null) {
            Vocabulary trl = vocabularyManager.get("trl");
            VocabularyEntry trlEntry = trl.getEntries().get(service.getTrl());
            if (trlEntry == null)
                throw new ValidationException(String.format("TRL '%s' does not exist.", service.getTrl()));
        } else throw new ValidationException("Field 'trl' is mandatory.");
    }

    //validates the correctness of Providers.
    private void validateProviders(InfraService service) {
        List<String> providers = service.getProviders();
        List<String> validProviders = new ArrayList<>();
        if ((providers == null) || CollectionUtils.isEmpty(service.getProviders()) ||
                (service.getProviders().stream().filter(Objects::nonNull).mapToInt(p -> 1).sum() == 0)) {
            throw new ValidationException("field 'providers' is obligatory");
        }
        if (service.getProviders().stream().filter(Objects::nonNull).anyMatch(x -> providerManager.getResource(x) == null)) {
            throw new ValidationException("Provider does not exist");
        }
        for (String provider: providers){
            if (!validProviders.contains(provider)){
                validProviders.add(provider);
            }
        }
        service.setProviders(validProviders);
    }

    //validates the correctness of Related and Required Services.
    public void validateServices(InfraService service) {
        List<String> relatedServices = service.getRelatedServices();
        List<String> existingRelatedServices = new ArrayList<>();
        if (relatedServices != null) {
            for (String serviceRel : relatedServices) {
                if (this.exists(new SearchService.KeyValue("infra_service_id", serviceRel)) && !existingRelatedServices.contains(serviceRel)) {
                    existingRelatedServices.add(serviceRel);
                }
            }
            service.setRelatedServices(existingRelatedServices);
        }

        List<String> requiredServices = service.getRequiredServices();
        List<String> existingRequiredServices = new ArrayList<>();
        if (requiredServices != null) {
            for (String serviceReq : requiredServices) {
                if (this.exists(new SearchService.KeyValue("infra_service_id", serviceReq)) && !existingRequiredServices.contains(serviceReq)) {
                    existingRequiredServices.add(serviceReq);
                }
            }
            service.setRequiredServices(existingRequiredServices);
        }
    }
}
