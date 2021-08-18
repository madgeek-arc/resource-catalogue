package eu.einfracentral.service;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.dto.UiService;
import eu.einfracentral.dto.Value;
import eu.einfracentral.ui.Field;
import eu.einfracentral.ui.FieldGroup;
import eu.einfracentral.ui.Group;
import eu.einfracentral.ui.GroupedFields;

import java.util.List;
import java.util.Map;

public interface UiElementsService {

    List<Object> getElements();

    List<String> getElementNames();

    List<GroupedFields<FieldGroup>> getModel();

    List<GroupedFields<Field>> getFlatModel();

    List<Group> getGroups();

    Field getField(int id);

    List<Field> getFields();

    List<Field> createFields(String className, String group) throws ClassNotFoundException;

    Map<String, List<Value>> getControlValuesMap();

    List<Value> getControlValues(String type);

    List<Value> getControlValues(String type, Boolean used);

    InfraService createService(UiService service);

    UiService createUiService(InfraService service);

    Map<String, List<UiService>> getUiServicesByExtraVoc(String vocabularyType, String value);
}
