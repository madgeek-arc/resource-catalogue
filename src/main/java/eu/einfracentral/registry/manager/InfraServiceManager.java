package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceMetadata;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.SearchService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service("infraServiceService")
public class InfraServiceManager extends ServiceResourceManager implements InfraServiceService<InfraService,InfraService> {

    @Autowired
    private VocabularyManager vocabularyManager;

    private Logger logger = Logger.getLogger(InfraServiceManager.class);

    public InfraServiceManager() {
        super(InfraService.class);
    }

    @Override
    public String getResourceType() {
        return "infra_service";
    }

    @Override
    public InfraService add(InfraService infraService, Authentication authentication) {

        migrate(infraService);
        // TODO: THROW EXCEPTION INSTEAD OF RETURNING NULL
        if (infraService.getId() == null) {
            String id = createServiceId(infraService);
            infraService.setId(id);
            logger.info("Providers: " + infraService.getProviders());

            logger.info("Created service with id: " + id);
        } else return null;

        if (infraService.getServiceMetadata() == null) {
            ServiceMetadata serviceMetadata = createServiceMetadata(infraService.getProviderName()); //FIXME: get name from backend
            infraService.setServiceMetadata(serviceMetadata);
        }
        InfraService ret;
        try {
            validate(infraService);
            ret = super.add(infraService,authentication);
        } catch (Exception e) {
            logger.error(e);
            ret = null;
        }
        return ret;
    }

    @Override
    public InfraService update(InfraService infraService, Authentication authentication) throws ResourceNotFoundException {
//        infraService.setService(validate(infraService.getService()));
        InfraService existingService = getLatest(infraService.getId());

        InfraService ret = null;
        if (infraService.getVersion().equals(existingService.getVersion())) {
            try {
                validate(infraService);
                // update existing service serviceMetadata
                ServiceMetadata serviceMetadata = updateServiceMetadata(existingService.getServiceMetadata(), infraService.getProviderName());
                infraService.setServiceMetadata(serviceMetadata);
                // replace existing service with new
                ret = super.update(infraService,authentication);
            } catch (Exception e) {
                logger.error(e);
            }
        } else {
//            Resource existingResource = getResource(infraService.getId(), infraService.getVersion());
//            existingService.setId(String.format("%s/%s", existingService.getId(), existingService.getVersion()));

            // save in exististing resource the payload of the updated service
//            existingResource.setPayload(serialize(infraService));
//            resourceService.updateResource(existingResource);

            // create new service
            ret = add(infraService,authentication);
        }
        return ret;
    }

    @Override
    public Browsing<InfraService> getAll(FacetFilter ff, Authentication authentication) {
        return super.getAll(ff,authentication);
//        Browsing<InfraService> services = super.getAll(ff);
//        services.setResults(services.getResults().stream().map(this::FillTransientFields).collect(Collectors.toList()));
//        return services;
    }

    @Override
    public Browsing<InfraService> getMy(FacetFilter facetFilter, Authentication authentication) {
        return null;
    }

    @Override
    public void delete(InfraService o) {

    }

    //    @Override
    public InfraService validate(InfraService service) throws Exception {
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        //just check if validateVocabularies did anything or not
        service = validateServices(service);
        service = validateName(service);
        service = validateVocabularies(service);
        service = validateURL(service);
        service = validateProviderName(service);
        service = validateDescription(service);
        service = validateSymbol(service);
        service = validateVersion(service);
        return service;
    }

    //logic for migrating our data to release schema; can be a no-op when outside of migratory period
    private InfraService migrate(InfraService service) {
        return service;
    }

