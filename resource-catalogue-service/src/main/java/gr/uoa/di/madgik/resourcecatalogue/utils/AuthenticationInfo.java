package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.resourcecatalogue.exception.OidcAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class AuthenticationInfo {

    private AuthenticationInfo() {
    }

    public static String getSub(Authentication auth) {
        return getOidcUser(auth).getSubject();
    }

    public static String getEmail(Authentication auth) {
        return getOidcUser(auth).getEmail();
    }

    public static String getName(Authentication auth) {
        return getOidcUser(auth).getName();
    }

    public static String getGivenName(Authentication auth) {
        return getOidcUser(auth).getGivenName();
    }

    public static String getFamilyName(Authentication auth) {
        return getOidcUser(auth).getFamilyName();
    }

    private static OidcUser getOidcUser(Authentication auth) {
        if (auth.getPrincipal() instanceof OidcUser) {
            OidcUser principal = ((OidcUser) auth.getPrincipal());
            return principal;
        } else {
            throw new OidcAuthenticationException("Could not retrieve user details.");
        }
    }
}
