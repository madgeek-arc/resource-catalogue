package eu.einfracentral.registry.manager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.UrlValidator;
import eu.einfracentral.core.ParserPool;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceMetadata;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service("infraServiceService")
public class InfraServiceManager extends ServiceResourceManager implements InfraServiceService {


    public class MyException extends Exception {

        public MyException() { }

        public MyException(String s) {
            super(s);
        }
    }

    @Autowired
    private VocabularyManager vocabularyManager;

    @Autowired
    private ProviderManager providerManager;

    @Autowired
    private SearchService searchService;

    @Autowired
    ParserPool parserPool;

    private Logger logger = Logger.getLogger(InfraServiceManager.class);

    public InfraServiceManager() {
        super(InfraService.class);
    }

    @Override
    public String getResourceType() {
        return "infra_service";
    }

    @Override
    public InfraService addService(InfraService infraService) throws Exception{

        migrate(infraService);
        if (infraService.getId() == null) {
            String id = createServiceId(infraService);
            infraService.setId(id);
            logger.info("Providers: " + infraService.getProviders());

            logger.info("Created service with id: " + id);
        } else return null;

        if (infraService.getServiceMetadata() == null) {
            ServiceMetadata serviceMetadata = createServiceMetadata(infraService.getEditorName()); //FIXME: get name from backend
            infraService.setServiceMetadata(serviceMetadata);
        }
        InfraService ret;
        try {
            validate(infraService);
            ret = super.add(infraService);
        } catch (Exception e) {
            logger.error(e);
            throw e;
        }
        return ret;
    }

    @Override
    public InfraService updateService(InfraService infraService) throws ResourceNotFoundException, Exception {
//        infraService.setService(validate(infraService.getService()));
        InfraService existingService = getLatest(infraService.getId());

        InfraService ret = null;
        if (infraService.getVersion().equals(existingService.getVersion())) {
            try {
                validate(infraService);
                // update existing service serviceMetadata
                ServiceMetadata serviceMetadata = updateServiceMetadata(existingService.getServiceMetadata(), infraService.getEditorName());
                infraService.setServiceMetadata(serviceMetadata);
                // replace existing service with new
                ret = super.update(infraService);
            } catch (Exception e) {
                logger.error(e);
                throw e;
            }
        } else {
            // create new service
            ret = add(infraService);
        }
        return ret;
    }

//    @Override
    public InfraService validate(InfraService service) throws Exception {
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        //just check if validateVocabularies did anything or not
        service = validateServices(service);
        service = validateName(service);
        service = validateVocabularies(service);
        service = validateURL(service);
//        service = validateProviderName(service);
        service = validateDescription(service);
        service = validateSymbol(service);
        service = validateVersion(service);
        service = validateLastUpdate(service);
        service = validateOrder(service);
        service = validateSLA(service);
        service = validateProviders(service);
        return service;
    }

    //logic for migrating our data to release schema; can be a no-op when outside of migratory period
    private InfraService migrate(InfraService service) {
        return service;
    }

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

//    //validates the correctness of Provider Name.
//    private InfraService validateProviderName(InfraService service) throws Exception{
//        if (service.getProviderName() == null || service.getProviderName().equals("")){
//            throw new Exception("field 'providerName' is mandatory");
//        }
//        return service;
//    }

    //validates the correctness of Providers.
    private InfraService validateProviders(InfraService service) throws Exception {
        List<String> providers = service.getProviders();
        List<String> existingProviders = new ArrayList<>();
        if (service.getProviders() == null) {
            throw new Exception("field 'providers' is obligatory");
        } if (service.getProviders().stream().noneMatch(x -> providerManager.getResource(x) != null)) {
            throw new Exception("Provider does not exist");
        }
        return service;
    }

    //validates the correctness of Service Description.
    private InfraService validateDescription(InfraService service) throws Exception {
        if (service.getDescription() == null || service.getDescription().equals("")){
            throw new Exception("field 'description' is mandatory");
        }
        return service;
    }

    //validates the correctness of Service Symbol.
    private InfraService validateSymbol(InfraService service) throws Exception {
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

    //validates the correctness of Service Last Update.
    private InfraService validateLastUpdate(InfraService service) throws Exception {
        if (service.getLastUpdate() == null || service.getLastUpdate().equals("")) {
            throw new Exception("field 'lastUpdate' is mandatory");
        }
        return service;
    }

    //validates the correctness of Service Order URL Page.
    private InfraService validateOrder(InfraService service) throws Exception {
        if (service.getOrder() == null || service.getOrder().equals("")) {
            throw new Exception("field 'order' is mandatory");
        }
        return service;
    }

    //validates the correctness of Service SLA.
    private InfraService validateSLA(InfraService service) throws Exception {
        if (service.getServiceLevelAgreement() == null || service.getServiceLevelAgreement().equals("")) {
            throw new Exception("field 'serviceLevelAgreement' is mandatory");
        }
        return service;
    }


    //validates the correctness of Related and Required Services.
    private InfraService validateServices(InfraService service) throws Exception {
        List<String> relatedServices = service.getRelatedServices();
        List<String> existingRelatedServices = new ArrayList<>();
        List<String> notFoundServices = new ArrayList<>();
        for (String serviceRel : relatedServices) {
            //logger.info("Inside loop relatedServices: " + serviceRel);
            if (this.exists(new SearchService.KeyValue("infra_service_id", serviceRel)))
                existingRelatedServices.add(serviceRel);
            else
                notFoundServices.add(serviceRel);
        }
        // TODO: decide if entering invalid service ids leads to Exception OR not.
        if (existingRelatedServices.size() != relatedServices.size())
            throw new Exception("relatedServices not found : " + String.join(", ", notFoundServices));

        service.setRelatedServices(existingRelatedServices);

        //logger.info(infraService.toString());

        List<String> requiredServices = service.getRequiredServices();
        List<String> existingRequiredServices = new ArrayList<>();
        for (String serviceReq : requiredServices) {
            //logger.info("Inside for requiredServices: " + serviceReq);
            if (this.exists(new SearchService.KeyValue("infra_service_id", serviceReq)))
                existingRequiredServices.add(serviceReq);
            else
                notFoundServices.add(serviceReq);
        }
        // TODO: decide if entering invalid service ids leads to Exception OR not.
        if (existingRequiredServices.size() != relatedServices.size())
            throw new Exception("requiredServices not found : " + String.join(", ", notFoundServices));

        service.setRequiredServices(existingRequiredServices);

        //logger.info(infraService.toString());

        return service;
    }

    @Override
    public Browsing<InfraService> getAll(FacetFilter ff) {
        return super.getAll(ff);
//        Browsing<InfraService> services = super.getAll(ff);
//        services.setResults(services.getResults().stream().map(this::FillTransientFields).collect(Collectors.toList()));
//        return services;
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
        return String.format("%s.%s", provider, service.getName().replaceAll("[^a-zA-Z\\s]+","").replaceAll(" ", "_").toLowerCase());
    }
}
