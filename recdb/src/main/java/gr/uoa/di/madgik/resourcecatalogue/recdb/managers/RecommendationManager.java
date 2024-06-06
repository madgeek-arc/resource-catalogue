package gr.uoa.di.madgik.resourcecatalogue.recdb.managers;

import gr.uoa.di.madgik.resourcecatalogue.domain.Service;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.recdb.services.RecommendationService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RecommendationManager implements RecommendationService<Service, Authentication> {

    private static final Logger logger = LogManager.getLogger(RecommendationManager.class);
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final DataSource recdbDataSource;

    @Autowired
    public RecommendationManager(ServiceBundleService<ServiceBundle> serviceBundleService,
                                 @Qualifier("recdbDataSource") DataSource recdbDataSource) {
        this.serviceBundleService = serviceBundleService;
        this.recdbDataSource = recdbDataSource;
    }

    public ResponseEntity<List<Service>> getRecommendedResources(int limit, Authentication authentication) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(recdbDataSource);
        List<Service> services = new ArrayList<>();

        /* Get user id */
        int user_id = -1;
        String query = "SELECT user_pk FROM users WHERE user_email = ?;";


        User user = User.of(authentication);
        try {
            user_id = jdbcTemplate.queryForObject(query, new Object[]{user.getEmail()}, int.class);

            // TODO: get recommendations for non authenticated users
            query = "SELECT service_name " +
                    "FROM services " +
                    "WHERE service_pk IN " +
                    "(SELECT service_id FROM view_count R RECOMMEND R.service_id TO R.user_id ON R.visits USING ItemCosCF WHERE R.user_id = ? ORDER BY R.visits LIMIT ? )";

            List<String> serviceIds = jdbcTemplate.queryForList(query, new Object[]{user_id, limit}, java.lang.String.class);

            String[] ids = serviceIds.toArray(new String[0]);
            services = serviceBundleService.getByIds(authentication, ids).stream().map(ServiceBundle::getService).collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.warn("Could not find user {} in recommendation database.", user.getEmail());
        } catch (Exception e) {
            logger.error(e);
        }
        return ResponseEntity.ok(services);
    }

}
