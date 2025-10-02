package gr.uoa.di.madgik.resourcecatalogue.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;


@Configuration
@ConfigurationProperties(prefix = "accounting")
@Validated
public class AccountingProperties {

    private boolean enabled;
    private String clientId;
    private String clientSecret;
    private String endpoint;
    private String projectName;
    private String tokenEndpoint;

    public AccountingProperties() {
    }

    @PostConstruct
    void validate() {
        if (enabled) {
            if (!StringUtils.hasText(clientId)) {
                throw new IllegalArgumentException("Property 'accounting.client-id' value is missing.");
            }
            if (!StringUtils.hasText(clientSecret)) {
                throw new IllegalArgumentException("Property 'accounting.client-secret' value is missing.");
            }
            if (!StringUtils.hasText(endpoint)) {
                throw new IllegalArgumentException("Property 'accounting.endpoint' value is missing.");
            }
            if (!StringUtils.hasText(projectName)) {
                throw new IllegalArgumentException("Property 'accounting.project-name' value is missing.");
            }
            if (!StringUtils.hasText(tokenEndpoint)) {
                throw new IllegalArgumentException("Property 'accounting.token-endpoint' value is missing.");
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }
}
