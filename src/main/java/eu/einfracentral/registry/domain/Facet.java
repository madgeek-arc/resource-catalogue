package eu.einfracentral.registry.domain;

/**
 * Created by pgl on 27/6/2017.
 */
import java.util.List;

public class Facet {

    private String field;
    private String label;
    private List<Value> values;

    public Facet() {
    }

    public Facet(String field, String label, List<Value> values) {
        this.field = field;
        this.label = label;
        this.values = values;
    }

    public String getField() {
        return field;
    }
    public void setField(String field) {
        this.field = field;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public List<Value> getValues() {
        return values;
    }
    public void setValues(List<Value> values) {
        this.values = values;
    }



}