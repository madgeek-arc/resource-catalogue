package gr.uoa.di.madgik.resourcecatalogue.validators;

import gr.uoa.di.madgik.resourcecatalogue.annotation.*;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FieldValidator {

    private static final Logger logger = LoggerFactory.getLogger(FieldValidator.class);

    private final VocabularyService vocabularyService;
    private final ProviderManager providerService;
    private final ServiceBundleService serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final CatalogueService catalogueService;
    private final InteroperabilityRecordService interoperabilityRecordService;

    private static final String MANDATORY_FIELD = "Field '%s' is mandatory.";
    private static final String NULL_OBJECT = "Attempt to validate null object..";

    private Deque<String> validationLocation;

    @Autowired
    public FieldValidator(VocabularyService vocabularyService,
                          ProviderManager providerService,
                          @Lazy ServiceBundleService serviceBundleService,
                          @Lazy TrainingResourceService trainingResourceService,
                          @Lazy CatalogueService catalogueService,
                          @Lazy InteroperabilityRecordService interoperabilityRecordService) {
        this.vocabularyService = vocabularyService;
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.catalogueService = catalogueService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.trainingResourceService = trainingResourceService;
    }

    private String getCurrentLocation() {
        return validationLocation.stream().filter(Objects::nonNull).collect(Collectors.joining("->"));
    }

    public void validate(Object o) throws IllegalAccessException {
        validationLocation = new ArrayDeque<>();
        validateFields(o);
        if (o.getClass().getSuperclass() != null && (o.getClass().getSuperclass().getCanonicalName().contains("gr.uoa.di.madgik.resourcecatalogue.domain.Bundle"))) {
            try {
                Field payload = Bundle.class.getDeclaredField("payload");
                payload.setAccessible(true);
                o = payload.get(o);
            } catch (NoSuchFieldException e) {
                logger.error("Could not find field 'payload'", e);
            }
            validateFields(o);
        }
    }

    private void validateFields(Object o) throws IllegalAccessException {
        if (o == null) {
            throw new ValidationException(NULL_OBJECT);
        }

        // get declared fields of class
        List<Field> declaredFields = new ArrayList<>(Arrays.asList(o.getClass().getDeclaredFields()));
        if (o instanceof ServiceBundle) {
            declaredFields.addAll(Arrays.asList(o.getClass().getSuperclass().getDeclaredFields()));
        }
        if (o instanceof TrainingResourceBundle) {
            declaredFields.addAll(Arrays.asList(o.getClass().getSuperclass().getDeclaredFields()));
        }
        if (o instanceof InteroperabilityRecordBundle) {
            declaredFields.addAll(Arrays.asList(o.getClass().getSuperclass().getDeclaredFields()));
        }

        // validate every field
        for (Field field : declaredFields) {
            validationLocation.addLast(field.getName());
            validateField(field, o);
            validationLocation.removeLast();
        }
    }

    private void validateField(Field field, Object o) throws IllegalAccessException {
        if (o == null) { // parent object here
            throw new ValidationException(NULL_OBJECT);
        }

        // email validation
        if (field.getAnnotation(EmailValidation.class) != null) {
            validateEmail(field, o, field.getAnnotation(EmailValidation.class));
            return;
        }

        // phone validation
        if (field.getAnnotation(PhoneValidation.class) != null) {
            validatePhone(field, o, field.getAnnotation(PhoneValidation.class));
            return;
        }

        // check if FieldValidation annotation exists
        Annotation vocabularyValidation = field.getAnnotation(VocabularyValidation.class);
        Annotation geoLocationVocValidation = field.getAnnotation(GeoLocationVocValidation.class);
        Annotation annotation = field.getAnnotation(FieldValidation.class);
        if (vocabularyValidation != null && annotation == null) {
            annotation = vocabularyValidation.annotationType().getAnnotation(FieldValidation.class);
        }
        // region/countries validation
        if (geoLocationVocValidation != null && annotation == null) {
            annotation = geoLocationVocValidation.annotationType().getAnnotation(FieldValidation.class);
        }

        validateField(field, o, (FieldValidation) annotation);
    }

    private void validatePhone(Field field, Object o, PhoneValidation annotation) throws IllegalAccessException {
        field.setAccessible(true);
        o = field.get(o);
        if (annotation.nullable() && (o == null || o.equals(""))) {
            return;
        } else if (o == null) {
            throw new ValidationException(String.format(MANDATORY_FIELD, getCurrentLocation()));
        }
        Pattern phonePattern = Pattern.compile("^(((\\+)|(00))\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}$");
        if (!phonePattern.matcher(o.toString()).matches()) {
            throw new ValidationException(String.format("The phone you provided [%s] is not valid. Found in field [%s]", o, getCurrentLocation()));
        }
    }

    private void validateEmail(Field field, Object o, EmailValidation annotation) throws IllegalAccessException {
        field.setAccessible(true);
        o = field.get(o);
        if (annotation.nullable() && (o == null || o.equals(""))) {
            return;
        } else if (o == null) {
            throw new ValidationException(String.format(MANDATORY_FIELD, getCurrentLocation()));
        }
        EmailValidator emailValidator = EmailValidator.getInstance();
        if (!emailValidator.isValid(o.toString())) {
            throw new ValidationException(String.format("Email [%s] is not valid. Found in field [%s]", o, getCurrentLocation()));
        }
    }

    private void validateField(Field field, Object o, FieldValidation annotation) throws IllegalAccessException {
        if (o == null) {
            throw new ValidationException(NULL_OBJECT);
        }

        if (annotation != null) {

            field.setAccessible(true);

            Object fieldValue = field.get(o);
            Class clazz = null;
            if (fieldValue != null) {
                clazz = field.get(o).getClass();

                if (Collection.class.isAssignableFrom(fieldValue.getClass())) {
                    removeNullOrEmptyEntries((Collection) fieldValue);
                    field.set(o, fieldValue);
                } else if ((String.class.isAssignableFrom(fieldValue.getClass())
                        || URL.class.isAssignableFrom(fieldValue.getClass()))
                        && "".equals(fieldValue)) {
                    fieldValue = null;
                    field.set(o, fieldValue);
                }
            }

            if (!annotation.nullable() && isNullOrEmpty(fieldValue, clazz)) {
                throw new ValidationException(String.format(MANDATORY_FIELD, getCurrentLocation()));
            }

            validateMaxLength(field, fieldValue, annotation);
            validateUrlValidity(field, fieldValue);
            validateDuplicates(field, fieldValue);

            if (annotation.containsId()) {
                validateIds(field, fieldValue, annotation);
            } else if (fieldValue != null && fieldValue.getClass().getCanonicalName().startsWith("gr.uoa.di.madgik.")) {
                validateFields(fieldValue);
            } else if (fieldValue != null && Collection.class.isAssignableFrom(fieldValue.getClass())) {
                for (Object entry : ((Collection) fieldValue)) {
                    validateFields(entry);
                }
            }
        }
    }

    private boolean isNullOrEmpty(Object o, Class clazz) {
        if (o == null)
            return true;
        else if (String.class.equals(clazz) && "".equals((String) o))
            return true;
        else if (URL.class.equals(clazz) && "".equals(o.toString()))
            return true;
        else if (Collection.class.isAssignableFrom(clazz) && ((Collection) o).isEmpty())
            return true;
        return false;
    }

    private void validateMaxLength(Field field, Object o, FieldValidation annotation) {
        if (annotation.maxLength() > 0 && o != null) {
            Class clazz = o.getClass();
            if (String.class.equals(clazz) || URL.class.equals(clazz)) {
                String val = (String) o;
                if (val.length() > annotation.maxLength()) {
                    throw new ValidationException(String.format("Max length for field '%s' is %s characters.",
                            field.getName(), annotation.maxLength()));
                }
            }
            // if given object is a collection, apply 'validateMaxLength' to all entries
            else if (Collection.class.isAssignableFrom(clazz) && !((Collection) o).isEmpty()) {
                for (Object entry : ((Collection) o)) {
                    validateMaxLength(field, entry, annotation);
                }
            }
        }
    }

    private void validateUrlValidity(Field field, Object o) {
        if (o != null) {
            Class clazz = o.getClass();
            if (URL.class.equals(clazz)) {
                URL url = (URL) o;
                validateUrl(field, url);
            } else if (ArrayList.class.equals(clazz) && !((ArrayList) o).isEmpty() && URL.class.equals(((ArrayList) o).get(0).getClass())) {
                for (int i = 0; i < ((ArrayList) o).size(); i++) {
                    URL url = (URL) ((ArrayList) o).get(i);
                    validateUrl(field, url);
                }
            }
        }
    }

    public void validateUrl(Field field, URL urlForValidation) {
        HttpsTrustManager.allowAllSSL();
        HttpURLConnection huc;
        int statusCode = 0;

        try {
            // replace spaces with %20
            if (urlForValidation.toString().contains(" ")) {
                urlForValidation = new URL(urlForValidation.toString().replaceAll("\\s", "%20"));
            }

            // open connection and get response code
            huc = (HttpURLConnection) urlForValidation.openConnection();
            assert huc != null;
            huc.setRequestMethod("HEAD");
            huc.setConnectTimeout(5000);
            statusCode = huc.getResponseCode();
        } catch (java.net.SocketTimeoutException e) {
            throw new ValidationException("URI provided is not valid, or takes too long to load. Found in field " + field.getName());
        } catch (IOException e) {
            logger.trace(e.getMessage());
        }

//        if (statusCode != 200 && statusCode != 301 && statusCode != 302 && statusCode != 308
//                && statusCode != 403 && statusCode != 405 && statusCode != 503) {
//            if (field == null) {
//                throw new ValidationException(String.format("The URL '%s' you provided is not valid.", urlForValidation));
//            } else {
//                throw new ValidationException(String.format("The URL '%s' you provided is not valid. Found in field '%s'", urlForValidation, field.getName()));
//            }
//        }
    }

    // TODO: find a better way to get resources by id
    private void validateIds(Field field, Object o, FieldValidation annotation) {
        if (o != null && annotation.containsId()) {
            if (Collection.class.isAssignableFrom(o.getClass())) {
                for (Object entry : ((Collection) o)) {
                    validateIds(field, entry, annotation);
                }
            } else if (String.class.equals(o.getClass())) {
                try {
                    if (annotation.containsResourceId()) {
                        ServiceBundle serviceBundle = serviceBundleService.getOrElseReturnNull(o.toString());
                        TrainingResourceBundle trainingResourceBundle = trainingResourceService.getOrElseReturnNull(o.toString());
                        if (serviceBundle == null && trainingResourceBundle == null) {
                            throw new ValidationException(
                                    String.format("Field '%s' should ONLY contain the ID of an existing Service " +
                                            "or Training Resource", field.getName()));
                        }
                    } else if (Vocabulary.class.equals(annotation.idClass())) {
                        Vocabulary voc = vocabularyService.get(o.toString());
                        VocabularyValidation vocabularyValidation = field.getAnnotation(VocabularyValidation.class);
                        GeoLocationVocValidation geoLocationVocValidation = field.getAnnotation(GeoLocationVocValidation.class);
                        if (vocabularyValidation != null) {
                            if (voc == null || Vocabulary.Type.fromString(voc.getType()) != vocabularyValidation.type()) {
                                throw new ValidationException(
                                        String.format("Field '%s' should contain the ID of a type '%s' Vocabulary",
                                                field.getName(), vocabularyValidation.type()));
                            }
                        }
                        // region/countries validation
                        if (geoLocationVocValidation != null) {
                            if (voc == null || (Vocabulary.Type.fromString(voc.getType()) != geoLocationVocValidation.region()
                                    && Vocabulary.Type.fromString(voc.getType()) != geoLocationVocValidation.country())) {
                                throw new ValidationException(
                                        String.format("Field '%s' should contain the ID of either one of the types '%s' or '%s' Vocabularies",
                                                field.getName(), geoLocationVocValidation.region(), geoLocationVocValidation.country()));
                            }
                        }
                    } else if (Provider.class.equals(annotation.idClass())
                            && providerService.get(o.toString()) == null) { //FIXME catalogueID
                        throw new ValidationException(
                                String.format("Field '%s' should contain the ID of an existing Provider",
                                        field.getName()));
                    } else if ((gr.uoa.di.madgik.resourcecatalogue.domain.Service.class.equals(annotation.idClass())
                            || ServiceBundle.class.equals(annotation.idClass()))
                            && serviceBundleService.get(o.toString()) == null) {
                        throw new ValidationException(
                                String.format("Field '%s' should contain the ID of an existing Service",
                                        field.getName()));
                    } else if ((TrainingResource.class.equals(annotation.idClass())
                            || TrainingResourceBundle.class.equals(annotation.idClass()))
                            && trainingResourceService.get(o.toString()) == null) {
                        throw new ValidationException(
                                String.format("Field '%s' should contain the ID of an existing Training Resource",
                                        field.getName()));
                    } else if ((Catalogue.class.equals(annotation.idClass())
                            || CatalogueBundle.class.equals(annotation.idClass()))
                            && catalogueService.get(o.toString()) == null) {
                        throw new ValidationException(
                                String.format("Field '%s' should contain the ID of an existing Catalogue",
                                        field.getName()));
                    } else if ((InteroperabilityRecord.class.equals(annotation.idClass())
                            || InteroperabilityRecordBundle.class.equals(annotation.idClass()))
                            && interoperabilityRecordService.get(o.toString()) == null) {
                        throw new ValidationException(
                                String.format("Field '%s' should contain the ID of an existing InteroperabilityRecord",
                                        field.getName()));
                    }
                } catch (ResourceException | ResourceNotFoundException e) {
                    throw new ValidationException(
                            String.format("%s with ID '%s' does not exist. Found in field '%s'",
                                    annotation.idClass().getSimpleName(), o.toString(), field.getName()));
                }
            }
        }
    }

    private void removeNullOrEmptyEntries(Collection collection) {
        if (collection != null) {
            for (Iterator i = collection.iterator(); i.hasNext(); ) {
                Object entry = i.next();
                // if elements are of type String or URL
                if (entry != null) {
                    if ((String.class.isAssignableFrom(entry.getClass())
                            || URL.class.isAssignableFrom(entry.getClass()))
                            && "".equals(entry.toString())) {
                        i.remove(); // remove empty string entries ("")
                    }
                } else {
                    i.remove(); // remove null entries
                }
            }
        }
    }

    private void validateDuplicates(Field field, Object o) {
        Set<String> duplicateEntries = new HashSet<>();
        String subField = field.toString().substring(field.toString().lastIndexOf(".") + 1);
        if (o != null) {
            Class clazz = o.getClass();
            if (ArrayList.class.equals(clazz)) {
                for (int i = 0; i < ((ArrayList) o).size(); i++) {
                    if (!duplicateEntries.add(((ArrayList) o).get(i).toString())) {
                        throw new ValidationException(String.format("Duplicate value found '%s' on field '%s'", ((ArrayList) o).get(i).toString(), subField));
                    }
                }
            }
        }
    }
}
