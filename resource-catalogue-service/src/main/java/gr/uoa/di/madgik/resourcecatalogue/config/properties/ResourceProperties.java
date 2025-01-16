package gr.uoa.di.madgik.resourcecatalogue.config.properties;

import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuerConfig;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;


@Validated
public class ResourceProperties {

    /**
     * The resource id prefix.
     */
    @NotNull
    @NotEmpty
    private String idPrefix;

    private String marketplaceEndpoint;

    /**
     * The PID Issuer properties.
     */
    private PidIssuerConfig pidIssuer;

    public ResourceProperties() {
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    public void setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public String getMarketplaceEndpoint() {
        return marketplaceEndpoint;
    }

    public void setMarketplaceEndpoint(String marketplaceEndpoint) {
        this.marketplaceEndpoint = marketplaceEndpoint;
    }

    public PidIssuerConfig getPidIssuer() {
        return pidIssuer;
    }

    public void setPidIssuer(PidIssuerConfig pidIssuer) {
        this.pidIssuer = pidIssuer;
    }
}
