package eu.einfracentral.domain;


import eu.einfracentral.dto.Category;
import eu.einfracentral.dto.ProviderInfo;
import eu.einfracentral.dto.ScientificDomain;

import javax.xml.bind.annotation.XmlTransient;
import java.util.List;
import java.util.Objects;

@XmlTransient
public class RichResource {

    private Service service;
    private Datasource datasource;
    private TrainingResource trainingResource;
    private Metadata metadata;

    private List<String> languageAvailabilityNames;
    private List<String> geographicAvailabilityNames;
    private String trlName;
    private String phaseName;
    private String lifeCycleStatusName;
    private List<String> targetUsersNames;
    private List<String> accessTypeNames;
    private List<String> accessModeNames;
    private List<String> fundingBodyNames;
    private List<String> fundingProgramNames;
    private String orderTypeName;

    private float views;
    private float ratings;
    private float userRate;
    private float hasRate;
    private float favourites;
    private float isFavourite;

    private List<Category> categories;
    private List<ScientificDomain> domains;
    private List<ProviderInfo> providerInfo;

    public RichResource() {
        // No arg constructor
    }

    public RichResource(ServiceBundle serviceBundle) {
        this.service = serviceBundle.getService();
        this.metadata = serviceBundle.getMetadata();
    }

    public RichResource(Service service, Metadata metadata) {
        this.service = service;
        this.metadata = metadata;
    }

    public RichResource(DatasourceBundle dataSourceBundle) {
        this.datasource = dataSourceBundle.getDatasource();
        this.metadata = dataSourceBundle.getMetadata();
    }

    public RichResource(Datasource datasource, Metadata metadata) {
        this.datasource = datasource;
        this.metadata = metadata;
    }

    public RichResource(ResourceBundle<?> resource) {
        if (resource.getPayload() instanceof Datasource){
            this.datasource = (Datasource) resource.getPayload();
        } else {
            this.service = resource.getPayload();
        }
        this.metadata = resource.getMetadata();
    }

    public RichResource(TrainingResourceBundle trainingResourceBundle) {
        this.trainingResource = trainingResourceBundle.getTrainingResource();
        this.metadata = trainingResourceBundle.getMetadata();
    }

