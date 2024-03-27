package gr.uoa.di.madgik.resourcecatalogue.dto;

public class ExtendedValue extends Value {

    private String catalogue;

    public ExtendedValue() {
    }

    public ExtendedValue(String catalogue) {
        this.catalogue = catalogue;
    }

    public String getCatalogue() {
        return catalogue;
    }

    public void setCatalogue(String catalogue) {
        this.catalogue = catalogue;
    }
}
