package eu.einfracentral.registry.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.dto.VocabularyTree;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.VocabularyService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.einfracentral.config.CacheConfig.*;

@Service
public class VocabularyManager extends ResourceManager<Vocabulary> implements VocabularyService {
    private static final Logger logger = LogManager.getLogger(VocabularyManager.class);

    private Map<String, Region> regions = new HashMap<>();

    public VocabularyManager() {
        super(Vocabulary.class);
        regions.put("EU", new Region("https://restcountries.com/v3.1/region/europe?fields=cca2"));
        regions.put("WW", new Region("https://restcountries.com/v3.1/all?fields=cca2"));
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
        Region region = regions.get(name);
        if (region.getMembers() == null || region.getMembers().length == 0) {
            fetchRegion(region);
        }
        return region.getMembers();
    }

    @Override
    public Vocabulary getParent(String id) {
        return get(get(id).getParentId());
    }

    @Override
    @Cacheable(value = CACHE_VOCABULARIES)
    public Browsing<Vocabulary> getAll(FacetFilter ff, Authentication auth) {
        return super.getAll(ff, auth);
    }

    @Override
    @Cacheable(value = CACHE_VOCABULARIES)
    public Map<Vocabulary.Type, List<Vocabulary>> getAllVocabulariesByType() {
        Map<Vocabulary.Type, List<Vocabulary>> allVocabularies = new HashMap<>();
        for (Vocabulary.Type type : Vocabulary.Type.values()) {
            allVocabularies.put(type, getByType(type));
        }
        return allVocabularies;
    }

    @Override
    @Cacheable(value = CACHE_VOCABULARIES)
    public List<Vocabulary> getByType(Vocabulary.Type type) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("type", type.getKey());
        List<Vocabulary> vocList = getAll(ff, null).getResults();
        return vocList.stream().sorted(Comparator.comparing(Vocabulary::getId)).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = CACHE_VOCABULARY_MAP)
    public Map<String, Vocabulary> getVocabulariesMap() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        return getAll(ff, null)
                .getResults()
                .stream()
                .collect(Collectors.toMap(Vocabulary::getId, v -> v));
    }

    @Override
    @Cacheable(value = CACHE_VOCABULARY_MAP)
    public Map<String, Vocabulary> getVocabulariesMap(FacetFilter ff) {
        return getAll(ff, null)
                .getResults()
                .stream()
                .collect(Collectors.toMap(Vocabulary::getId, v -> v));
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @CacheEvict(value = {CACHE_VOCABULARIES, CACHE_VOCABULARY_MAP, CACHE_VOCABULARY_TREE}, allEntries = true)
    public void addAll(List<Vocabulary> vocabularies, Authentication auth) {
        for (Vocabulary vocabulary : vocabularies) {
            add(vocabulary, auth);
        }
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @CacheEvict(value = {CACHE_VOCABULARIES, CACHE_VOCABULARY_MAP, CACHE_VOCABULARY_TREE}, allEntries = true)
    public void deleteAll(Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<Vocabulary> allVocs = getAll(ff, auth).getResults();
        for (Vocabulary vocabulary : allVocs) {
            delete(vocabulary);
        }
    }

    @Override
    @Cacheable(value = CACHE_VOCABULARY_TREE)
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
    @CacheEvict(value = {CACHE_VOCABULARIES, CACHE_VOCABULARY_MAP, CACHE_VOCABULARY_TREE}, allEntries = true)
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
            logger.error("{} already exists!\n{}", resourceType.getName(), vocabulary);
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
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
    @CacheEvict(value = {CACHE_VOCABULARIES, CACHE_VOCABULARY_MAP, CACHE_VOCABULARY_TREE}, allEntries = true)
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

    private void fetchRegion(Region region) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(region.getSource()).openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Accept", "application/json");
            if (c.getResponseCode() == 200) {
                Country[] countries = new ObjectMapper().readValue(c.getInputStream(), Country[].class);
                c.disconnect();
                region.setMembers(Stream.of(countries).map(e -> e.cca2).toArray(String[]::new));
            }
        } catch (IOException e) {
            logger.error("ERROR", e);
        }
    }

    @PostConstruct
    private void postConstruct() {
        regions.forEach((key, value) -> this.fetchRegion(value));
    }

    private static class Country {
        private String cca2;

        public String getCca2() {
            return cca2;
        }

        public void setCca2(String cca2) {
            this.cca2 = cca2;
        }
    }

    private static class Region {
        private String[] members = null;
        private String source;

        private Region(String source) {
            this.source = source;
        }

        public String[] getMembers() {
            return members;
        }

        public void setMembers(String[] members) {
            this.members = members;
        }

        public String getSource() {
            return source;
        }
    }
}