    public RichResource(TrainingResource trainingResource, Metadata metadata) {
        this.trainingResource = trainingResource;
        this.metadata = metadata;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Datasource getDatasource() {
        return datasource;
    }

    public void setDatasource(Datasource datasource) {
        this.datasource = datasource;
    }

    public TrainingResource getTrainingResource() {
        return trainingResource;
    }

    public void setTrainingResource(TrainingResource trainingResource) {
        this.trainingResource = trainingResource;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }


    // Getters/Setters for VocabularyNames

    public List<String> getLanguageAvailabilityNames() {
        return languageAvailabilityNames;
    }

    public void setLanguageAvailabilityNames(List<String> languageAvailabilityNames) {
        this.languageAvailabilityNames = languageAvailabilityNames;
    }

    public List<String> getGeographicAvailabilityNames() {
        return geographicAvailabilityNames;
    }

    public void setGeographicAvailabilityNames(List<String> geographicAvailabilityNames) {
        this.geographicAvailabilityNames = geographicAvailabilityNames;
    }

    public String getTrlName() {
        return trlName;
    }

    public void setTrlName(String trlName) {
        this.trlName = trlName;
    }

    public String getPhaseName() {
        return phaseName;
    }

    public void setPhaseName(String phaseName) {
        this.phaseName = phaseName;
    }

    public String getLifeCycleStatusName() {
        return lifeCycleStatusName;
    }

    public void setLifeCycleStatusName(String lifeCycleStatusName) {
        this.lifeCycleStatusName = lifeCycleStatusName;
    }

    public List<String> getTargetUsersNames() {
        return targetUsersNames;
    }

    public void setTargetUsersNames(List<String> targetUsersNames) {
        this.targetUsersNames = targetUsersNames;
    }

    public List<String> getAccessTypeNames() {
        return accessTypeNames;
    }

    public void setAccessTypeNames(List<String> accessTypeNames) {
        this.accessTypeNames = accessTypeNames;
    }

    public List<String> getAccessModeNames() {
        return accessModeNames;
    }

    public void setAccessModeNames(List<String> accessModeNames) {
        this.accessModeNames = accessModeNames;
    }

    public List<String> getFundingBodyNames() {
        return fundingBodyNames;
    }

    public void setFundingBodyNames(List<String> fundingBodyNames) {
        this.fundingBodyNames = fundingBodyNames;
    }

    public List<String> getFundingProgramNames() {
        return fundingProgramNames;
    }

    public void setFundingProgramNames(List<String> fundingProgramNames) {
        this.fundingProgramNames = fundingProgramNames;
    }

    public String getOrderTypeName() {
        return orderTypeName;
    }

    public void setOrderTypeName(String orderTypeName) {
        this.orderTypeName = orderTypeName;
    }

    public float getViews() {
        return views;
    }

    public void setViews(float views) {
        this.views = views;
    }

    public float getRatings() {
        return ratings;
    }

    public void setRatings(float ratings) {
        this.ratings = ratings;
    }

    public float getUserRate() {
        return userRate;
    }

    public void setUserRate(float userRate) {
        this.userRate = userRate;
    }

    public float getHasRate() {
        return hasRate;
    }

    public void setHasRate(float hasRate) {
        this.hasRate = hasRate;
    }

    public float getFavourites() {
        return favourites;
    }

    public void setFavourites(float favourites) {
        this.favourites = favourites;
    }

    public float getIsFavourite() {
        return isFavourite;
    }

    public void setIsFavourite(float isFavourite) {
        this.isFavourite = isFavourite;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public List<ScientificDomain> getDomains() {
        return domains;
    }

    public void setDomains(List<ScientificDomain> domains) {
        this.domains = domains;
    }

    public List<ProviderInfo> getProviderInfo() {
        return providerInfo;
    }

    public void setProviderInfo(List<ProviderInfo> providerInfo) {
        this.providerInfo = providerInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RichResource that = (RichResource) o;
        return Float.compare(that.views, views) == 0 && Float.compare(that.ratings, ratings) == 0 && Float.compare(that.userRate, userRate) == 0 && Float.compare(that.hasRate, hasRate) == 0 && Float.compare(that.favourites, favourites) == 0 && Float.compare(that.isFavourite, isFavourite) == 0 && Objects.equals(service, that.service) && Objects.equals(datasource, that.datasource) && Objects.equals(trainingResource, that.trainingResource) && Objects.equals(metadata, that.metadata) && Objects.equals(languageAvailabilityNames, that.languageAvailabilityNames) && Objects.equals(geographicAvailabilityNames, that.geographicAvailabilityNames) && Objects.equals(trlName, that.trlName) && Objects.equals(phaseName, that.phaseName) && Objects.equals(lifeCycleStatusName, that.lifeCycleStatusName) && Objects.equals(targetUsersNames, that.targetUsersNames) && Objects.equals(accessTypeNames, that.accessTypeNames) && Objects.equals(accessModeNames, that.accessModeNames) && Objects.equals(fundingBodyNames, that.fundingBodyNames) && Objects.equals(fundingProgramNames, that.fundingProgramNames) && Objects.equals(orderTypeName, that.orderTypeName) && Objects.equals(categories, that.categories) && Objects.equals(domains, that.domains) && Objects.equals(providerInfo, that.providerInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, datasource, trainingResource, metadata, languageAvailabilityNames, geographicAvailabilityNames, trlName, phaseName, lifeCycleStatusName, targetUsersNames, accessTypeNames, accessModeNames, fundingBodyNames, fundingProgramNames, orderTypeName, views, ratings, userRate, hasRate, favourites, isFavourite, categories, domains, providerInfo);
    }
}
