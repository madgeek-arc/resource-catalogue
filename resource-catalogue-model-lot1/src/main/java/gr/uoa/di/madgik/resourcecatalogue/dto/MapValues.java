package gr.uoa.di.madgik.resourcecatalogue.dto;

import java.util.List;

public class MapValues<T extends Value> {

    private String key;
    private List<T> values;

    public MapValues() {
    }

    public MapValues(String key, List<T> values) {
        this.key = key;
        this.values = values;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<T> getValues() {
        return values;
    }

    public void setValues(List<T> values) {
        this.values = values;
    }
}
