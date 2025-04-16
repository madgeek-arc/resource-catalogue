/*
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

package gr.uoa.di.madgik.resourcecatalogue.config.properties;

import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuerConfig;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.List;


@Validated
public class ResourceProperties {

    /**
     * The resource id prefix.
     */
    @NotNull
    @NotEmpty
    private String idPrefix;

    /**
     * Endpoints in which the PID will resolve to (optional).
     */
    private List<String> resolveEndpoints;

    /**
     * The PID Issuer properties (optional).
     */
    @NestedConfigurationProperty
    private PidIssuerConfig pidIssuer;

    public ResourceProperties() {
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    public void setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public List<String> getResolveEndpoints() {
        return resolveEndpoints;
    }

    public void setResolveEndpoints(List<String> resolveEndpoints) {
        this.resolveEndpoints = resolveEndpoints;
    }

    public PidIssuerConfig getPidIssuer() {
        return pidIssuer;
    }

    public void setPidIssuer(PidIssuerConfig pidIssuer) {
        this.pidIssuer = pidIssuer;
    }
}
