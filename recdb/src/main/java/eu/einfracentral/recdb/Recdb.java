package eu.einfracentral.recdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Configuration
@EnableScheduling
public class Recdb {

    private static final Logger logger = LogManager.getLogger(Recdb.class);
    private static final String serviceVisitsTemplate = "%s/index.php?module=API&method=Events.getName&secondaryDimension=eventAction&flat=1&idSite=%s&period=day&date=%s&format=JSON&token_auth=%s";
    private String serviceEvents;

    private RestTemplate restTemplate;
    private HttpHeaders headers;

    @PostConstruct
    void urlConstruct() {
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        String authorizationHeader = "";
        headers.add("Authorization", authorizationHeader);
        String matomoHost = "https://www.portal.catris.eu/matomo";
        String matomoSiteId = "2";
        String matomoDate = "yesterday";
//        String matomoDate = "29-01-2021";
        String matomoToken = "d532d295158f72d9eca31823aa58750e";
        serviceEvents = String.format(serviceVisitsTemplate, matomoHost, matomoSiteId, matomoDate, matomoToken);
    }

    public String getMatomoResponse(String url) {

        try {
            HttpEntity<String> request = new HttpEntity<String>(headers);
            try {
                ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
                if (responseEntity.getStatusCode() != HttpStatus.OK) {
                    logger.error("Could not retrieve analytics from matomo\nResponse Code: {}\nResponse Body: {}",
                            responseEntity.getStatusCode().toString(), responseEntity.getBody());
                }
                return responseEntity.getBody();
            } catch (IllegalArgumentException e) {
                logger.info("URI is not absolute");
            }
        } catch (RuntimeException e) {
            logger.error("Could not retrieve analytics from matomo", e);
        }
        return "";
    }

    @Scheduled(fixedDelay = 60 * 1000)
    public void getViews() throws IOException {
        urlConstruct();

        String str = getMatomoResponse(serviceEvents);
        ObjectMapper mapper = new ObjectMapper();

        EventsModel[] day = mapper.readValue(str, EventsModel[].class);
        for (EventsModel event : day) {
            System.out.println(event.label);
            System.out.println(event.Events_EventName);
            System.out.println(event.Events_EventAction);
        }
    }
}
