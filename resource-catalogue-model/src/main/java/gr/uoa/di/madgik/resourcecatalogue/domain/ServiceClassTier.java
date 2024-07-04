package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;
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

    @XmlElement
    @Schema
    private List<String> offerings;

    public ServiceClassTier() {
    }

    public ServiceClassTier(int level, String accessPolicy, String costModel, List<String> offerings) {
        this.level = level;
        this.accessPolicy = accessPolicy;
        this.costModel = costModel;
        this.offerings = offerings;
    }

    @Override
    public String toString() {
        return "ServiceClassTier{" +
                "level=" + level +
                ", accessPolicy='" + accessPolicy + '\'' +
                ", costModel='" + costModel + '\'' +
                ", offerings=" + offerings +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceClassTier that = (ServiceClassTier) o;
        return level == that.level && Objects.equals(accessPolicy, that.accessPolicy) && Objects.equals(costModel, that.costModel) && Objects.equals(offerings, that.offerings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, accessPolicy, costModel, offerings);
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

    public List<String> getOfferings() {
        return offerings;
    }

    public void setOfferings(List<String> offerings) {
        this.offerings = offerings;
    }
}
