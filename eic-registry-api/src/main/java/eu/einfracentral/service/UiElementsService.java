package eu.einfracentral.service;

import eu.einfracentral.ui.Field;

import java.util.List;

public interface UiElementsService {

    List<Object> getElements();

    List<String> getElementNames();

    List<Field> getFields();

    List<Field> createFields(String className, String group) throws ClassNotFoundException;
}
