package eu.einfracentral.recdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;

@Component
public class Recdb {

    private static final Logger logger = LogManager.getLogger(Recdb.class);
    private static final String SERVICE_VISITS_TEMPLATE = "%s/index.php?module=API&method=%s&flat=1&idSite=%s&period=day&date=%s&format=JSON&token_auth=%s";
    private String serviceEvents;

    private RestTemplate restTemplate;
    private HttpHeaders headers;

    @Value("${matomoHost:localhost}")
    private String matomoHost;

    @Value("${matomoToken:}")
    private String matomoToken;

    @Value("${matomoSiteId:1}")
    private String matomoSiteId;

    @Value("${matomoAuthorizationHeader:}")
    private String authorizationHeader;

    @PostConstruct
    void urlConstruct() {
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.add("Authorization", authorizationHeader);

        String method = "Events.getCategory";
//        String method= "Events.getName&secondaryDimension=eventAction";
        String matomoDate = "yesterday";
//        String matomoDate = "today";
        serviceEvents = String.format(SERVICE_VISITS_TEMPLATE, matomoHost, method, matomoSiteId, matomoDate, matomoToken);
    }

    public String getMatomoResponse(String url) {

        try {
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                logger.error("Could not retrieve analytics from matomo\nResponse Code: {}\nResponse Body: {}",
                        responseEntity.getStatusCode(), responseEntity.getBody());
            }
            return responseEntity.getBody();
        } catch (IllegalArgumentException e) {
            logger.info("URI is not absolute");
        } catch (RuntimeException e) {
            logger.error("Could not retrieve analytics from matomo", e);
        }

        return "";
    }

    @Autowired()
    @Qualifier("recdb.datasource")
    private DataSource datasource;

    //    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Scheduled(cron = "0 0 * * * *")
    public void getViews() throws IOException {
        urlConstruct();

        String str = getMatomoResponse(serviceEvents);
        ObjectMapper mapper = new ObjectMapper();

        logger.info("Display views \n");
        EventsModel[] day = mapper.readValue(str, EventsModel[].class);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
        int user_id = -1;
        int service_id = -1;
        String query = "";

        for (EventsModel event : day) {
            if (event.Events_EventCategory.equals("Recommendations")) {

                String[] data = event.Events_EventAction.split(" ");
                /* Add user email if it is not in the table */
                query = "INSERT INTO users (user_email) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM users WHERE  user_email = ?)";
                jdbcTemplate.update(query, data[0], data[0]);
                /* Get user id */
                query = "SELECT user_pk FROM users WHERE user_email = ?;";
                user_id = jdbcTemplate.queryForObject(query, new Object[]{data[0]}, int.class);

                logger.trace("User id: {}", user_id);

                /* Add service name if it is not in the table */
                query = "INSERT INTO services (service_name) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM services WHERE  service_name = ?)";
                jdbcTemplate.update(query, data[1], data[1]);
                /* Get service id */
                query = "SELECT service_pk FROM services WHERE service_name = ?;";
                service_id = jdbcTemplate.queryForObject(query, new Object[]{data[1]}, int.class);

                logger.trace("Service id: {}", service_id);
                logger.trace("email: {} service id: {} value: {}", data[0], data[1], event.sum_event_value);

                jdbcTemplate.update("UPDATE view_count set visits = visits + ? WHERE user_id = ? " +
                                "AND service_id = ?; INSERT INTO view_count (user_id, service_id, visits)" +
                                "SELECT ?,?,? WHERE NOT EXISTS (SELECT 1 FROM view_count WHERE" +
                                " user_id = ? AND service_id = ?);",
                        event.sum_event_value, user_id, service_id, user_id, service_id, event.sum_event_value, user_id, service_id);
            }
        }
    }
}
