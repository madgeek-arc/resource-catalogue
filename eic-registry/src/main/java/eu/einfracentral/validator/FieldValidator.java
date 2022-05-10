package eu.einfracentral.validator;


import eu.einfracentral.annotation.EmailValidation;
import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.PhoneValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.manager.ProviderManager;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.VocabularyService;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class FieldValidator {

    private static final Logger logger = LogManager.getLogger(FieldValidator.class);

    private final VocabularyService vocabularyService;
    private final ProviderManager providerService;
    private final InfraServiceService<InfraService, InfraService> infraServiceService;

    @Autowired
    public FieldValidator(VocabularyService vocabularyService,
                          ProviderManager providerService,
                          InfraServiceService<InfraService, InfraService> infraServiceService) {
        this.vocabularyService = vocabularyService;
        this.providerService = providerService;
        this.infraServiceService = infraServiceService;
    }

    public void validateFields(Object o) throws IllegalAccessException {
        if (o == null) {
            throw new ValidationException("Attempt to validate null object..");
        }

        // get declared fields of class
        Field[] declaredFields = o.getClass().getDeclaredFields();

        // validate every field
        for (Field field : declaredFields) {
            validateField(field, o);
        }
    }

    public void validateField(Field field, Object o) throws IllegalAccessException {
        if (o == null) { // parent object here
            throw new ValidationException("Attempt to validate null object..");
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
        Annotation annotation = field.getAnnotation(FieldValidation.class);
        if (vocabularyValidation != null && annotation == null) {
            annotation = vocabularyValidation.annotationType().getAnnotation(FieldValidation.class);
        }

        validateField(field, o, (FieldValidation) annotation);
    }

    public void validatePhone(Field field, Object o, PhoneValidation annotation) throws IllegalAccessException {
        field.setAccessible(true);
        o = field.get(o);
        if (annotation.nullable() && o == null) {
            return;
        } else if (o == null) {
            throw new ValidationException("Field '" + field.getName() + "' is mandatory.");
        }
        Pattern phonePattern = Pattern.compile("^(((\\+)|(00))\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}$");
        if (!phonePattern.matcher(o.toString()).matches()) {
            throw new ValidationException(String.format("The phone you provided [%s] is not valid. Found in field [%s]", o, field.getName()));
        }
    }

    public void validateEmail(Field field, Object o, EmailValidation annotation) throws IllegalAccessException {
        field.setAccessible(true);
        o = field.get(o);
        if (annotation.nullable() && o == null) {
            return;
        } else if (o == null) {
            throw new ValidationException("Field '" + field.getName() + "' is mandatory.");
        }
        EmailValidator emailValidator = EmailValidator.getInstance();
        if (!emailValidator.isValid(o.toString())) {
            throw new ValidationException(String.format("Email [%s] is not valid. Found in field [%s]", o, field.getName()));
        }
    }

    public void validateField(Field field, Object o, FieldValidation annotation) throws IllegalAccessException {
        if (o == null) {
            throw new ValidationException("Attempt to validate null object..");
        }

        if (annotation != null) {

            FieldValidation validationAnnotation = (FieldValidation) annotation;

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

            if (!validationAnnotation.nullable() && isNullOrEmpty(fieldValue, clazz)) {
                throw new ValidationException("Field '" + field.getName() + "' is mandatory.");
            }

            validateMaxLength(field, fieldValue, validationAnnotation);
            validateUrlValidity(field, fieldValue);
            validateDuplicates(field, fieldValue);

            if (validationAnnotation.containsId()) {
                validateIds(field, fieldValue, validationAnnotation);
            } else if (fieldValue != null && fieldValue.getClass().getCanonicalName().startsWith("eu.einfracentral.")) {
                validateFields(fieldValue);
            } else if (fieldValue != null && Collection.class.isAssignableFrom(fieldValue.getClass())) {
                for (Object entry : ((Collection) fieldValue)) {
                    validateFields(entry);
                }
            }
        }
    }

    public boolean isNullOrEmpty(Object o, Class clazz) {
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

    public void validateMaxLength(Field field, Object o, FieldValidation annotation) {
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

    public void validateUrlValidity(Field field, Object o) {
        if (o != null) {
            Class clazz = o.getClass();
            if (URL.class.equals(clazz)) {
                URL url = (URL) o;
                validateUrl(field, url);
                //FIXME: List<URL> with a single empty String value never enters.
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
            statusCode = huc.getResponseCode();
        } catch (IOException e) {
            logger.trace(e.getMessage());
        }

        if (statusCode != 200 && statusCode != 301 && statusCode != 302 && statusCode != 403 && statusCode != 405 && statusCode != 503) {
            if (field == null) {
                throw new ValidationException(String.format("The URL '%s' you provided is not valid.", urlForValidation));
            } else {
                throw new ValidationException(String.format("The URL '%s' you provided is not valid. Found in field '%s'", urlForValidation, field.getName()));
            }
        }
    }

    // TODO: find a better way to get resources by id
    public void validateIds(Field field, Object o, FieldValidation annotation) {
        if (o != null && annotation.containsId()) {
            if (Collection.class.isAssignableFrom(o.getClass())) {
                for (Object entry : ((Collection) o)) {
                    validateIds(field, entry, annotation);
                }
            } else if (String.class.equals(o.getClass())) {
                try {
                    if (Vocabulary.class.equals(annotation.idClass())) {
                        Vocabulary voc = vocabularyService.get(o.toString());
                        VocabularyValidation vocabularyValidation = field.getAnnotation(VocabularyValidation.class);
                        if (vocabularyValidation != null) {
                            if (voc == null || Vocabulary.Type.fromString(voc.getType()) != vocabularyValidation.type()) {
                                throw new ValidationException(
                                        String.format("Field '%s' should contain the ID of a type '%s' Vocabulary",
                                                field.getName(), vocabularyValidation.type()));
                            }
                        }
                    } else if (Provider.class.equals(annotation.idClass())
                            && providerService.get(o.toString()) == null) {
                        throw new ValidationException(
                                String.format("Field '%s' should contain the ID of an existing Provider",
                                        field.getName()));
                    } else if ((eu.einfracentral.domain.Service.class.equals(annotation.idClass())
                            || InfraService.class.equals(annotation.idClass()))
                            && infraServiceService.get(o.toString()) == null) {
                        throw new ValidationException(
                                String.format("Field '%s' should contain the ID of an existing Service",
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

    public void validateDuplicates(Field field, Object o) {
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
