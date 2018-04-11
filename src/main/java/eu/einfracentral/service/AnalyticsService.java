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
    @Value("${matomoToken:e235d94544916c326e80b713dd233cd1}")
    private String matomoToken;
    @Value("${fqdn:beta.einfracentral.eu}")
    private String fqdn;
    private String visits;

    AnalyticsService() {
        visits = String.format(
                "http://%s:8084/index.php?token_auth=%s&module=API&method=Actions.getPageUrls&format=JSON&idSite=1&period=day&flat=1&filter_limit=100&date=",
                fqdn,
                matomoToken);
    }

    public HashMap<String, Integer> getVisitsForLabel(String label) {
        HashMap<String, Integer> map = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            map = mapper.readValue(getAnalyticsForLabel(label), new TypeReference<Map<String, Integer>>() {});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public String getAnalyticsForLabel(String label) {
        StringBuilder ret = new StringBuilder();
        BufferedReader in = null;
        try {
            URL url = new URL("http://www.oracle.com/");
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                ret.append(inputLine).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret.toString();
    }
}