package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class BundleStatus {

    @XmlElement(defaultValue = "null")
    private String actionType;

    @XmlElement(defaultValue = "null")
    private String date;

    @XmlElement(defaultValue = "null")
    private String comment;

    @XmlElement(defaultValue = "null")
    private String user;

    public BundleStatus() {
    }

    public BundleStatus(String actionType, String date, String comment, String user) {
        this.actionType = actionType;
        this.date = date;
        this.comment = comment;
        this.user = user;
    }

    public enum ActionTypes {
        VALID("valid"),
        INVALID("invalid"),
        UPDATED_INVALID("updated invalid");

        private final String actionType;

        ActionTypes(final String actionType) {
            this.actionType = actionType;
        }

        public String getKey() {
            return actionType;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static BundleStatus.ActionTypes fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(BundleStatus.ActionTypes.values())
                    .filter(v -> v.actionType.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
