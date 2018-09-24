package eu.einfracentral.utils;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.security.core.Authentication;

public class AuthenticationDetails {

    public static String getSub(Authentication auth) throws Exception {
        return getOIDC(auth).getSub();
    }

    public static String getEmail(Authentication auth) throws Exception {
        return getOIDC(auth).getUserInfo().getEmail();
    }

    public static String getName(Authentication auth) throws Exception {
        return getOIDC(auth).getUserInfo().getName();
    }

    public static String getGivenName(Authentication auth) throws Exception {
        return getOIDC(auth).getUserInfo().getGivenName();
    }

    public static String getFamilyName(Authentication auth) throws Exception {
        return getOIDC(auth).getUserInfo().getFamilyName();
    }

    private static OIDCAuthenticationToken getOIDC(Authentication auth) throws Exception {
        if (auth instanceof OIDCAuthenticationToken) {
            return ((OIDCAuthenticationToken) auth);
        } else {
            throw new Exception("Could not retriece 'sub' field. Authentication is not an instance of OIDCAuthentication");
        }
    }
}
