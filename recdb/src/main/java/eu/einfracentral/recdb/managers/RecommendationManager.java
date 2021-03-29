package eu.einfracentral.recdb.managers;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.RichService;
import eu.einfracentral.recdb.services.RecommendationService;
import eu.einfracentral.registry.service.InfraServiceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;

import javax.sql.DataSource;
import java.util.List;

@Component
public class RecommendationManager implements RecommendationService{

    private static final Logger logger = LogManager.getLogger(RecommendationManager.class);
    private final InfraServiceService<InfraService, InfraService> infraService;

    @Autowired(required = true)
    @Qualifier("recdb.datasource")
    private DataSource datasource;

    @Autowired
    public RecommendationManager(InfraServiceService<InfraService, InfraService> infraService) {
        this.infraService = infraService;
    }

    public ResponseEntity<List<RichService>> getRecommendationServices(int limit, Authentication authentication){
        JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);

        /* Get user id */
        String query = "SELECT user_pk FROM users WHERE user_email = ?;";
        int user_id = jdbcTemplate.queryForObject(query, new Object[]{ ((OIDCAuthenticationToken) authentication).getUserInfo().getEmail() }, int.class);

        query = "SELECT service_name " +
                "FROM services " +
                "WHERE service_pk IN " +
                "(SELECT service_id FROM view_count R RECOMMEND R.service_id TO R.user_id ON R.visits USING ItemCosCF WHERE R.user_id = ? ORDER BY R.visits LIMIT ? )";

        List<String> serviceIds = jdbcTemplate.queryForList(query, new Object[] {user_id, limit}, java.lang.String.class);

        String[] ids = serviceIds.toArray(new String[0]);
        return ResponseEntity.ok(infraService.getByIds(authentication, ids));
    }

}
