package eu.einfracentral.recdb.managers;

import eu.einfracentral.recdb.services.RecommendationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;

@Component
public class RecommendationManager implements RecommendationService{

    private static final Logger logger = LogManager.getLogger(RecommendationManager.class);

    @Autowired(required = true)
    @Qualifier("recdb.datasource")
    private DataSource datasource;

    @Autowired
    public RecommendationManager() {
    }

    public List<String> getRecommendationServices(Authentication authentication){
        JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
        String finalQuery = "SELECT service_name " +
                "FROM services " +
                "WHERE service_pk IN " +
                "(SELECT service_id FROM view_count R RECOMMEND R.service_id TO R.user_id ON R.visits USING ItemCosCF WHERE R.user_id = ? ORDER BY R.visits LIMIT 3 )";

        List<String> serviceIds = jdbcTemplate.queryForList(finalQuery, new Object[] { ((OIDCAuthenticationToken) authentication).getUserInfo().getEmail() }, java.lang.String.class);
        for (int i = 0; i < Objects.requireNonNull(serviceIds).size(); i++) {
            System.out.println(serviceIds.get(i));

        }
        return serviceIds;
    }

}
