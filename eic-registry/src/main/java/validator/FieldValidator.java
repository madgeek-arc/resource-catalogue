package validator;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.exception.ValidationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collection;

public class FieldValidator {

    public static void validateFields(Object o) throws IllegalAccessException {

        // get declared fields of class
        Field[] declaredFields = o.getClass().getDeclaredFields();

        // validate every field
        for(Field field : declaredFields) {
            validateField(field, o);
        }
    }

    public static void validateField(Field field, Object o) throws IllegalAccessException {
        // check if FieldValidation annotation exists
        Annotation annotation = field.getAnnotation(FieldValidation.class);
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
        }
    }

    public static boolean isNullOrEmpty(Object o, Class clazz) {
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

    public static void validateMaxLength(Field field, Object o, FieldValidation annotation) {
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

    private FieldValidator() {}
}
