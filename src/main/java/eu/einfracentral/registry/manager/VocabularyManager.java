package eu.einfracentral.registry.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.VocabularyService;
import java.io.IOException;
import java.net.*;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class VocabularyManager extends ResourceManager<Vocabulary> implements VocabularyService {
    private String[] euURLs = {"https://restcountries.eu/rest/v2/regionalbloc/EU?fields=alpha2Code"};
    private String[] eu;

    public VocabularyManager() {
        super(Vocabulary.class);
    }

    @Override
    public String getResourceType() {
        return "vocabulary";
    }

    @Override
    public String[] getEU() {
        if (eu == null || eu.length == 0) {
            fetchEU();
        }
        return eu;
    }

    @PostConstruct
    private void fetchEU() {
        //do not be fooled; this code only supports a specific type of rest data, and it's outside its scope for now to support any more
        Stream.of(euURLs).anyMatch(url -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setRequestMethod("GET");
                c.setRequestProperty("Accept", "application/json");
                if (c.getResponseCode() == 200) {
                    Country[] countries = new ObjectMapper().readValue(c.getInputStream(), Country[].class);
                    c.disconnect();
                    this.eu = Stream.of(countries).map(e -> e.alpha2Code).toArray(String[]::new);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return this.eu != null && this.eu.length > 0;
        });
    }

    private static class Country {
        private String alpha2Code;

        public String getAlpha2Code() { return alpha2Code; }

        public void setAlpha2Code(String alpha2Code) { this.alpha2Code = alpha2Code; }
    }
}
