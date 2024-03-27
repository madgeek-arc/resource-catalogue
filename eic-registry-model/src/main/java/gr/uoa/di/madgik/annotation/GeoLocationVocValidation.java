package gr.uoa.di.madgik.annotation;

import gr.uoa.di.madgik.domain.Vocabulary;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@FieldValidation(containsId = true, idClass = Vocabulary.class)
public @interface GeoLocationVocValidation {

    Vocabulary.Type region();

    Vocabulary.Type country();
}
