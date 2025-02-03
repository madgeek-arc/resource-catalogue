package gr.uoa.di.madgik.resourcecatalogue.config.properties;

import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuerConfig;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.List;


@Validated
public class ResourceProperties {

    /**
     * The resource id prefix.
     */
    @NotNull
    @NotEmpty
    private String idPrefix;

    /**
     * Endpoints in which the PID will resolve to (optional).
     */
    private List<String> resolveEndpoints;

    /**
     * The PID Issuer properties (optional).
     */
    @NestedConfigurationProperty
    private PidIssuerConfig pidIssuer;

    public ResourceProperties() {
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    public void setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public List<String> getResolveEndpoints() {
        return resolveEndpoints;
    }

    public void setResolveEndpoints(List<String> resolveEndpoints) {
        this.resolveEndpoints = resolveEndpoints;
    }

    public PidIssuerConfig getPidIssuer() {
        return pidIssuer;
    }

    public void setPidIssuer(PidIssuerConfig pidIssuer) {
        this.pidIssuer = pidIssuer;
    }
}
