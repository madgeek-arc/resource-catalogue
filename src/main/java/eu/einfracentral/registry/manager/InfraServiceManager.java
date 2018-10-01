package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceMetadata;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.utils.ServiceValidators;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.service.SearchService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service("infraServiceService")
public class InfraServiceManager extends ServiceResourceManager implements InfraServiceService<InfraService, InfraService> {

    @Autowired
    private VocabularyManager vocabularyManager;

    @Autowired
    private ProviderManager providerManager;

    private static final Logger logger = LogManager.getLogger(InfraServiceManager.class);

    public InfraServiceManager() {
        super(InfraService.class);
    }

    @Override
    public String getResourceType() {
        return "infra_service";
    }

    private String getUser(Authentication auth) {
        if (auth instanceof OIDCAuthenticationToken) {
            return ((OIDCAuthenticationToken) auth).getUserInfo().getName();
        } else
            throw new AccessDeniedException("not an OIDCAuthentication");
    }

    @Override
    public InfraService addService(InfraService infraService, Authentication authentication) throws Exception {
        InfraService ret;
        try {
            validate(infraService);
            infraService.setActive(false);
            String id = createServiceId(infraService);
            infraService.setId(id);
            logger.info("Created service with id: " + id);
            logger.info("Providers: " + infraService.getProviders());

            if (infraService.getServiceMetadata() == null) {
                ServiceMetadata serviceMetadata = createServiceMetadata(getUser(authentication));
                infraService.setServiceMetadata(serviceMetadata);
            }

            ret = super.add(infraService, authentication);
        } catch (Exception e) {
            logger.error(e);
            throw e;
        }
        return ret;
    }

