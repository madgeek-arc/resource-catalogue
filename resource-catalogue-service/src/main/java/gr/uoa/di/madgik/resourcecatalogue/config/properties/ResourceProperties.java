/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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
     * Endpoints the PID should resolve to on our node's userspace/search UI (optional). Each entry is
     * either a plain URL (the PID is appended raw, e.g. {@code endpoint/prefix/suffix}) or a template
     * containing {@code {pid}} (raw PID substituted) or {@code {encodedPid}} (PID with its internal
     * "/" percent-encoded, for endpoints that expect the whole PID as a single path segment).
     */
    private List<String> resolveEndpoints;

    /**
     * The FDO kernel information fields (FdoType/FdoProfile/FdoData/FdoVersion) this
     * resource type's PID records are registered with.
     */
    @NestedConfigurationProperty
    private Fdo fdo;

    /**
     * The path segment this resource type is exposed under on the federated search aggregator
     * (e.g. "services" for {@code https://federatedsearch.service.eosc-beyond.eu/api/federation/services}).
     * Optional: if unset, the federation duplicate-id check is skipped for this resource type.
     */
    private String federationPath;

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

    public Fdo getFdo() {
        return fdo;
    }

    public void setFdo(Fdo fdo) {
        this.fdo = fdo;
    }

    public String getFederationPath() {
        return federationPath;
    }

    public void setFederationPath(String federationPath) {
        this.federationPath = federationPath;
    }

    public PidIssuerConfig getPidIssuer() {
        return pidIssuer;
    }

    public void setPidIssuer(PidIssuerConfig pidIssuer) {
        this.pidIssuer = pidIssuer;
    }

    public static class Fdo {

        /**
         * The FdoType value (HS_ADMIN index 9991).
         */
        private String type;

        /**
         * The handle of the FDO type registry profile this resource type's PID records conform to
         * (HS_ADMIN index 9992).
         */
        private String profile;

        /**
         * The FdoData value (HS_ADMIN index 9993).
         */
        private String data;

        /**
         * The FdoVersion value (HS_ADMIN index 9994).
         */
        private String version;

        public Fdo() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
