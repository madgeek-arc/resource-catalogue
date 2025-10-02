package gr.uoa.di.madgik.resourcecatalogue.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "helpdesk")
@Validated
public class HelpdeskProperties {

    private boolean enabled;
    private String endpoint;

    public HelpdeskProperties() {
    }

    @PostConstruct
    void validate() {
        if (enabled) {
            if (!StringUtils.hasText(endpoint)) {
                throw new IllegalArgumentException("Property 'helpdesk.endpoint' value is missing.");
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
