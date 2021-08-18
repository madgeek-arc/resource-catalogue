package eu.einfracentral.controllers;

import eu.einfracentral.dto.Value;
import eu.einfracentral.service.UiElementsService;
import eu.einfracentral.ui.Field;
import eu.einfracentral.ui.FieldGroup;
import eu.einfracentral.ui.GroupedFields;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("ui")
public class UiElementsController {

    private static final Logger logger = Logger.getLogger(UiElementsController.class);

    private final UiElementsService uiElementsService;

    @Autowired
    public UiElementsController(UiElementsService uiElementsService) {
        this.uiElementsService = uiElementsService;
    }

    @GetMapping(value = "{className}/fields/create", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Field> createFields(@PathVariable("className") String name) throws ClassNotFoundException {
        return uiElementsService.createFields(name, null);
    }

    @GetMapping(value = "{className}/fields", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Field> getFields(@PathVariable("className") String name) {
        return uiElementsService.getFields();
    }

    @GetMapping(value = "form/model/flat", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<GroupedFields<Field>> getFlatModel() {
        return uiElementsService.getFlatModel();
    }

    @GetMapping(value = "form/model", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<GroupedFields<FieldGroup>> getModel() {
        return uiElementsService.getModel();
    }

    @GetMapping(path = "vocabulary/{type}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<Value> getByExtraVoc(@PathVariable("type") String vocabularyType, @RequestParam(name = "used", required = false) Boolean used) {
        return uiElementsService.getControlValues(vocabularyType, used);
    }

    @GetMapping(value = "vocabularies", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<Value>> getControlValuesByType() {
        return uiElementsService.getControlValuesMap();
    }
}
