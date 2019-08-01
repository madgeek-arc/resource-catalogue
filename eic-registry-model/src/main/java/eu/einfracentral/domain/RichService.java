package eu.einfracentral.domain;


import eu.einfracentral.dto.Category;
import eu.einfracentral.dto.ScientificDomain;

import javax.xml.bind.annotation.XmlTransient;
import java.util.List;

@XmlTransient
public class RichService {

    private Service service;
    private ServiceMetadata serviceMetadata;

    private List<String> languageNames;
    private List<String> placeNames;
    private String trlName;
    private String phaseName;

    private List<String> targetUsersNames;
    private List<String> accessTypeNames;
    private List<String> accessModeNames;
    private List<String> fundedByNames;
    private String orderTypeName;

    private int views;
    private int ratings;
    private float userRate;
    private float hasRate;
    private int favourites;
    private boolean isFavourite;

    private List<Category> categories;
    private List<ScientificDomain> domains;

    public RichService() {
        // No arg constructor
    }

    public RichService(Service service, ServiceMetadata serviceMetadata) {
        this.service = service;
        this.serviceMetadata = serviceMetadata;
    }

    public RichService(InfraService service) {
        this.service = new Service(service); // copy constructor is needed to 'hide' infraService fields
        this.serviceMetadata = service.getServiceMetadata();
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public ServiceMetadata getServiceMetadata() {
        return serviceMetadata;
    }

    public void setServiceMetadata(ServiceMetadata serviceMetadata) {
        this.serviceMetadata = serviceMetadata;
      

    // Getters/Setters for VocabularyNames
    public List<String> getLanguageNames() {
        return languageNames;
    }

    public void setLanguageNames(List<String> languageNames) {
        this.languageNames = languageNames;
    }

    public List<String> getPlaceNames() {
        return placeNames;
    }

    public void setPlaceNames(List<String> placeNames) {
        this.placeNames = placeNames;
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

    public List<String> getFundedByNames() {
        return fundedByNames;
    }

    public void setFundedByNames(List<String> fundedByNames) {
        this.fundedByNames = fundedByNames;
    }

    public String getOrderTypeName() {
        return orderTypeName;
    }

    public void setOrderTypeName(String orderTypeName) {
        this.orderTypeName = orderTypeName;
    }

      
    // Getters/Setters for Statistics
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

    public boolean getIsFavourite() {
        return isFavourite;
    }

    public void setFavourite(boolean favourite) {
        isFavourite = favourite;
    }

    public float getUserRate() {
        return userRate;
    }

    public void setUserRate(float userRate) {
        this.userRate = userRate;
    }

    public List<ScientificDomain> getDomains() {
        return domains;
    }

    public void setDomains(List<ScientificDomain> domains) {
        this.domains = domains;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }
}
