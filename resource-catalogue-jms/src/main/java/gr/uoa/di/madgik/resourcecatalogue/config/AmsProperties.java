package gr.uoa.di.madgik.resourcecatalogue.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Null;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "catalogue.jms.ams")
@Validated
public class AmsProperties {

    private boolean enabled = true;

    private String host;
    private String key;
    private String project;

    public AmsProperties() {
    }

    @PostConstruct
    void validate() {
        if (enabled) {
            if (!StringUtils.hasText(host)) {
                throw new IllegalArgumentException("Property 'catalogue.jms.ams.host' value is missing.");
            }
            if (!StringUtils.hasText(key)) {
                throw new IllegalArgumentException("Property 'catalogue.jms.ams.key' value is missing.");
            }
            if (!StringUtils.hasText(project)) {
                throw new IllegalArgumentException("Property 'catalogue.jms.ams.project' value is missing.");
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String projects) {
        this.project = projects;
    }
}
