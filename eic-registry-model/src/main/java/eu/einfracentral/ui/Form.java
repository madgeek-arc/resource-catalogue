package eu.einfracentral.ui;


import java.util.List;

public class Form {

    FieldIdName dependsOn;
    List<FieldIdName> affects = null;
    String vocabulary;
    String group;
    String description;
    String suggestion;
    String placeholder;
    Boolean mandatory;
    Boolean immutable;
    Boolean isVisible;
    int order;


    public Form() {
    }

    public FieldIdName getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(FieldIdName dependsOn) {
        this.dependsOn = dependsOn;
    }

    public List<FieldIdName> getAffects() {
        return affects;
    }

    public void setAffects(List<FieldIdName> affects) {
        this.affects = affects;
    }

    public String getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(String vocabulary) {
        this.vocabulary = vocabulary;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public Boolean getImmutable() {
        return immutable;
    }

    public void setImmutable(Boolean immutable) {
        this.immutable = immutable;
    }

    public Boolean getVisible() {
        return isVisible;
    }

    public void setVisible(Boolean visible) {
        isVisible = visible;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
