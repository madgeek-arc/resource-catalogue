package eu.einfracentral.ui;

import java.util.List;

public class FieldGroup {

    private Field field;
    private List<FieldGroup> subFieldGroups;

    public FieldGroup() {}

    public FieldGroup(Field field) {
        this.field = field;
    }

    public FieldGroup(Field field, List<FieldGroup> subFieldGroups) {
        this.field = field;
        this.subFieldGroups = subFieldGroups;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public List<FieldGroup> getSubFieldGroups() {
        return subFieldGroups;
    }

    public void setSubFieldGroups(List<FieldGroup> subFieldGroups) {
        this.subFieldGroups = subFieldGroups;
    }
}
