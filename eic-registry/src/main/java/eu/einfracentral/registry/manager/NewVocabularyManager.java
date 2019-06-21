package eu.einfracentral.registry.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.NewVocabulary;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.NewVocabularyService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class NewVocabularyManager extends ResourceManager<NewVocabulary> implements NewVocabularyService {
    private static final Logger logger = LogManager.getLogger(NewVocabularyManager.class);

    private Map<String, Region> regions = new HashMap<>();

    public NewVocabularyManager() {
        super(NewVocabulary.class);
        regions.put("EU", new Region("https://restcountries.eu/rest/v2/regionalbloc/EU?fields=alpha2Code"));
        regions.put("WW", new Region("https://restcountries.eu/rest/v2?fields=alpha2Code"));
    }

    @Override
    public String getResourceType() {
        return "new_vocabulary";
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
//    @Cacheable(value = CACHE_VOCABULARIES) // TODO enable them when old vocabularies are removed
    public Browsing<NewVocabulary> getAll(FacetFilter ff, Authentication auth) {
        return super.getAll(ff, auth);
    }

    @Override
//    @Cacheable(value = CACHE_VOCABULARIES)
    public List<NewVocabulary> getByType(NewVocabulary.Type type) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("type", type.getKey());
        return getAll(ff, null).getResults();
    }

    @Override
    public Map<String, NewVocabulary> getVocabulariesMap(FacetFilter ff) {
        return getAll(ff, null)
                .getResults()
                .stream()
                .collect(Collectors.toMap(NewVocabulary::getId, v-> v));
    }

    @Override
//    @CacheEvict(value = CACHE_VOCABULARIES, allEntries = true)
    public NewVocabulary add(NewVocabulary vocabulary, Authentication auth) {
        if (vocabulary.getId() == null || "".equals(vocabulary.getId())) {
            String id = vocabulary.getName().toLowerCase();
            id = id.replaceAll(" ", "_");
            id = id.replaceAll("&", "and");
            if (vocabulary.getParentId() != null) {
                id = String.format("%s-%s", vocabulary.getParentId().toLowerCase(), id);
            }
            vocabulary.setId(id);
        }
        if (exists(vocabulary)) {
            logger.error(String.format("%s already exists!%n%s", resourceType.getName(), vocabulary));
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }
        String serialized = serialize(vocabulary);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);
        logger.info("Adding Resource " + vocabulary);
        return vocabulary;
    }

    @Override
//    @CacheEvict(value = CACHE_VOCABULARIES, allEntries = true)
    public NewVocabulary update(NewVocabulary vocabulary, Authentication auth) {
        Resource existing = whereID(vocabulary.getId(), true);
        if (vocabulary.getId().startsWith("category-") || vocabulary.getId().startsWith("subcategory-")) { // TODO: remove
            String id = vocabulary.getName().toLowerCase();
            id = id.replaceAll(" ", "_");
            id = id.replaceAll("&", "and");
            if (vocabulary.getParentId() != null) {
                id = String.format("%s-%s", vocabulary.getParentId().toLowerCase(), id);
            }
            vocabulary.setId(id);
        }
        String serialized = serialize(vocabulary);
        serialized = serialized.replaceAll(":tns", "");
        serialized = serialized.replaceAll("tns:", "");
        existing.setPayload(serialized);
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.info("Updating Resource " + vocabulary);
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
                region.setMembers(Stream.of(countries).map(e -> e.alpha2Code).toArray(String[]::new));
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
        private String alpha2Code;

        public String getAlpha2Code() {
            return alpha2Code;
        }

        public void setAlpha2Code(String alpha2Code) {
            this.alpha2Code = alpha2Code;
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
