package eu.einfracentral.manager;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.service.UiElementsService;
import eu.einfracentral.ui.Field;
import eu.einfracentral.ui.GroupedFields;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


@Component
public class UiElementsManager implements UiElementsService {

    private static final Logger logger = Logger.getLogger(UiElementsManager.class);

    private final String directory;
    private String jsonObject;

    @Autowired
    public UiElementsManager(@Value("${ui.elements.json.dir}") String directory) {
        if ("".equals(directory)) {
            directory = "catalogue/uiElements";
            logger.warn("'ui.elements.json.dir' was not set. Using default: " + directory);
        }
        this.directory = directory;
        File dir = new File(directory);
        if (dir.mkdirs()) {
            logger.error("Directory for UI elements has been created. Please place the necessary files inside...");
        }
    }

    //    @PostConstruct
    void readJsonFile(String filepath) {
        try {
            jsonObject = readFile(filepath);
        } catch (IOException e) {
            logger.error("Could not read UiElements Json File", e);
        }
    }

    protected String readFile(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            return sb.toString();
        }
    }

    @Override
    public List<Object> getElements() {
        return null;
    }

    @Override
    public List<String> getElementNames() {
        return null;
    }

    @Override
    public List<GroupedFields> getModel() {
        return null;
    }

    @Override
    public List<Field> getFields() {
        return null;
    }

    public List<Field> createFields(String className, String parent) throws ClassNotFoundException {
        List<Field> fields = new LinkedList<>();
        Class<?> clazz = Class.forName("eu.einfracentral.domain." + className);


        if (clazz.getSuperclass().getName().startsWith("eu.einfracentral.domain.Bundle")) {
            String name = clazz.getGenericSuperclass().getTypeName();
            name = name.replaceFirst(".*\\.", "").replace(">", "");
            List<Field> subfields = createFields(name, name);
            fields.addAll(subfields);
        }

        if (clazz.getSuperclass().getName().startsWith("eu.einfracentral.domain")) {
            String name = clazz.getSuperclass().getName().replaceFirst(".*\\.", "");
            List<Field> subfields = createFields(name, name);
            fields.addAll(subfields);
        }

        java.lang.reflect.Field[] classFields = clazz.getDeclaredFields();
        for (java.lang.reflect.Field field : classFields) {
            Field uiField = new Field();

//            field.setAccessible(true);
            uiField.setName(field.getName());
            uiField.setParent(parent);

            FieldValidation annotation = field.getAnnotation(FieldValidation.class);

            if (annotation != null) {
                uiField.getForm().setMandatory(!annotation.nullable());

                if (annotation.containsId() && Vocabulary.class.equals(annotation.idClass())) {
                    VocabularyValidation vvAnnotation = field.getAnnotation(VocabularyValidation.class);
                    if (vvAnnotation != null) {
                        uiField.getForm().setVocabulary(vvAnnotation.type().getKey());
                    }
                    uiField.setType("VOCABULARY");
                } else if (!field.getType().getName().contains("eu.einfracentral.domain.Identifiable")) {
                    String type = field.getType().getName();

                    if (Collection.class.isAssignableFrom(field.getType())) {
                        uiField.setMultiplicity(true);
                        type = field.getGenericType().getTypeName();
                        type = type.replaceFirst(".*<", "");
                        type = type.substring(0, type.length() - 1);
                    }
                    String typeName = type.replaceFirst(".*\\.", "").replaceAll("[<>]", "");
                    uiField.setType(typeName);

                    if (type.startsWith("eu.einfracentral.domain")) {
//                        uiField.getForm().setSubgroup(typeName);
                        List<Field> subfields = createFields(typeName, field.getName());
                        fields.addAll(subfields);
                    }
                }

            }
            fields.add(uiField);
        }
        return fields;
    }
}
