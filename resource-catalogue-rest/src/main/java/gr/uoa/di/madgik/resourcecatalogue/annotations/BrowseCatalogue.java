package gr.uoa.di.madgik.resourcecatalogue.annotations;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Parameter(in = ParameterIn.QUERY, name = "catalogue", description = "Catalogue ID")
public @interface BrowseCatalogue {
}
