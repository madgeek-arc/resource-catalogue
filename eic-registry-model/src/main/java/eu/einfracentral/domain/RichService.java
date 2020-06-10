package eu.einfracentral.domain;


import eu.einfracentral.dto.Category;
import eu.einfracentral.dto.ProviderInfo;
import eu.einfracentral.dto.ScientificDomain;

import javax.xml.bind.annotation.XmlTransient;
import java.util.List;

@XmlTransient
public class RichService {

    private Service service;
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

    private int views;
    private int ratings;
    private float userRate;
    private float hasRate;
    private int favourites;
    private boolean isFavourite;

    private List<Category> categories;
    private List<ScientificDomain> domains;
    private List<ProviderInfo> providerInfo;

    public RichService() {
        // No arg constructor
    }

    public RichService(Service service, Metadata metadata) {
        this.service = service;
        this.metadata = metadata;
    }

    public RichService(InfraService service) {
        this.service = service.getService(); // copy constructor is needed to 'hide' infraService fields
        this.metadata = service.getMetadata();
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
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

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public int getRatings() {
        return ratings;
    }

    public void setRatings(int ratings) {
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

    public int getFavourites() {
        return favourites;
    }

    public void setFavourites(int favourites) {
        this.favourites = favourites;
    }

    public boolean isFavourite() {
        return isFavourite;
    }

    public void setFavourite(boolean favourite) {
        isFavourite = favourite;
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
}
