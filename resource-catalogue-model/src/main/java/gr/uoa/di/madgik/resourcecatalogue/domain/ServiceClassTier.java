package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ServiceClassTier {

    @XmlElement(required = true, defaultValue = "3")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int level;

    @XmlElement
    @Schema
    private String accessPolicy;

    @XmlElement
    @Schema
    private String costModel;

    public ServiceClassTier() {
    }

    public ServiceClassTier(int level, String accessPolicy, String costModel) {
        this.level = level;
        this.accessPolicy = accessPolicy;
        this.costModel = costModel;
    }

    @Override
    public String toString() {
        return "ServiceClassTier{" +
                "level=" + level +
                ", accessPolicy='" + accessPolicy + '\'' +
                ", costModel='" + costModel + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceClassTier that = (ServiceClassTier) o;
        return level == that.level && Objects.equals(accessPolicy, that.accessPolicy) && Objects.equals(costModel, that.costModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, accessPolicy, costModel);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(String accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public String getCostModel() {
        return costModel;
    }

    public void setCostModel(String costModel) {
        this.costModel = costModel;
    }
}
