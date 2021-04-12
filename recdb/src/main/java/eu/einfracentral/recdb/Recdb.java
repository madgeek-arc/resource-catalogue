package eu.einfracentral.recdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Array;
import java.util.List;
import java.util.Objects;

@Configuration
@EnableScheduling
public class Recdb {

    private static final Logger logger = LogManager.getLogger(Recdb.class);
    private static final String serviceVisitsTemplate = "%s/index.php?module=API&method=%s&flat=1&idSite=%s&period=day&date=%s&format=JSON&token_auth=%s";
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
        String method= "Events.getCategory";
//        String method= "Events.getName&secondaryDimension=eventAction";
        String matomoSiteId = "2";
        String matomoDate = "yesterday";
//        String matomoDate = "today";
//        String matomoDate = "12-02-2021";
        String matomoToken = "d532d295158f72d9eca31823aa58750e";
        serviceEvents = String.format(serviceVisitsTemplate, matomoHost, method, matomoSiteId, matomoDate, matomoToken);
    }

    public String getMatomoResponse(String url) {

        try {
            HttpEntity<String> request = new HttpEntity<>(headers);
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

    @Autowired(required = true)
    @Qualifier("recdb.datasource")
    private DataSource datasource;

//    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Scheduled(cron = "0 0 1 * * *", zone = "Europe/Athens")
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

                System.out.println(user_id);

                /* Add service name if it is not in the table */
                query = "INSERT INTO services (service_name) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM services WHERE  service_name = ?)";
                jdbcTemplate.update(query, data[1], data[1]);
                /* Get service id */
                query = "SELECT service_pk FROM services WHERE service_name = ?;";
                service_id = jdbcTemplate.queryForObject(query, new Object[]{data[1]}, int.class);

                System.out.println(service_id);


                System.out.println("email: " + data[0] + " service id: " + data[1] + " value: " + event.sum_event_value);
                jdbcTemplate.update("UPDATE view_count set visits = visits + ? WHERE user_id = ? " +
                                "AND service_id = ?; INSERT INTO view_count (user_id, service_id, visits)" +
                                "SELECT ?,?,? WHERE NOT EXISTS (SELECT 1 FROM view_count WHERE" +
                                " user_id = ? AND service_id = ?);",
                        event.sum_event_value, user_id, service_id, user_id, service_id, event.sum_event_value, user_id, service_id);
            }
        }
    }
}
