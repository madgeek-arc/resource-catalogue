/**
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.annotations;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Parameters({
        @Parameter(in = ParameterIn.QUERY, name = "keyword", description = "Keyword to refine the search", content = @Content(schema = @Schema(type = "string"))),
        @Parameter(in = ParameterIn.QUERY, name = "from", description = "Starting index in the result set", content = @Content(schema = @Schema(type = "string", defaultValue = "0"))),
        @Parameter(in = ParameterIn.QUERY, name = "quantity", description = "Quantity to be fetched", content = @Content(schema = @Schema(type = "string", defaultValue = "10"))),
        @Parameter(in = ParameterIn.QUERY, name = "sort", description = "Field to use for sorting", content = @Content(schema = @Schema(type = "string"))),
        @Parameter(in = ParameterIn.QUERY, name = "order", description = "Order of results", content = @Content(schema = @Schema(type = "string", defaultValue = "asc", allowableValues = {"asc", "desc"})))
})
public @interface Browse {
}