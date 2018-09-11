//package eu.einfracentral.registry.manager;
//
//import eu.einfracentral.core.ParserPool;
//import eu.einfracentral.domain.Addenda;
//import eu.einfracentral.domain.Service;
//import eu.einfracentral.exception.ResourceException;
//import eu.einfracentral.registry.service.ResourceService;
//import eu.openminted.registry.core.domain.Resource;
//import eu.openminted.registry.core.service.SearchService;
//import org.apache.log4j.Logger;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Component;
//
//@Component
//public class ServiceManager extends ResourceManager<Service> implements ResourceService<Service> {
////public class ServiceManager extends ServiceResourceManager implements InfraServiceService {
//
//    @Autowired
//    private VocabularyManager vocabularyManager;
//
//    @Autowired
//    private SearchService searchService;
//
//    @Autowired
//    ParserPool parserPool;
//
//    private Logger logger = Logger.getLogger(ServiceManager.class);
//
//    public ServiceManager() {
//        super(Service.class);
//    }
//
//    @Override
//    public String getResourceType() {
//        return "infra_service";
//    }
//
//    @Override
//    public Service add(Service service) {
//        migrate(service);
//
//        if (service.getId() == null) {
//            String id = createServiceId(service);
//            service.setId(id);
//            logger.info("Providers: " + service.getProviders());
//
//            logger.info("Created service with id: " + id);
//        }
////        if (!service.getId().contains(".")) {
////            service.setId(java.util.UUID.randomUUID().toString());
////        }
//        if (exists(service)) {
//            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
//        }
//        ensureAddenda(service.getId()); //using ensure instead of add here, in case we were populated via a DB transfer
//        return super.add(validate(service));
//    }
//
//    @Override
//    public Service update(Service service) {
//        service = validate(service);
//        Service existingService = get(service.getId());
//        fixVersion(existingService); //remove this when it has ran for all services
//        updateAddenda(service.getId());
//        Service ret;
//        if (service.getVersion().equals(existingService.getVersion())) {
//            ret = super.update(service);
//        } else {
//            Resource existingResource = whereID(service.getId(), false);
//            existingService.setId(String.format("%s/%s", existingService.getId(), existingService.getVersion()));
//            existingResource.setPayload(serialize(existingService));
//            resourceService.updateResource(existingResource);
//            ret = add(service);
//        }
//        return ret;
//    }
//
//    @Override
//    public Service validate(Service service) {
//        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
//        //just check if validateVocabularies did anything or not
//        return validateVocabularies(fixVersion(service));
//    }
//
//    //logic for migrating our data to release schema; can be a no-op when outside of migratory period
//    private Service migrate(Service service) {
//        return service;
//    }
//
//    //yes, this is foreign key logic right here on the application
//    private Service validateVocabularies(Service service) {
////        Map<String, List<String>> validVocabularies = vocabularyManager.getBy("type").entrySet().stream().collect(
////                Collectors.toMap(
////                        Map.Entry::getKey,
////                        entry -> entry.getValue().stream().map(Vocabulary::getId).collect(Collectors.toList())
////                )
////        );
//        //logic for invalidating data based on whether or not they comply with existing ids
//        if (!vocabularyManager.exists(
//                new SearchService.KeyValue("type", "Category"),
//                new SearchService.KeyValue("name", service.getCategory()))) {
//            service.setCategory(null);
//        }
//        if (!vocabularyManager.exists(
//                new SearchService.KeyValue("type", "Subcategory"),
//                new SearchService.KeyValue("name", service.getSubcategory()))) {
//            service.setSubcategory(null);
//        }
//        if (service.getPlaces() != null) {
//            if (!service.getPlaces().parallelStream().allMatch(place -> vocabularyManager.exists(
//                    new SearchService.KeyValue("type", "Place"),
//                    new SearchService.KeyValue("vocabulary_id", place)))) {
//                service.setPlaces(null);
//            }
//        }
//        if (service.getLanguages() != null) {
//            if (!service.getLanguages().parallelStream().allMatch(lang -> vocabularyManager.exists(
//                    new SearchService.KeyValue("type", "Language"),
//                    new SearchService.KeyValue("vocabulary_id", lang)))) {
//                service.setLanguages(null);
//            }
//        }
//        if (!vocabularyManager.exists(
//                new SearchService.KeyValue("type", "LifeCycleStatus"),
//                new SearchService.KeyValue("vocabulary_id", service.getLifeCycleStatus()))) {
//            service.setLifeCycleStatus(null);
//        }
//        if (!vocabularyManager.exists(
//                new SearchService.KeyValue("type", "TRL"),
//                new SearchService.KeyValue("vocabulary_id", service.getTrl()))) {
//            service.setTrl(null);
//        }
//        return service;
//    }
//
//    private Addenda updateAddenda(String id) {
////        try {
////            Addenda ret = ensureAddenda(id);
////            ret.setModifiedAt(System.currentTimeMillis());
////            ret.setModifiedBy("pgl"); //get actual username somehow
////            return addendaManager.update(ret);
////        } catch (Throwable e) {
////            e.printStackTrace();
////            return null; //addenda are thoroughly optional, and should not interfere with normal add/update operations
////        }
//        return null; // TODO remove
//    }
//
//    private Addenda ensureAddenda(String id) {
////        try {
////            return parserPool.deserialize(addendaManager.where("service", id, true), Addenda.class).get();
////        } catch (InterruptedException | ExecutionException | ResourceException e) {
////            e.printStackTrace();
////            return addAddenda(id);
////        }
//        return null; // TODO remove
//    }
//
//    private Addenda addAddenda(String id) {
////        try {
////            Addenda ret = new Addenda();
////            ret.setId(UUID.randomUUID().toString());
////            ret.setService(id);
////            ret.setRegisteredBy("pgl"); //get actual username somehow
////            ret.setRegisteredAt(System.currentTimeMillis());
////            return addendaManager.add(ret);
////        } catch (Throwable e) {
////            e.printStackTrace();
////            return null; //addenda are thoroughly optional, and should not interfere with normal add/update operations
////        }
//        return null; // TODO remove
//    }
//
//    private Service fixVersion(Service service) {
//        if (service.getVersion() == null || service.getVersion().equals("")) {
//            service.setVersion("0");
//        }
//        return service;
//    }
//
//    private String createServiceId(Service service) {
//        String id = "";
//        String provider = service.getEditorName();
////        List<String> providers = service.getProviders();
//
////        FacetFilter facetFilter = new FacetFilter();
////        facetFilter.setResourceType("service");
////        facetFilter.setKeyword("field");
//
////        try {
////            List<Resource> services = searchService.search(facetFilter).getResults();
//
////        List<Resource> services = searchService.cqlQuery("provider="+provider, "service", 1000, 0,
////                    "modification_date", SortOrder.DESC).getResults();
////        if (providers.size() > 0) {
////            provider = providers.get(0) + ".";
////                provider = "";
////                Collections.sort(providers);
////                for (String prov : providers) {
////                    provider += (prov + ".");
////                }
////        }
////        id = String.format("%s%02d", provider, services.size()+1);
//
////        } catch (UnknownHostException e) {
////            e.printStackTrace();
////        }
//
//        id = String.format("%s.%s", provider, service.getName().replaceAll(" ", "_").toLowerCase());
//        return id;
//    }
//}
