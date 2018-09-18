package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceMetadata;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.SearchService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@org.springframework.stereotype.Service("infraServiceService")
public class InfraServiceManager extends ServiceResourceManager implements InfraServiceService<InfraService, InfraService> {

    @Autowired
    private VocabularyManager vocabularyManager;

    @Autowired
    private ProviderManager providerManager;

    private Logger logger = Logger.getLogger(InfraServiceManager.class);

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
            migrate(infraService);
            String id = createServiceId(infraService);
            infraService.setId(id);
            logger.info("Created service with id: " + id);
            logger.info("Providers: " + infraService.getProviders());

            if (infraService.getServiceMetadata() == null) {
                ServiceMetadata serviceMetadata = createServiceMetadata(getUser(authentication)); //FIXME: get name from backend
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
    public Browsing<InfraService> getAll(FacetFilter ff, Authentication authentication) {
        return super.getAll(ff, authentication);
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
        validateServices(service);
        validateName(service);
        validateVocabularies(service);
        validateURL(service);
        validateDescription(service);
        validateSymbol(service);
        validateVersion(service);
        validateLastUpdate(service);
        validateOrder(service);
        validateSLA(service);
        validateProviders(service);
        validateMaxLength(service);
        return service;
    }

    public boolean userIsServiceProvider(String email, Service service) {
        Optional<List<String>> providers = Optional.of(service.getProviders());
        return providers
                .get()
                .stream()
                .map(id -> providerManager.get(id))
                .flatMap(x -> x.getUsers().stream().filter(Objects::nonNull))
                .anyMatch(x-> x.getEmail().equals(email));
    }

    //logic for migrating our data to release schema; can be a no-op when outside of migratory period
    private InfraService migrate(InfraService service) {
        return service;
    }

    //yes, this is foreign key logic right here on the application
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

    //validates the correctness of Service Name.
    private void validateName(InfraService service) throws Exception {
        if (service.getName() == null || service.getName().equals("")) {
            throw new Exception("field 'name' is obligatory");
        }
        //TODO: Core should check the max length
        if (service.getName().length() > 80){
            throw new Exception("max length for 'name' is 80 chars");
        }
    }

    //validates the correctness of Service URL.
    private void validateURL(InfraService service) throws Exception {
        if (service.getUrl() == null || service.getUrl().equals("")) {
            throw new Exception("field 'url' is mandatory");
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

    //validates the correctness of Service Description.
    private void validateDescription(InfraService service) throws Exception {
        if (service.getDescription() == null || service.getDescription().equals("")) {
            throw new Exception("field 'description' is mandatory");
        }
        //TODO: Core should check the max length
        if (service.getDescription().length() > 1000){
            throw new Exception("max length for 'description' is 1000 chars");
        }
    }

    //validates the correctness of Service Symbol.
    private void validateSymbol(InfraService service) throws Exception {
        if (service.getSymbol() == null || service.getSymbol().equals("")) {
            throw new Exception("field 'symbol' is mandatory");
        }
    }

    //validates the correctness of Service Version.
    private void validateVersion(InfraService service) throws Exception {
        if (service.getVersion() == null || service.getVersion().equals("")) {
            throw new Exception("field 'version' is mandatory");
        }
        //TODO: Core should check the max length
        if (service.getVersion().length() > 10){
            throw new Exception("max length for 'version' is 10 chars");
        }
    }

    //validates the correctness of Service Last Update (Revision Date).
    private void validateLastUpdate(InfraService service) throws Exception {
        if (service.getLastUpdate() == null || service.getLastUpdate().equals("")) {
            throw new Exception("field 'Revision Date' (lastUpdate) is mandatory");
        }
    }

    //validates the correctness of URL for requesting the service from the service providers.
    private void validateOrder(InfraService service) throws Exception {
        if (service.getOrder() == null || service.getOrder().equals("")) {
            throw new Exception("field 'order' is mandatory");
        }
    }

    //validates the correctness of Service SLA.
    private void validateSLA(InfraService service) throws Exception {
        if (service.getServiceLevelAgreement() == null || service.getServiceLevelAgreement().equals("")) {
            throw new Exception("field 'serviceLevelAgreement' is mandatory");
        }
    }


    //validates the correctness of Related and Required Services.
    private void validateServices(InfraService service) throws Exception {
        List<String> relatedServices = service.getRelatedServices();
        List<String> existingRelatedServices = new ArrayList<>();
        for (String serviceRel : relatedServices) {
            //logger.info("Inside loop relatedServices: " + serviceRel);
            if (this.exists(new SearchService.KeyValue("infra_service_id", serviceRel))) {
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

                    new SearchService.KeyValue("infra_service_id", serviceReq))) {
                existingRequiredServices.add(serviceReq);
            }
        }
        service.setRequiredServices(existingRequiredServices);

        //logger.info(infraService.toString());
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

    //validates the max length of various variables.
    //FIXME: Core should check the max length
    private InfraService validateMaxLength(InfraService service) throws Exception {
        if (service.getTagline().length() > 100){
            throw new Exception("max length for 'tagline' is 100 chars");
        }
        if (service.getOptions().length() > 1000){
            throw new Exception("max length for 'options' is 1000 chars");
        }
        if (service.getTargetUsers().length() > 1000){
            throw new Exception("max length for 'targetUsers' is 1000 chars");
        }
        if (service.getUserValue().length() > 1000){
            throw new Exception("max length for 'userValue' is 1000 chars");
        }
        if (service.getUserBase().length() > 1000){
            throw new Exception("max length for 'userBase' is 1000 chars");
        }
        if (service.getChangeLog().length() > 1000){
            throw new Exception("max length for 'changeLog' is 1000 chars");
        }
        if (service.getFunding().length() > 500){
            throw new Exception("max length for 'funding' is 500 chars");
        }
        return service;
    }
}
