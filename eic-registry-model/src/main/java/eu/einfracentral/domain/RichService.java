package eu.einfracentral.domain;


import java.util.List;

// FIXME: change to composition instead of inheritance.
public class RichService extends Service {

    //    private Service service;
    private ServiceMetadata serviceMetadata;
    private String categoryName;
    private String subCategoryName;
    private String trlName;
    private String lifeCycleStatusName;
    private List<String> languageNames;
    private List<String> placeNames;
    private int views;
    private int ratings;
    private float userRate;
    private float hasRate;
    private int favourites;
    private boolean isFavourite;


    public RichService() {

    }

    public RichService(Service service, ServiceMetadata serviceMetadata) {
//        this.service = service;
        super(service);
        this.serviceMetadata = serviceMetadata;
    }

    public RichService(InfraService service) {
//        this.service = (Service) service;
        super(service);
        this.serviceMetadata = service.getServiceMetadata();
    }

/*    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }*/

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getSubCategoryName() {
        return subCategoryName;
    }

    public void setSubCategoryName(String subCategoryName) {
        this.subCategoryName = subCategoryName;
    }

    public List<String> getLanguageNames() {
        return languageNames;
    }

    public void setLanguageNames(List<String> languageNames) {
        this.languageNames = languageNames;
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

    public String getTrlName() {
        return trlName;
    }

    public void setTrlName(String trlName) {
        this.trlName = trlName;
    }

    public String getLifeCycleStatusName() {
        return lifeCycleStatusName;
    }

    public void setLifeCycleStatusName(String lifeCycleStatusName) {
        this.lifeCycleStatusName = lifeCycleStatusName;
    }

    public List<String> getPlaceNames() {
        return placeNames;
    }

    public void setPlaceNames(List<String> placeNames) {
        this.placeNames = placeNames;
    }
}
