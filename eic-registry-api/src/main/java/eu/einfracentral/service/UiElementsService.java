package eu.einfracentral.service;

import eu.einfracentral.ui.Field;
import eu.einfracentral.ui.Group;
import eu.einfracentral.ui.GroupedFields;

import java.util.List;

public interface UiElementsService {

    List<Object> getElements();

    List<String> getElementNames();

    List<GroupedFields> getModel();

    List<Group> getGroups();

    List<Field> getFields();

    List<Field> createFields(String className, String group) throws ClassNotFoundException;
}