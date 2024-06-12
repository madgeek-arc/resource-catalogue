package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.resourcecatalogue.exception.OidcAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

public class AuthenticationInfo {

    private AuthenticationInfo() {
    }

    public static String getSub(Authentication auth) {
        return getClaims(auth).get("sub").toString();
    }

    public static String getEmail(Authentication auth) {
        return getClaims(auth).get("email").toString();
    }

    public static String getFullName(Authentication auth) {
        return getClaims(auth).get("name").toString();
    }

    public static String getGivenName(Authentication auth) {
        return getClaims(auth).get("given_name").toString();
    }

    public static String getFamilyName(Authentication auth) {
        return getClaims(auth).get("family_name").toString();
    }

    private static Map<String, Object> getClaims(Authentication auth) {
        if (auth.getPrincipal() instanceof OidcUser) {
            OidcUser principal = ((OidcUser) auth.getPrincipal());
            return principal.getClaims();
        } else if (auth.getPrincipal() instanceof Jwt) {
            return ((Jwt) auth.getPrincipal()).getClaims();
        } else {
            throw new OidcAuthenticationException("Could not retrieve user details.");
        }
    }
}
