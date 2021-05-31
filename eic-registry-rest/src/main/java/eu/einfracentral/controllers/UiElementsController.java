package eu.einfracentral.controllers;

import eu.einfracentral.dto.Value;
import eu.einfracentral.service.UiElementsService;
import eu.einfracentral.ui.Field;
import eu.einfracentral.ui.FieldGroup;
import eu.einfracentral.ui.GroupedFields;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


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

    @GetMapping(value = "vocabulary/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Value> getControlValuesByType(@PathVariable("type") String type) {
        return uiElementsService.getControlValuesByType(type);
    }
}
