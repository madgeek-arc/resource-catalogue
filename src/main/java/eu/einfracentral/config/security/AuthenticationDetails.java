package eu.einfracentral.config.security;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.security.core.Authentication;

public class AuthenticationDetails {

    public static String getSub(Authentication auth) throws Exception {
        return getOIDC(auth).getSub();
    }

    public static String getEmail(Authentication auth) throws Exception {
        return getOIDC(auth).getUserInfo().getEmail();
    }



    private static OIDCAuthenticationToken getOIDC(Authentication auth) throws Exception {
        if (auth instanceof OIDCAuthenticationToken) {
            return ((OIDCAuthenticationToken) auth);
        } else {
            throw new Exception("Could not retriece 'sub' field. Authentication is not an instance of OIDCAuthentication");
        }
    }
}
