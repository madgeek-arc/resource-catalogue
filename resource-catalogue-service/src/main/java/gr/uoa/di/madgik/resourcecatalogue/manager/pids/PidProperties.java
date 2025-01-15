package gr.uoa.di.madgik.resourcecatalogue.manager.pids;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "pid")
@Validated
public class PidProperties {

    /**
     *  A key-value pair of resource names and PID Issuer Configurations.
     */
    private Map<String, PidIssuerConfig> resources = new HashMap<>();

    public PidProperties() {
    }

    public Map<String, PidIssuerConfig> getResources() {
        return resources;
    }

    public void setResources(Map<String, PidIssuerConfig> resources) {
        this.resources = resources;
    }

    public PidIssuerConfig getIssuerConfigurationByResource(String resource) {
        if (!this.resources.containsKey(resource)) {
            throw new IllegalArgumentException("Unknown prefix: " + resource);
        }
        return this.resources.get(resource);
    }

}
