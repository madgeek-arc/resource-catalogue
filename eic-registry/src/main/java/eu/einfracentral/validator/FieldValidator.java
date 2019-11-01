package eu.einfracentral.validator;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.manager.IndicatorManager;
import eu.einfracentral.registry.manager.ProviderManager;
import eu.einfracentral.registry.service.FunderService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.VocabularyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collection;

@Service
public class FieldValidator {

    private final VocabularyService vocabularyService;
    private final FunderService funderService;
    private final ProviderManager providerService;
    private final InfraServiceService<InfraService, InfraService> infraServiceService;
    private final IndicatorManager indicatorService;


    @Autowired
    public FieldValidator(VocabularyService vocabularyService,
                          FunderService funderService,
                          ProviderManager providerService,
                          InfraServiceService<InfraService, InfraService> infraServiceService,
                          IndicatorManager indicatorService) {
        this.vocabularyService = vocabularyService;
        this.funderService = funderService;
        this.providerService = providerService;
        this.infraServiceService = infraServiceService;
        this.indicatorService = indicatorService;
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
        if (o == null) {
            throw new ValidationException("Attempt to validate null object..");
        }

        // check if FieldValidation annotation exists
        Annotation vocabularyValidation = field.getAnnotation(VocabularyValidation.class);
        Annotation annotation = field.getAnnotation(FieldValidation.class);
        if (vocabularyValidation != null && annotation == null) {
            annotation = vocabularyValidation.annotationType().getAnnotation(FieldValidation.class);
        }

        if (annotation != null) {

            FieldValidation validationAnnotation = (FieldValidation) annotation;

            field.setAccessible(true);

            Object fieldValue = field.get(o);
            Class clazz = null;
            if (fieldValue != null) {
                clazz = field.get(o).getClass();
            }

            if (!validationAnnotation.nullable() && isNullOrEmpty(fieldValue, clazz)) {
                throw new ValidationException("Field '" + field.getName() + "' is mandatory.");
            }

            validateMaxLength(field, fieldValue, validationAnnotation);

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
                    } else if (Funder.class.equals(annotation.idClass())
                            && funderService.get(o.toString()) == null) {
                        throw new ValidationException(
                                String.format("Field '%s' should contain the ID of an existing Funder",
                                        field.getName()));
                    } else if (InfraService.class.equals(annotation.idClass())
                            && infraServiceService.get(o.toString()) == null) {
                        throw new ValidationException(
                                String.format("Field '%s' should contain the ID of an existing Service",
                                        field.getName()));
                    } else if (Indicator.class.equals(annotation.idClass())
                            && indicatorService.get(o.toString()) == null) {
                        throw new ValidationException(
                                String.format("Field '%s' should contain the ID of an existing Service",
                                        field.getName()));
                    }
                } catch (ResourceException e) {
                    throw new ValidationException(
                            String.format("%s with ID '%s' does not exist. Found in field '%s'",
                                    annotation.idClass().getSimpleName(), o.toString(), field.getName()));
                }
            }
        }
    }
}
