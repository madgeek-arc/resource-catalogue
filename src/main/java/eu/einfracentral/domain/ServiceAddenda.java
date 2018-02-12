package eu.einfracentral.domain;

import java.util.List;
import javax.xml.bind.annotation.*;

/**
 * Created by pgl on 24/10/17.
 */
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ServiceAddenda implements Identifiable {
    @XmlElement
    private String id;
    @XmlElement
    private List<Measurement<?>> performanceData;
    @XmlElement
    private int externalHits;
    @XmlElement
    private int internalHits;
    @XmlElement
    private int favouriteCount;
    @XmlElement
    private float averageRating;
    @XmlElement
    private float ratings;
    @XmlElement
    private boolean featured;
    @XmlElement(defaultValue = "false")
    private boolean published;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public List<Measurement<?>> getPerformanceData() {
        return performanceData;
    }

    public void setPerformanceData(List<Measurement<?>> performanceData) {
        this.performanceData = performanceData;
    }

    public int getExternalHits() {
        return externalHits;
    }

    public void setExternalHits(int externalHits) {
        this.externalHits = externalHits;
    }

    public int getInternalHits() {
        return internalHits;
    }

    public void setInternalHits(int internalHits) {
        this.internalHits = internalHits;
    }

    public int getFavouriteCount() {
        return favouriteCount;
    }

    public void setFavouriteCount(int favouriteCount) {
        this.favouriteCount = favouriteCount;
    }

    public float getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(float averageRating) {
        this.averageRating = averageRating;
    }

    public float getRatings() {
        return ratings;
    }

    public void setRatings(float ratings) {
        this.ratings = ratings;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }
}
