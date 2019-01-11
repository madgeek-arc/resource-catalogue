package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.utils.ObjectUtils;
import eu.einfracentral.utils.ServiceValidators;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.service.SearchService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@org.springframework.stereotype.Service("infraServiceService")
public class InfraServiceManager extends ServiceResourceManager implements InfraServiceService<InfraService, InfraService> {

    private VocabularyManager vocabularyManager;
    private ProviderManager providerManager;

    private static final Logger logger = LogManager.getLogger(InfraServiceManager.class);

    @Autowired
    public InfraServiceManager(VocabularyManager vocabularyManager, ProviderManager providerManager) {
        super(InfraService.class);
        this.vocabularyManager = vocabularyManager;
        this.providerManager = providerManager;
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
            String id = createServiceId(infraService);
            infraService.setLatest(true);
            infraService.setId(id);
            logger.info("Created service with id: " + id);
            logger.info("Providers: " + infraService.getProviders());

            if (infraService.getServiceMetadata() == null) {
                ServiceMetadata serviceMetadata = createServiceMetadata(new User(authentication).getFullName());
                infraService.setServiceMetadata(serviceMetadata);
            }

            ret = super.add(infraService, authentication);

            if (providerManager.getServices(infraService.getProviders().get(0)).size() == 1) { // user just added the service
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

        if (infraService.getVersion().equals(existingService.getVersion())) {
            // update existing service serviceMetadata
            ServiceMetadata serviceMetadata = updateServiceMetadata(existingService.getServiceMetadata(), new User(authentication).getFullName());
            infraService.setServiceMetadata(serviceMetadata);
//                ObjectUtils.merge(existingService, infraService); // FIXME: this method does not assign values of Superclass
            infraService.setActive(existingService.isActive());
            infraService.setLatest(existingService.isLatest());
            infraService.setStatus(existingService.getStatus());
            ret = super.update(infraService, authentication);

        } else {
            // set previous version not latest
            existingService.setLatest(false);
            super.update(existingService, authentication);

//                infraService.setStatus(); // TODO: enable this when services support the Status field
            ret = add(infraService, authentication);
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
//        service.setActive(true);
//        service.setOrder(service.getRequests() != null ? service.getRequests() : service.getUrl());
//
//        //Change Service's trl according to the new vocabularies
//        String trl = service.getTrl();
//        service.setTrl(trl.toLowerCase());
//
//        //Change Service's lifeCycleStatus according to the new vocabularies
//        String lifecyclestatus = service.getLifeCycleStatus();
//        String[] lfc = lifecyclestatus.split("-");
//        String lfc2 = lfc.length == 2 ? lfc[1] : lfc[0];
//        service.setLifeCycleStatus(lfc2.toLowerCase());
//
//        //Change Service's categories according to the new vocabularies
//        String category = service.getCategory();
//        String [] ctg = category.split("-");
//        String ctg2 = ctg.length == 2 ? ctg[1] : ctg[0];
//        service.setCategory(ctg2.toLowerCase());
//
//        //Change Service's subcategories according to the new vocabularies
//        String subcategory = service.getSubcategory();
//        subcategory = subcategory.replace("Subcategory-","");
//        service.setSubcategory(subcategory.toLowerCase());
//
//        //Change Service's places according to the new vocabularies
//        List<String> places = service.getPlaces();
//        List<String> placesNew = new ArrayList<>();
//        for (int i=0; i<places.size(); i++){
//            if (places.get(i).contains("-")){
//                String[] pl = places.get(i).split("-");
//                String pl2 = pl[1];
//                if (pl2.equals("WW")){
//                    pl2 = "world";
//                } else {
//                    pl2 = "europe";
//                }
//                placesNew.add(pl2);
//            }
//        }
//        service.setPlaces(placesNew);
//
//        //Change Service's languages according to the new vocabularies
//        List<String> languages = service.getLanguages();
//        List<String> languagesNew = new ArrayList<>();
//        if (languages.size() == 1){
//            languagesNew.add("english");
//        } else {
//            languagesNew.add("english");
//            languagesNew.add("french");
//            languagesNew.add("danish");
//            languagesNew.add("bulgarian");
//            languagesNew.add("german");
//            languagesNew.add("greek");
//            languagesNew.add("albanian");
//        }
//        service.setLanguages(languagesNew);
//
//
//        String id = service.getId();
//        if (id.equals("egi.egi_marketplace")){
//            List<String> languagesNew = new ArrayList<>();
//            languagesNew.add("english");
//            service.setLanguages(languagesNew);
//            service.setSubcategory("tools");
//            service.setCategory("operations");
//            List<String> placesNew = new ArrayList<>();
//            placesNew.add("europe");
//            service.setPlaces(placesNew);
//            service.setLifeCycleStatus("production");
//            service.setTrl("trl-9");
//        }


//        if (service.getTags() == null) {
//            List<String> tags = new ArrayList<>();
//            service.setTags(tags);
//        }
//        if (service.getTargetUsers() == null) {
//            service.setTargetUsers("-");
//        }
//        if (service.getSymbol() == null) {
//            service.setSymbol(new URL("http://fvtelibrary.com/img/user/NoLogo.png"));
//        }
//        if (service.getLastUpdate() == null) {
//            GregorianCalendar date = new GregorianCalendar(2000, 1, 1);
//            service.setLastUpdate(DatatypeFactory.newInstance().newXMLGregorianCalendar(date));
//        }
//        if (service.getVersion() == null) {
//            service.setVersion("0");
//        } else {
//            service.setVersion(service.getVersion());
//        }

//        return service;
//    }

    private ServiceMetadata updateServiceMetadata(ServiceMetadata serviceMetadata, String modifiedBy) {
        ServiceMetadata ret;
        if (serviceMetadata == null) {
            ret = createServiceMetadata(modifiedBy);
        } else {
            ret = serviceMetadata;
        }
        ret.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        ret.setModifiedBy(modifiedBy);
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
        return String.format("%s.%s", provider, StringUtils
                .stripAccents(service.getName())
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replaceAll(" ", "_")
                .toLowerCase());
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
            for (String place : servicePlaces) {
                VocabularyEntry placeFound = places.get(place);
                if (placeFound == null) {
                    notFoundPlaces.add(place);
                }
            }
            if (!notFoundPlaces.isEmpty()) {
                throw new ValidationException(String.format("Places not found: %s", String.join(", ", notFoundPlaces)));
            }
        } else throw new ValidationException("Field 'places' is mandatory.");

        //Validate Languages
        if (service.getLanguages() != null && !service.getLanguages().isEmpty()) {
            Map<String, VocabularyEntry> languages = vocabularyManager.get("languages").getEntries();
            List<String> serviceLanguages = service.getLanguages();
            List<String> notFoundLanguages = new ArrayList<>();
            for (String language : serviceLanguages) {
                VocabularyEntry languageFound = languages.get(language);
                if (languageFound == null) {
                    notFoundLanguages.add(language);
                }
            }
            if (!notFoundLanguages.isEmpty()) {
                throw new ValidationException(String.format("Languages not found: %s", String.join(", ", notFoundLanguages)));
            }
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
        if ((providers == null) || CollectionUtils.isEmpty(service.getProviders()) ||
                (service.getProviders().stream().filter(Objects::nonNull).mapToInt(p -> 1).sum() == 0)) {
            throw new ValidationException("field 'providers' is obligatory");
        }
        if (service.getProviders().stream().filter(Objects::nonNull).anyMatch(x -> providerManager.getResource(x) == null)) {
            throw new ValidationException("Provider does not exist");
        }
    }

    //validates the correctness of Related and Required Services.
    public void validateServices(InfraService service) {
        List<String> relatedServices = service.getRelatedServices();
        List<String> existingRelatedServices = new ArrayList<>();
        if (relatedServices != null) {
            for (String serviceRel : relatedServices) {
                if (this.exists(new SearchService.KeyValue("infra_service_id", serviceRel))) {
                    existingRelatedServices.add(serviceRel);
                }
            }
            service.setRelatedServices(existingRelatedServices);
        }

        List<String> requiredServices = service.getRequiredServices();
        List<String> existingRequiredServices = new ArrayList<>();
        if (requiredServices != null) {
            for (String serviceReq : requiredServices) {
                if (this.exists(

                        new SearchService.KeyValue("infra_service_id", serviceReq))) {
                    existingRequiredServices.add(serviceReq);
                }
            }
            service.setRequiredServices(existingRequiredServices);
        }
    }
}
