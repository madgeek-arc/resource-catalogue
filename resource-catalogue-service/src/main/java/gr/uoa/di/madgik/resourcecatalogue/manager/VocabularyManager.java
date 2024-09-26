package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.dto.VocabularyTree;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VocabularyManager extends ResourceManager<Vocabulary> implements VocabularyService {
    private static final Logger logger = LoggerFactory.getLogger(VocabularyManager.class);

    private final ProviderManager providerManager;

    private final SecurityService securityService;

    private final IdCreator idCreator;

    public VocabularyManager(@Lazy ProviderManager providerManager, @Lazy IdCreator idCreator, @Lazy SecurityService securityService) {
        super(Vocabulary.class);
        this.providerManager = providerManager;
        this.idCreator = idCreator;
        this.securityService = securityService;
    }

    @Override
    public String getResourceType() {
        return "vocabulary";
    }

    @Override
    public Vocabulary getOrElseThrow(String id) {
        Vocabulary vocabulary = null;
        try {
            vocabulary = get(id);
        } catch (ResourceException e) {
            throw new ResourceException(String.format("Vocabulary with id '%s' does not exist!", id), HttpStatus.NOT_FOUND);
        }
        return vocabulary;
    }

    @Override
    public String[] getRegion(String name) {
        List<Vocabulary> allCountries = getByType(Vocabulary.Type.COUNTRY);
        if (name.equals("WW")) {
            return allCountries.stream().map(Vocabulary::getId).toArray(String[]::new);
        } else {
            return allCountries.stream()
                    .filter(vocabulary -> vocabulary.getExtras().containsKey("region"))
                    .filter(vocabulary -> vocabulary.getExtras().get("region").equals(name))
                    .map(Vocabulary::getId)
                    .toArray(String[]::new);
        }
    }

    @Override
    public Vocabulary getParent(String id) {
        return get(get(id).getParentId());
    }

    @Override
    public Browsing<Vocabulary> getAll(FacetFilter ff, Authentication auth) {
        return super.getAll(ff, auth);
    }

    @Override
    public Map<Vocabulary.Type, List<Vocabulary>> getAllVocabulariesByType() {
        Map<Vocabulary.Type, List<Vocabulary>> allVocabularies = new HashMap<>();
        for (Vocabulary.Type type : Vocabulary.Type.values()) {
            allVocabularies.put(type, getByType(type));
        }
        return allVocabularies;
    }

    @Override
    public List<Vocabulary> getByType(Vocabulary.Type type) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("type", type.getKey());
        List<Vocabulary> vocList = getAll(ff, null).getResults();
        return vocList.stream().sorted(Comparator.comparing(Vocabulary::getId)).collect(Collectors.toList());
    }

    @Override
    public Map<String, Vocabulary> getVocabulariesMap() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        return getAll(ff, null)
                .getResults()
                .stream()
                .collect(Collectors.toMap(Vocabulary::getId, v -> v));
    }

    @Override
    public void addBulk(List<Vocabulary> vocabularies, Authentication auth) {
        super.addBulk(vocabularies, auth);
    }

    @Override
    public void updateBulk(List<Vocabulary> vocabularies, Authentication auth) {
        for (Vocabulary vocabulary : vocabularies) {
            update(vocabulary, auth);
        }
    }

    @Override
    public void deleteAll(Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<Vocabulary> allVocs = getAll(ff, auth).getResults();
        for (Vocabulary vocabulary : allVocs) {
            delete(vocabulary);
        }
    }

    @Override
    public VocabularyTree getVocabulariesTree(Vocabulary.Type type) { // TODO: refactor method
        VocabularyTree root = new VocabularyTree();
        root.setVocabulary(null);
        Map<String, List<Vocabulary>> vocabularies = getBy("parent_id");
        List<VocabularyTree> superTreeList = new ArrayList<>();
        List<Vocabulary> superVocabularies = getByType(type);
        if (superVocabularies != null) {
            for (Vocabulary superVocabulary : superVocabularies) {
                VocabularyTree superTree = new VocabularyTree();
                superTree.setVocabulary(superVocabulary);
                List<VocabularyTree> treeList = new ArrayList<>();
                List<Vocabulary> vocs = vocabularies.get(superVocabulary.getId());
                if (vocs != null) {
                    for (Vocabulary voc : vocs) {
                        VocabularyTree tree = new VocabularyTree();
                        tree.setVocabulary(voc);
                        List<VocabularyTree> subTreeList = new ArrayList<>();
                        List<Vocabulary> subVocabularies = vocabularies.get(voc.getId());
                        if (subVocabularies != null) {
                            for (Vocabulary subVocabulary : subVocabularies) {
                                VocabularyTree subTree = new VocabularyTree();
                                subTree.setVocabulary(subVocabulary);
//                    subTree.setChildren(null);
                                subTreeList.add(subTree);
                            }
                        }
                        tree.setChildren(subTreeList);
                        treeList.add(tree);
                    }
                }
                superTree.setChildren(treeList);
                superTreeList.add(superTree);
            }
        }
        root.setChildren(superTreeList);
        return root;
    }

    @Override
    public Vocabulary add(Vocabulary vocabulary, Authentication auth) {
        if (vocabulary.getId() == null || "".equals(vocabulary.getId())) {
            String id = vocabulary.getName().toLowerCase();
            id = id.replace(" ", "_");
            id = id.replace("&", "and");
            if (vocabulary.getParentId() != null) {
                id = String.format("%s-%s", vocabulary.getParentId().toLowerCase(), id);
            }
            vocabulary.setId(id);
        }
        if (exists(vocabulary)) {
            throw new ResourceAlreadyExistsException(String.format("%s already exists!%n%s", resourceType.getName(), vocabulary));
        }
        String serialized = serialize(vocabulary);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);
        logger.debug("Adding Vocabulary {}", vocabulary);
        return vocabulary;
    }

    @Override
    public Vocabulary update(Vocabulary vocabulary, Authentication auth) {
        Resource existing = whereID(vocabulary.getId(), true);
        String serialized = serialize(vocabulary);
        serialized = serialized.replace(":tns", "");
        serialized = serialized.replace("tns:", "");
        existing.setPayload(serialized);
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Vocabulary {}", vocabulary);
        return vocabulary;
    }

    //    @Scheduled(initialDelay = 0, fixedRate = 120000)
