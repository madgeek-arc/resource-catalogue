package gr.uoa.di.madgik.resourcecatalogue.dto;

public class PlaceCount {

    String place;
    int count;

    public PlaceCount() {
    }

    public PlaceCount(String place, int count) {
        this.place = place;
        this.count = count;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
