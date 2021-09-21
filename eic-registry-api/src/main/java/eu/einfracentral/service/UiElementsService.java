package eu.einfracentral.service;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.dto.UiService;
import eu.einfracentral.dto.Value;
import eu.einfracentral.ui.Field;
import eu.einfracentral.ui.FieldGroup;
import eu.einfracentral.ui.Group;
import eu.einfracentral.ui.GroupedFields;

import java.util.List;
import java.util.Map;

public interface UiElementsService {

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

    Map<String, Object> createServiceSnippet(InfraService service);

    /**
     * Get Vocabulary - Service map by extra vocabulary
     */

    Map<Vocabulary, List<InfraService>> getByExtraVoc(String vocabularyType, String value);

    Map<String, List<InfraService>> getServicesByExtraVoc(String vocabularyType, String value);

    Map<String, List<Map<String, Object>>> getServicesSnippetsByExtraVoc(String vocabularyType, String value);

    Map<String, List<UiService>> getUiServicesByExtraVoc(String vocabularyType, String value);
}
