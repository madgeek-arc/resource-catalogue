package eu.einfracentral.config.security;

import com.nimbusds.jwt.JWT;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.mitre.openid.connect.model.UserInfo;
import org.mitre.openid.connect.client.OIDCAuthoritiesMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class EICAuthoritiesMapper implements OIDCAuthoritiesMapper {

    final private static Logger logger = LogManager.getLogger(EICAuthoritiesMapper.class);
    final private static String ROLE_CLAIMS = "edu_person_entitlements";
    private Map<String, SimpleGrantedAuthority> userRolesMap;


    public EICAuthoritiesMapper() throws IOException {
        userRolesMap = new HashMap<>();
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(JWT idToken, UserInfo userInfo) {
        Set<GrantedAuthority> out = new HashSet<>();
        out.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (userInfo.getSource().getAsJsonArray(ROLE_CLAIMS) != null) {

            userInfo.getSource().getAsJsonArray(ROLE_CLAIMS).forEach(role -> {
                SimpleGrantedAuthority authority = userRolesMap.get(role.getAsString());
                if (authority != null) {
                    logger.debug("Role mapped " + role);
                    out.add(authority);
                }
            });
        }
        return out;
    }
}
