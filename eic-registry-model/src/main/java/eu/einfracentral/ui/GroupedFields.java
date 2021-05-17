package eu.einfracentral.ui;

import java.util.List;

public class GroupedFields {

    private String group;
    private List<Field> fields;

    public GroupedFields() {}

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }
}
