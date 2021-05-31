package eu.einfracentral.service;

import eu.einfracentral.dto.Value;
import eu.einfracentral.ui.Field;
import eu.einfracentral.ui.FieldGroup;
import eu.einfracentral.ui.Group;
import eu.einfracentral.ui.GroupedFields;

import java.util.List;

public interface UiElementsService {

    List<Object> getElements();

    List<String> getElementNames();

    List<GroupedFields<FieldGroup>> getModel();

    List<GroupedFields<Field>> getFlatModel();

    List<Group> getGroups();

    List<Field> getFields();

    List<Field> createFields(String className, String group) throws ClassNotFoundException;

    List<Value> getControlValuesByType(String type);
}
