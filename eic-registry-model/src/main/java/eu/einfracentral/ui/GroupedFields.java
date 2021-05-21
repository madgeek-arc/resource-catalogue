package eu.einfracentral.ui;

import java.util.List;

public class GroupedFields <T> {

    private Group group;
    private List<T> fields;
    private RequiredFields required;

    public GroupedFields() {
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public List<T> getFields() {
        return fields;
    }

    public void setFields(List<T> fields) {
        this.fields = fields;
    }

    public RequiredFields getRequired() {
        return required;
    }

    public void setRequired(RequiredFields required) {
        this.required = required;
    }
}