//    @Scheduled(cron = "0 0 12 ? * 2/7") // At 12:00:00pm, every 7 days starting on Monday, every month
    public void updateHostingLegalEntityVocabularyList() {
        logger.info("Checking for possible new Hosting Legal Entity entries..");
        List<Vocabulary> hostingLegalEntities = getByType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY);
        List<String> hostingLegalEntityNames = new ArrayList<>();
        for (Vocabulary hostingLegalEntity : hostingLegalEntities) {
            hostingLegalEntityNames.add(hostingLegalEntity.getName());
        }
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved provider");
        ff.addFilter("published", false);
        List<ProviderBundle> allActiveAndApprovedProviders = providerManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<String> providerNames = new ArrayList<>();
        for (ProviderBundle providerBundle : allActiveAndApprovedProviders) {
            if (providerBundle.getProvider().isLegalEntity()) {
                providerNames.add(providerBundle.getProvider().getName());
            }
        }
        for (Iterator<String> it = providerNames.iterator(); it.hasNext(); ) {
            String providerName = it.next();
            for (String hleName : hostingLegalEntityNames) {
                if (hleName.contains(providerName)) {
                    it.remove();
                    break;
                }
            }
        }
        updateHLEVocabularyList(providerNames);
    }

    private void updateHLEVocabularyList(List<String> providerNames) {
        for (String newHLE : providerNames) {
            Vocabulary newHostingLegalEntity = new Vocabulary();
            newHostingLegalEntity.setId(idCreator.sanitizeString(newHLE));
            newHostingLegalEntity.setName(newHLE);
            newHostingLegalEntity.setType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY.getKey());
            //TODO: Also add catalogueId as extras if this method gets activated
            logger.info(String.format("Creating a new Hosting Legal Entity Vocabulary with id: [%s] and name: [%s]",
                    newHostingLegalEntity.getId(), newHostingLegalEntity.getName()));
//                        add(newHostingLegalEntity, null);
        }
    }

}
