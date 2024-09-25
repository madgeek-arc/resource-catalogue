package gr.uoa.di.madgik.resourcecatalogue.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldValidation {

    boolean nullable() default false;

    int maxLength() default 0;

    boolean containsId() default false;

    Class idClass() default Object.class;

    boolean containsResourceId() default false;
}
