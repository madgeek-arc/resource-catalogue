package eu.openminted.registry.beans.security;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import org.mitre.openid.connect.client.OIDCAuthoritiesMapper;
import org.mitre.openid.connect.client.SubjectIssuerGrantedAuthority;
import org.mitre.openid.connect.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.text.ParseException;
import java.util.*;

/**
 * Created by stefanos on 23/6/2017.
 */
public class OMTDAuthoritiesMapper implements OIDCAuthoritiesMapper {

    private static Logger logger = LoggerFactory.getLogger(OMTDAuthoritiesMapper.class);

    final private static String ROLE_CLAIMS = "edu_person_entitlements";

    private Map<String,SimpleGrantedAuthority> userRolesMap;

    OMTDAuthoritiesMapper(Map<String,String> userRoles) {
        userRolesMap = new HashMap<>();
        userRoles.forEach((omtdRole, appRole) -> userRolesMap.put(omtdRole, new SimpleGrantedAuthority(appRole)));
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(JWT idToken, UserInfo userInfo) {
        Set<GrantedAuthority> out = new HashSet<>();
        out.add(new SimpleGrantedAuthority("ROLE_USER"));
        userInfo.getSource().getAsJsonArray(ROLE_CLAIMS).forEach(role -> {
            SimpleGrantedAuthority authority = userRolesMap.get(role.getAsString());
            if(authority != null) {
                logger.debug("Role mapped " + role);
                out.add(authority);
            }
        });
        return out;
    }
}
