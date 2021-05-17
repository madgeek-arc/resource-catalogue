package eu.einfracentral.ui;

import java.util.List;

public class GroupedFields {

    private Group group;
    private List<Field> fields;

    public GroupedFields() {
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }
}
