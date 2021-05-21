package eu.einfracentral.ui;

public class RequiredFields {

    private int topLevel;
    private int total;

    public RequiredFields() {}

    public RequiredFields(int topLevel, int total) {
        this.topLevel = topLevel;
        this.total = total;
    }

    public int getTopLevel() {
        return topLevel;
    }

    public void setTopLevel(int topLevel) {
        this.topLevel = topLevel;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
