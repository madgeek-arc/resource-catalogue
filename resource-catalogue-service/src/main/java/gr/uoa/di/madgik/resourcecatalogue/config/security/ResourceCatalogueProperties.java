package gr.uoa.di.madgik.resourcecatalogue.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "catalogue")
public class ResourceCatalogueProperties {

    private Set<Object> admins;
    private String loginRedirect;
    private String logoutRedirect;

    public Set<Object> getAdmins() {
        return admins;
    }

    public ResourceCatalogueProperties setAdmins(Set<Object> admins) {
        this.admins = admins;
        return this;
    }

    public String getLoginRedirect() {
        return loginRedirect;
    }

    public ResourceCatalogueProperties setLoginRedirect(String loginRedirect) {
        this.loginRedirect = loginRedirect;
        return this;
    }

    public String getLogoutRedirect() {
        return logoutRedirect;
    }

    public ResourceCatalogueProperties setLogoutRedirect(String logoutRedirect) {
        this.logoutRedirect = logoutRedirect;
        return this;
    }
}
