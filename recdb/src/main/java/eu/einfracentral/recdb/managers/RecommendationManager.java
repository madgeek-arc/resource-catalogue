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
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Component
public class RecommendationManager implements RecommendationService<RichService, Authentication> {

    private static final Logger logger = LogManager.getLogger(RecommendationManager.class);
    private final InfraServiceService<InfraService, InfraService> infraService;
    private final DataSource recdbDataSource;

    @Autowired
    public RecommendationManager(InfraServiceService<InfraService, InfraService> infraService,
                                 @Qualifier("recdbDataSource") DataSource recdbDataSource) {
        this.infraService = infraService;
        this.recdbDataSource = recdbDataSource;
    }

    public ResponseEntity<List<RichService>> getRecommendedResources(int limit, Authentication authentication) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(recdbDataSource);
        List<RichService> services = new ArrayList<>();

        /* Get user id */
        int user_id = -1;
        String query = "SELECT user_pk FROM users WHERE user_email = ?;";
        try {
            user_id = jdbcTemplate.queryForObject(query, new Object[]{((OIDCAuthenticationToken) authentication).getUserInfo().getEmail()}, int.class);
            query = "SELECT service_name " +
                    "FROM services " +
                    "WHERE service_pk IN " +
                    "(SELECT service_id FROM view_count R RECOMMEND R.service_id TO R.user_id ON R.visits USING ItemCosCF WHERE R.user_id = ? ORDER BY R.visits LIMIT ? )";

            List<String> serviceIds = jdbcTemplate.queryForList(query, new Object[]{user_id, limit}, java.lang.String.class);

            String[] ids = serviceIds.toArray(new String[0]);
            services = infraService.getByIds(authentication, ids);
        } catch (DataAccessException e) {
            logger.warn("Could not find user {} in recommendation database.", ((OIDCAuthenticationToken) authentication).getUserInfo().getEmail());
        } catch (NullPointerException e) {
            logger.error("Null pointer exception", e);
        } catch (Exception e) {
            logger.error(e);
        }
        return ResponseEntity.ok(services);
    }

}
