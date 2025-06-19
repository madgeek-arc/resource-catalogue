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

package gr.uoa.di.madgik.resourcecatalogue.manager.pids;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class PidIssuerConfig {

    /**
     * The PID Issuer url.
     */
    @NotNull
    @NotEmpty
    private String url;

    /**
     * The PID Issuer username.
     */
    @NotNull
    @NotEmpty
    private String user;

    /**
     * The PID Issuer user index.
     */
    @NotNull
    @NotEmpty
    private String userIndex;

    /**
     * The PID Issuer user password (optional - otherwise fill in cert).
     */
    private String password;

    /**
     * The certificate-based authentication configuration of the PID Issuer (optional - otherwise fill in password).
     */
    private IssuerCertificateAuthenticationConfig auth;

    public PidIssuerConfig() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUserIndex() {
        return userIndex;
    }

    public void setUserIndex(String userIndex) {
        this.userIndex = userIndex;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public IssuerCertificateAuthenticationConfig getAuth() {
        return auth;
    }

    public void setAuth(IssuerCertificateAuthenticationConfig auth) {
        this.auth = auth;
    }

    public static class IssuerCertificateAuthenticationConfig {

        /**
         * Client key.
         */
        @NotNull
        @NotEmpty
        private String clientKey;

        /**
         * Client certificate.
         */
        @NotNull
        @NotEmpty
        private String clientCert;

        /**
         * Whether the ssl certificate is self-signed (default: false)
         */
        @NotNull
        private Boolean selfSignedCert = false;

        public IssuerCertificateAuthenticationConfig() {
        }

        public String getClientKey() {
            return clientKey;
        }

        public void setClientKey(String clientKey) {
            this.clientKey = clientKey;
        }

        public String getClientCert() {
            return clientCert;
        }

        public void setClientCert(String clientCert) {
            this.clientCert = clientCert;
        }

        public boolean isSelfSignedCert() {
            return selfSignedCert;
        }

        public void setSelfSignedCert(boolean selfSignedCert) {
            this.selfSignedCert = selfSignedCert;
        }
    }
}
