package eu.einfracentral.ui;

public class Field {

    private static int ID_COUNTER = 0;

    int id;
    String name;
    Integer parentId;
    String parent;
    String label;
    String accessPath;
    boolean multiplicity = false;
    String type;
    boolean includedInSnippet;
    Form form;
    Display display;

    public Field() {
        this.id = ID_COUNTER++;
        this.form = new Form();
        this.display = new Display();
    }

    public String getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type.getKey();
    }

    public void setType(String type) {
        try {
            if (FieldType.exists(type)) {
                this.type = FieldType.fromString(type).getKey();
            } else {
                this.type = type;
            }
        } catch (IllegalArgumentException e) {
            this.type = FieldType.COMPOSITE.getKey();
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAccessPath() {
        return accessPath;
    }

    public void setAccessPath(String accessPath) {
        this.accessPath = accessPath;
    }

    public boolean getMultiplicity() {
        return multiplicity;
    }

    public void setMultiplicity(boolean multiplicity) {
        this.multiplicity = multiplicity;
    }

    public boolean isIncludedInSnippet() {
        return includedInSnippet;
    }

    public void setIncludedInSnippet(boolean includedInSnippet) {
        this.includedInSnippet = includedInSnippet;
    }

    public Form getForm() {
        return form;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public Display getDisplay() {
        return display;
    }

    public void setDisplay(Display display) {
        this.display = display;
    }
}