    @Override
    public InfraService updateService(InfraService infraService, Authentication authentication) throws Exception {
        InfraService ret;
        try {
            validate(infraService);
            InfraService existingService = getLatest(infraService.getId());
            if (authentication != null) {
                logger.info("User: " + authentication.getDetails());
            }

            if (infraService.getVersion().equals(existingService.getVersion())) {
                // update existing service serviceMetadata
                ServiceMetadata serviceMetadata = updateServiceMetadata(existingService.getServiceMetadata(), getUser(authentication));
                infraService.setServiceMetadata(serviceMetadata);
                ret = super.update(infraService, authentication);

            } else {
                ret = add(infraService, authentication);
            }
        } catch (Exception e) {
            logger.error(e);
            throw e;
        }
        return ret;
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
    public InfraService eInfraCentralUpdate(InfraService infraService) {
        InfraService ret = null;
        try {
            ret = migrate(infraService);
//            validate(ret);
            InfraService existingService = getLatest(infraService.getId());

            // update existing service serviceMetadata
            ServiceMetadata serviceMetadata = updateServiceMetadata(existingService.getServiceMetadata(), "eInfraCentral");
            ret.setServiceMetadata(serviceMetadata);
            ret = super.update(ret, null);

        } catch (Exception e) {
            logger.error(e);
        }
        return ret;
    }

    @Override
    public Browsing<InfraService> getAll(FacetFilter ff, Authentication authentication) {
        return super.getAll(ff, authentication);
    }

    @Override
    public Browsing<InfraService> getMy(FacetFilter facetFilter, Authentication authentication) {
        return null;
    }

    @Override
    public void delete(InfraService o) {

    }

    @Override
    public boolean validate(InfraService service) throws Exception {
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        //just check if validateVocabularies did anything or not
        validateServices(service);
        validateProviders(service);
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
        return true;
    }

    //logic for migrating our data to release schema; can be a no-op when outside of migratory period
    private InfraService migrate(InfraService service) {
        service.setActive(true);
        return service;
    }

    private ServiceMetadata updateServiceMetadata(ServiceMetadata serviceMetadata, String modifiedBy) {
        ServiceMetadata ret;
        if (serviceMetadata == null) {
            ret = createServiceMetadata(modifiedBy);
        } else {
            ret = serviceMetadata;
        }
        ret.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        ret.setModifiedBy(modifiedBy); //TODO: get actual username from backend
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

    private String createServiceId(Service service) {
        String provider = service.getProviders().get(0);
        return String.format("%s.%s", provider, service.getName().replaceAll("[^a-zA-Z\\s]+", "").replaceAll(" ", "_").toLowerCase());
    }

    private void validateVocabularies(InfraService service) throws Exception {
        if (service.getCategory() == null || !vocabularyManager.exists(
                new SearchService.KeyValue("type", "Category"),
                new SearchService.KeyValue("vocabulary_id", service.getCategory()))) {
            throw new Exception(String.format("Category '%s' does not exist.", service.getCategory()));
        }
        if (service.getSubcategory() == null || !vocabularyManager.exists(
                new SearchService.KeyValue("type", "Subcategory"),
                new SearchService.KeyValue("vocabulary_id", service.getSubcategory()))) {
            throw new Exception(String.format("Subcategory '%s' does not exist.", service.getSubcategory()));
        }

        if (service.getPlaces() != null && CollectionUtils.isNotEmpty(service.getPlaces())) {
            if (!service.getPlaces().parallelStream().allMatch(place -> vocabularyManager.exists(
                    new SearchService.KeyValue("type", "Place"),
                    new SearchService.KeyValue("vocabulary_id", place)))) {
                throw new Exception("One or more places do not exist.");
            }
        } else throw new Exception("field 'places' is obligatory");
        if (service.getLanguages() != null && CollectionUtils.isNotEmpty(service.getLanguages())) {
            if (!service.getLanguages().parallelStream().allMatch(lang -> vocabularyManager.exists(
                    new SearchService.KeyValue("type", "Language"),
                    new SearchService.KeyValue("vocabulary_id", lang)))) {
                throw new Exception("One or more languages do not exist.");
            }
        } else throw new Exception("field 'languages' is obligatory");
        if (service.getLifeCycleStatus() == null || !vocabularyManager.exists(
                new SearchService.KeyValue("type", "LifeCycleStatus"),
                new SearchService.KeyValue("vocabulary_id", service.getLifeCycleStatus()))) {
            throw new Exception(String.format("lifeCycleStatus '%s' does not exist.", service.getLifeCycleStatus()));
        }
        if (service.getTrl() == null || !vocabularyManager.exists(
                new SearchService.KeyValue("type", "TRL"),
                new SearchService.KeyValue("vocabulary_id", service.getTrl()))) {
            throw new Exception(String.format("trl '%s' does not exist.", service.getTrl()));
        }
    }

    //validates the correctness of Providers.
    private void validateProviders(InfraService service) throws Exception {
        List<String> providers = service.getProviders();
        List<String> existingProviders = new ArrayList<>();
        if (providers == null || CollectionUtils.isEmpty(service.getProviders())) {
            throw new Exception("field 'providers' is obligatory");
        }
        if (service.getProviders().stream().anyMatch(x -> providerManager.getResource(x) == null)) {
            throw new Exception("Provider does not exist");
        }
    }

    //validates the correctness of Related and Required Services.
    public void validateServices(InfraService service) throws Exception {
        List<String> relatedServices = service.getRelatedServices();
        List<String> existingRelatedServices = new ArrayList<>();
        if (relatedServices != null) {
            for (String serviceRel : relatedServices) {
                //logger.info("Inside loop relatedServices: " + serviceRel);
                if (this.exists(new SearchService.KeyValue("infra_service_id", serviceRel))) {
                    existingRelatedServices.add(serviceRel);
                }
            }
            service.setRelatedServices(existingRelatedServices);
        }

        //logger.info(infraService.toString());

        List<String> requiredServices = service.getRequiredServices();
        List<String> existingRequiredServices = new ArrayList<>();
        if (requiredServices != null) {
            for (String serviceReq : requiredServices) {
                //logger.info("Inside for requiredServices: " + serviceReq);
                if (this.exists(

                        new SearchService.KeyValue("infra_service_id", serviceReq))) {
                    existingRequiredServices.add(serviceReq);
                }
            }
            service.setRequiredServices(existingRequiredServices);
        }
        //logger.info(infraService.toString());
    }
}
