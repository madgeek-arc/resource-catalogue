package gr.uoa.di.madgik.resourcecatalogue.annotations;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ApiImplicitParams({
        @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
        @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
        @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
        @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
        @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
})
public @interface Browse {
}
