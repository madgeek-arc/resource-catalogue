package eu.einfracentral.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldValidation {

    boolean nullable() default false;

    boolean multivalued() default false; // TODO: probably remove

    int maxLength() default 0;
}
