package eu.einfracentral.registry.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.VocabularyService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class VocabularyManager extends ResourceManager<Vocabulary> implements VocabularyService {
    private Map<String, Region> regions = new HashMap<>();

    private static Logger logger = LogManager.getLogger(VocabularyManager.class);

    public VocabularyManager() {
        super(Vocabulary.class);
        regions.put("EU", new Region("https://restcountries.eu/rest/v2/regionalbloc/EU?fields=alpha2Code"));
        regions.put("WW", new Region("https://restcountries.eu/rest/v2?fields=alpha2Code"));
    }

    @Override
    @Cacheable(value = "vocabularies")
    public Browsing<Vocabulary> getAll(FacetFilter ff, Authentication auth) {
        return super.getAll(ff, auth);
    }

    @Override
    @Cacheable(value = "vocabularies", key = "#id")
    public Vocabulary get(String id) {
        return super.get(id);
    }

    @Override
    @CacheEvict(value = "vocabularies", allEntries = true)
    public Vocabulary add(Vocabulary vocabulary, Authentication auth) {
        Vocabulary ret = super.add(vocabulary, auth);
        logger.info("Adding Vocabulary " + vocabulary);
        return ret;
    }

    @Override
    @CacheEvict(value = "vocabularies", allEntries = true)
    public Vocabulary update(Vocabulary vocabulary, Authentication auth) {
        Vocabulary ret = super.update(vocabulary, auth);
        logger.info("Updating Vocabulary " + vocabulary);
        return ret;
    }

    @Override
    @CacheEvict(value = "vocabularies", allEntries = true)
    public void delete(Vocabulary vocabulary) {
        super.delete(vocabulary);
        logger.info("Deleting Vocabulary " + vocabulary);
    }

    @Override
    public String getResourceType() {
        return "vocabulary";
    }

    @Override
    public String[] getRegion(String name) {
        Region region = regions.get(name);
        if (region.getMembers() == null || region.getMembers().length == 0) {
            fetchRegion(region);
        }
        return region.getMembers();
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
