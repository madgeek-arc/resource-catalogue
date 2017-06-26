package eu.openminted.registry.beans;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by stefanos on 9/5/2017.
 */
public class FrontEndLinkURIAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private String frontEndURI;
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OIDCAuthenticationToken authOIDC = (OIDCAuthenticationToken) authentication;
        Cookie sessionCookie = new Cookie("name", authOIDC.getSub());
        int expireSec = -1;
        sessionCookie.setMaxAge(expireSec);
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);
        response.sendRedirect(frontEndURI);
    }

    public String getFrontEndURI() {
        return frontEndURI;
    }

    public void setFrontEndURI(String frontEndURI) {
        this.frontEndURI = frontEndURI;
    }
}
