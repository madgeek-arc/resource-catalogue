package gr.uoa.di.madgik.resourcecatalogue.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "catalogue")
public class ResourceCatalogueProperties {

    private Set<String> admins;
    private Set<String> onboardingTeam;
    private String loginRedirect;
    private String logoutRedirect;

    public Set<String> getAdmins() {
        return admins;
    }

    public ResourceCatalogueProperties setAdmins(Set<String> admins) {
        this.admins = admins;
        return this;
    }

    public Set<String> getOnboardingTeam() {
        return onboardingTeam;
    }

    public void setOnboardingTeam(Set<String> onboardingTeam) {
        this.onboardingTeam = onboardingTeam;
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
