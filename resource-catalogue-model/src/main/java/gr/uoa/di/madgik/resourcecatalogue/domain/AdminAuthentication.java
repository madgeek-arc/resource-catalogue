package gr.uoa.di.madgik.resourcecatalogue.domain;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminAuthentication implements Authentication {

    private final String name = "Administrator";
    private boolean authenticated = true;

    public AdminAuthentication() {
        // no-arg constructor
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        Map<String, Object> info = new HashMap<>();
        info.put("email", "admin@resource-catalogue.gr");
        info.put("givenName", "Admin");
        info.put("familyName", "Administrator");
        return new OidcUserInfo(info);
    }

    @Override
    public Object getPrincipal() {
        return name;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return name;
    }
}
