package gr.uoa.di.madgik.resourcecatalogue.config.properties;

import org.springframework.boot.autoconfigure.mail.MailProperties;


public class MailerProperties extends MailProperties {
    private String from;
    private boolean auth = true;
    private boolean ssl = true;

    public String getFrom() {
        return from;
    }

    public MailerProperties setFrom(String from) {
        this.from = from;
        return this;
    }

    public boolean isAuth() {
        return auth;
    }

    public MailerProperties setAuth(boolean auth) {
        this.auth = auth;
        return this;
    }

    public boolean isSsl() {
        return ssl;
    }

    public MailerProperties setSsl(boolean ssl) {
        this.ssl = ssl;
        return this;
    }
}
