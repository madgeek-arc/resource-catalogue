package eu.einfracentral.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 11/04/18.
 */
@Service("analyticsService")
public class AnalyticsService {
    @Autowired
    private ApplicationConfig config;
    private String visits;

    AnalyticsService() {
        visits = String.format(
                "http://%s:8084/index.php?token_auth=%s&module=API&method=Actions.getPageUrls&format=JSON&idSite=1&period=day&flat=1&filter_limit=100&date=",
                fqdn,
                matomoToken);
    }

    public Map<String, Integer> getVisitsForLabel(String label) {
        Map<String, Integer> ret = new HashMap<>();
        getAnalyticsForLabel(label).fields().forEachRemaining(dayStats -> {
            ret.put(dayStats.getKey(), dayStats.getValue().get(0) != null ? dayStats.getValue().get(0).path("nb_visits").asInt(0) : 0);
        });
        return ret;
    }

    private JsonNode getAnalyticsForLabel(String label) {
        String contents = getURL(String.format(visits, label));
        JsonNode ret = parse(contents);
        return ret;
    }

    private static String getURL(String url) {
        StringBuilder ret = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                ret.append(inputLine).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret.toString();
    }

    private static JsonNode parse(String json) {
        try {
            return new ObjectMapper(new JsonFactory()).readTree(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}