    //yes, this is foreign key logic right here on the application
    private InfraService validateVocabularies(InfraService service) throws Exception {
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
        if (service.getPlaces() != null) {
            if (!service.getPlaces().parallelStream().allMatch(place-> vocabularyManager.exists(
                    new SearchService.KeyValue("type", "Place"),
                    new SearchService.KeyValue("vocabulary_id", place)))) {
                throw new Exception("One or more places do not exist.");
            }
        } else throw new Exception("field 'places' is obligatory");
        if (service.getLanguages() != null) {
            if (!service.getLanguages().parallelStream().allMatch(lang-> vocabularyManager.exists(
                    new SearchService.KeyValue("type", "Language"),
                    new SearchService.KeyValue("vocabulary_id", lang)))) {
                throw new Exception("One or more languages do not exist.");
            }
        } else throw new Exception("field 'languages' is obligatory");
        if (!vocabularyManager.exists(
                new SearchService.KeyValue("type", "LifeCycleStatus"),
                new SearchService.KeyValue("vocabulary_id", service.getLifeCycleStatus()))) {
            throw new Exception(String.format("lifeCycleStatus '%s' does not exist.", service.getLifeCycleStatus()));
        }
        if (!vocabularyManager.exists(
                new SearchService.KeyValue("type", "TRL"),
                new SearchService.KeyValue("vocabulary_id", service.getTrl()))) {
            throw new Exception(String.format("trl '%s' does not exist.", service.getTrl()));
        }
        return service;
    }

    //validates the correctness of Service Name.
    private InfraService validateName(InfraService service) throws Exception {
        if (service.getName() == null || service.getName().equals("")) {
            throw new Exception("field 'name' is obligatory");
        }
        return service;
    }

    //validates the correctness of Service URL.
    private InfraService validateURL(InfraService service) throws Exception{
        if (service.getUrl() == null || service.getUrl().equals("")){
            throw new Exception("field 'url' is mandatory");
        }
     return service;
    }

    //validates the correctness of Provider Name.
    private InfraService validateProviderName(InfraService service) throws Exception{
        if (service.getProviderName() == null || service.getProviderName().equals("")){
            throw new Exception("field 'providerName' is mandatory");
        }
        return service;
    }

    //validates the correctness of Service Description.
    private InfraService validateDescription(InfraService service) throws Exception{
        if (service.getDescription() == null || service.getDescription().equals("")){
            throw new Exception("field 'description' is mandatory");
        }
        return service;
    }

    //validates the correctness of Service Symbol.
    private InfraService validateSymbol(InfraService service) throws Exception{
        if (service.getSymbol() == null || service.getSymbol().equals("")){
            throw new Exception("field 'symbol' is mandatory");
        }
        return service;
    }

    //validates the correctness of Service Version.
    private InfraService validateVersion(InfraService service) throws Exception {
        if (service.getVersion() == null || service.getVersion().equals("")) {
            throw new Exception("field 'version' is mandatory");
        }
        return service;
    }

    //validates the correctness of Related and Required Services.
    private InfraService validateServices(InfraService service) throws Exception {
        List<String> relatedServices = service.getRelatedServices();
        List<String> existingRelatedServices = new ArrayList<>();
        for (String serviceRel : relatedServices) {
            //logger.info("Inside loop relatedServices: " + serviceRel);
            if (this.exists(new SearchService.KeyValue("infra_service_id", serviceRel)))
            {
                existingRelatedServices.add(serviceRel);
            }
        }
        service.setRelatedServices(existingRelatedServices);

        //logger.info(infraService.toString());

        List<String> requiredServices = service.getRequiredServices();
        List<String> existingRequiredServices = new ArrayList<>();
        for (String serviceReq : requiredServices) {
            //logger.info("Inside for requiredServices: " + serviceReq);
            if (this.exists(

                    new SearchService.KeyValue("infra_service_id", serviceReq)))

            {
                existingRequiredServices.add(serviceReq);
            }
        }
        service.setRequiredServices(existingRequiredServices);

        //logger.info(infraService.toString());

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
        String provider = service.getProviderName();
        return String.format("%s.%s", provider, service.getName().replaceAll("[^a-zA-Z\\s]+","").replaceAll(" ", "_").toLowerCase());
    }
}
