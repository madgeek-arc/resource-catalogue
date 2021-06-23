package eu.einfracentral.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import eu.einfracentral.domain.DynamicField;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.dto.UiService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.UiElementsService;
import eu.einfracentral.ui.*;
import eu.openminted.registry.core.domain.FacetFilter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.CACHE_FOR_UI;


@Component
public class UiElementsManager implements UiElementsService {

    private static final Logger logger = Logger.getLogger(UiElementsManager.class);

    private static final String FILENAME_GROUPS = "groups.json";
    private static final String FILENAME_FIELDS = "fields.json";

    private final String directory;
    private String jsonObject;

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    private final VocabularyService vocabularyService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final InfraServiceService<InfraService, InfraService> infraServiceService;

    @Autowired
    public UiElementsManager(@Value("${ui.elements.json.dir}") String directory,
                             VocabularyService vocabularyService,
                             ProviderService<ProviderBundle, Authentication> providerService,
                             InfraServiceService<InfraService, InfraService> infraServiceService) {
        this.vocabularyService = vocabularyService;
        this.providerService = providerService;
        this.infraServiceService = infraServiceService;
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

    @Scheduled(fixedRate = 3600000)
    @CachePut(value = CACHE_FOR_UI)
    public Map<String, List<eu.einfracentral.dto.Value>> cacheVocabularies() {
        return getControlValuesByType();
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

    protected List<Group> readGroups(String filepath) {
        List<Group> groups = null;
        try {
            jsonObject = readFile(filepath);
            ObjectMapper objectMapper = new ObjectMapper();
            Group[] groupsArray = objectMapper.readValue(jsonObject, Group[].class);
            groups = new ArrayList<>(Arrays.asList(groupsArray));
        } catch (IOException e) {
            logger.error(e);
        }

        return groups;
    }

    protected List<Field> readFields(String filepath) {
        List<Field> fields = null;
        try {
            jsonObject = readFile(filepath);
            ObjectMapper objectMapper = new ObjectMapper();
            Field[] fieldsArray = objectMapper.readValue(jsonObject, Field[].class);
            fields = new ArrayList<>(Arrays.asList(fieldsArray));
        } catch (IOException e) {
            logger.error(e);
        }

        return fields;
    }

    private Field getField(int id) {
        List<Field> allFields = readFields(directory + "/" + FILENAME_FIELDS);
        for (Field field : allFields) {
            if (field.getId() == id) {
                return field;
            }
        }
        return null;
    }

    private Field getExtraField(String name) {
        List<Field> allFields = readFields(directory + "/" + FILENAME_FIELDS);
        for (Field field : allFields) {
            if (field.getParent() != null && "extras".equals(field.getParent()) && name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public InfraService createService(UiService service) {
        List<DynamicField> extras = new ArrayList<>();

        for (Map.Entry<String, ?> entry : service.getExtras().entrySet()) {
            DynamicField field = new DynamicField();
            field.setName(entry.getKey());
            if (!Collection.class.isAssignableFrom(entry.getValue().getClass())) {
                List<Object> temp = new ArrayList<>();
                temp.add(entry.getValue());
                field.setValue(temp);
            } else {
                field.setValue((List<Object>) entry.getValue());
            }
            extras.add(field);


            Field fieldInfo = getExtraField(entry.getKey());
            if (fieldInfo != null) {
                field.setFieldId(fieldInfo.getId());
            }

        }
        InfraService infraService = new InfraService();
        infraService.setService(service.getService());
        infraService.setExtras(extras);
        return infraService;
    }

    @Override
    public UiService createUiService(InfraService service) {
        UiService uiService = new UiService();
        uiService.setService(service.getService());
        uiService.setExtras(new HashMap<>());
        for (DynamicField field : service.getExtras()) {
            Field fieldInfo = getField(field.getFieldId());
            if (fieldInfo != null && !fieldInfo.getMultiplicity()) {
                if (field.getValue() != null && !field.getValue().isEmpty()) {
                    uiService.getExtras().put(field.getName(), field.getValue().get(0));
                } else {
                    uiService.getExtras().put(field.getName(), field.getValue());
                }
            } else {
                uiService.getExtras().put(field.getName(), field.getValue());
            }
        }
        return uiService;
    }

    @Override
    public List<Object> getElements() {
        return null;
    }

    @Override
    public List<String> getElementNames() {
        return null;
    }

    @Override // TODO: refactoring
    public List<GroupedFields<FieldGroup>> getModel() {
        List<GroupedFields<FieldGroup>> groupedFieldGroups = new ArrayList<>();
        List<GroupedFields<Field>> groupedFieldsList = getFlatModel();

        for (GroupedFields<Field> groupedFields : groupedFieldsList) {
            GroupedFields<FieldGroup> groupedFieldGroup = new GroupedFields<>();

            groupedFieldGroup.setGroup(groupedFields.getGroup());
            List<FieldGroup> fieldGroups = createFieldGroups(groupedFields.getFields());
            groupedFieldGroup.setFields(fieldGroups);

            int total = 0;
            for (Field f : groupedFields.getFields()) {
                if (f.getForm().getMandatory() != null && f.getForm().getMandatory()
                        && f.getType() != null && !f.getType().equals("composite")) {
                    total += 1;
                }
            }

            int topLevel = 0;
            for (FieldGroup fg : fieldGroups) {
                if (fg.getField().getForm().getMandatory() != null && fg.getField().getForm().getMandatory()) {
                    topLevel += 1;
                }
            }
            RequiredFields requiredFields = new RequiredFields(topLevel, total);
            groupedFieldGroup.setRequired(requiredFields);

            groupedFieldGroups.add(groupedFieldGroup);
        }


        return groupedFieldGroups;
    }

    // TODO: recurse until starting list length == returned list length
    List<FieldGroup> createFieldGroups(List<Field> fields) {
        Map<Integer, FieldGroup> fieldGroupMap = new HashMap<>();
        Map<Integer, List<FieldGroup>> groups = new HashMap<>();
        Set<Integer> ids = fields
                .stream()
                .map(Field::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Integer id : ids) {
            groups.put(id, new ArrayList<>());
        }

        for (Iterator<Field> it = fields.iterator(); it.hasNext(); ) {
            Field field = it.next();
            FieldGroup fieldGroup = new FieldGroup(field);
            if (ids.contains(field.getParentId())) {
                groups.get(field.getParentId()).add(fieldGroup);
            } else {
                fieldGroupMap.put(field.getId(), fieldGroup);
            }
        }

        for (Map.Entry<Integer, List<FieldGroup>> entry : groups.entrySet()) {
            fieldGroupMap.get(entry.getKey()).setSubFieldGroups(entry.getValue());
        }


        return new ArrayList<>(fieldGroupMap.values());

    }

    @Override
    public List<GroupedFields<Field>> getFlatModel() {
        List<Group> groups = getGroups();
        List<GroupedFields<Field>> groupedFieldsList = new ArrayList<>();

        if (groups != null) {
            for (Group group : groups) {
                GroupedFields<Field> groupedFields = new GroupedFields<>();

                groupedFields.setGroup(group);
                groupedFields.setFields(getFieldsByGroup(group.getId()));

                groupedFieldsList.add(groupedFields);
            }
        }

        return groupedFieldsList;
    }

    @Override
    public List<Group> getGroups() {
        return readGroups(directory + "/" + FILENAME_GROUPS);
    }

    @Override
    public List<Field> getFields() { // TODO: refactor
        List<Field> allFields = readFields(directory + "/" + FILENAME_FIELDS);

        Map<Integer, Field> fieldMap = new HashMap<>();
        for (Field field : allFields) {
            fieldMap.put(field.getId(), field);
        }
        for (Field f : allFields) {
            if (f.getForm().getDependsOn() != null) {
                // f -> dependsOn
                FieldIdName dependsOn = f.getForm().getDependsOn();

                // affectingField is the field that 'f' dependsOn
                // meaning the field that affects 'f'
                Field affectingField = fieldMap.get(dependsOn.getId());
                dependsOn.setName(affectingField.getName());

                FieldIdName affects = new FieldIdName(f.getId(), f.getName());
                if (affectingField.getForm().getAffects() == null) {
                    affectingField.getForm().setAffects(new ArrayList<>());
                }
                affectingField.getForm().getAffects().add(affects);

            }
        }

        for (Field field : allFields) {
            String accessPath = field.getName();
            Field parentField = field;
            int counter = 0;
            while (parentField.getParent() != null && counter < allFields.size()) {
                counter++;
                accessPath = String.join(".", parentField.getParent(), accessPath);
                for (Field temp : allFields) {
                    if (temp.getName().equals(parentField.getParent())) {
                        parentField = temp;
                        break;
                    }
                }
            }
            if (counter >= allFields.size()) {
                throw new RuntimeException("The json model located at '" + directory + "/" + FILENAME_FIELDS +
                        "' contains errors in the 'parent' fields...\nPlease fix it and try again.");
            }

            // FIXME: implement this properly. Check if infraService is needed or not and decide what to do.
            accessPath = accessPath.replaceFirst("\\w+\\.", ""); // used to remove infraService entry

            field.setAccessPath(accessPath);
        }
        return allFields;
    }


    public List<Field> getFieldsByGroup(String groupId) {
        List<Field> allFields = getFields();

        return allFields
                .stream()
                .filter(field -> field.getForm() != null)
                .filter(field -> field.getForm().getGroup() != null)
                .filter(field -> field.getForm().getGroup().equals(groupId))
                .collect(Collectors.toList());
    }

    @Override
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

    @Override
    @Cacheable(value = CACHE_FOR_UI)
    public Map<String, List<eu.einfracentral.dto.Value>> getControlValuesByType() {
        Map<String, List<eu.einfracentral.dto.Value>> controlValues = new HashMap<>();
        List<eu.einfracentral.dto.Value> values;
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);

        // add providers
        values = this.providerService.getAll(ff, null).getResults()
                .parallelStream()
                .map(value -> new eu.einfracentral.dto.Value(value.getId(), value.getProvider().getName()))
                .collect(Collectors.toList());
        controlValues.put("Provider", values);

        // add services
        ff.addFilter("active", true);
        ff.addFilter("latest", true);
        values = this.infraServiceService.getAll(ff, null).getResults()
                .parallelStream()
                .map(value -> new eu.einfracentral.dto.Value(value.getId(), value.getService().getName()))
                .collect(Collectors.toList());
        controlValues.put("Service", values);


        // add all vocabularies
        for (Map.Entry<Vocabulary.Type, List<Vocabulary>> entry : vocabularyService.getAllVocabulariesByType().entrySet()) {
            values = entry.getValue()
                    .parallelStream()
                    .map(v -> new eu.einfracentral.dto.Value(v.getId(), v.getName(), v.getParentId()))
                    .collect(Collectors.toList());
            controlValues.put(entry.getKey().getKey(), values);
        }

        return controlValues;
    }

    @Override
    @Cacheable(cacheNames = CACHE_FOR_UI, key = "#type")
    public List<eu.einfracentral.dto.Value> getControlValuesByType(String type) {
        List<eu.einfracentral.dto.Value> values = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        if (Vocabulary.Type.exists(type)) {
            List<Vocabulary> vocabularies = this.vocabularyService.getByType(Vocabulary.Type.fromString(type));
            vocabularies.forEach(v -> values.add(new eu.einfracentral.dto.Value(v.getId(), v.getName())));
        } else if (type.equalsIgnoreCase("provider")) {
            List<ProviderBundle> providers = this.providerService.getAll(ff, null).getResults();
            providers.forEach(v -> values.add(new eu.einfracentral.dto.Value(v.getId(), v.getProvider().getName())));
        } else if (type.equalsIgnoreCase("service") || type.equalsIgnoreCase("resource")) {
            List<InfraService> services = this.infraServiceService.getAll(ff, null).getResults();
            services.forEach(v -> values.add(new eu.einfracentral.dto.Value(v.getId(), v.getService().getName())));
        }
        return values;
    }
}
