package eu.einfracentral.domain;

import java.util.Date;

// FIXME: find a better name for this class
public class Status {

    private String action;
    private Date date;
    private String username;
    private String comment;


    public Status() {
    }

    public Status(String action, Date date, String username, String comment) {
        this.action = action;
        this.date = date;
        this.username = username;
        this.comment = comment;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
