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
        private String clientKey;

        /**
         * Client certificate.
         */
        private String clientCert;

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
    }
}

