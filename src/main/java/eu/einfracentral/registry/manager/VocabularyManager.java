package eu.einfracentral.registry.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.VocabularyService;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class VocabularyManager extends ResourceManager<Vocabulary> implements VocabularyService {
    private Map<String, Region> regions = new HashMap<>();

    private static Logger logger = Logger.getLogger(VocabularyManager.class);

    public VocabularyManager() {
        super(Vocabulary.class);
        regions.put("EU", new Region("https://restcountries.eu/rest/v2/regionalbloc/EU?fields=alpha2Code"));
        regions.put("WW", new Region("https://restcountries.eu/rest/v2?fields=alpha2Code"));
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

    @Override
    public boolean exists(SearchService.KeyValue... ids) {
        Resource resource;
        try {
            resource = this.searchService.searchId(getResourceType(), ids);
            return resource != null;
        } catch (UnknownHostException e) {
            logger.error(e);
            throw new ServiceException(e);
        }
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
            e.printStackTrace();
        }
    }

    @PostConstruct
    private void postConstruct() {
        regions.forEach((key, value) -> this.fetchRegion(value));
    }

    private static class Country {
        private String alpha2Code;

        public String getAlpha2Code() { return alpha2Code; }

        public void setAlpha2Code(String alpha2Code) { this.alpha2Code = alpha2Code; }
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
