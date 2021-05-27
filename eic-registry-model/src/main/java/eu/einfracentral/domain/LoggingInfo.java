package eu.einfracentral.domain;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class LoggingInfo {

    @XmlElement(defaultValue = "null")
    private String date;

    @XmlElement(defaultValue = "null")
    private String userEmail;

    @XmlElement(defaultValue = "null")
    private String userRole;

    @XmlElement(defaultValue = "null")
    private String type;

    @XmlElement(defaultValue = "null")
    private String comment;

    @XmlElement(defaultValue = "null")
    private String actionType;

    public LoggingInfo() {
    }

    public LoggingInfo(LoggingInfo loggingInfo) {
        this.date = loggingInfo.getDate();
        this.userEmail = loggingInfo.getUserEmail();
        this.userRole = loggingInfo.getUserRole();
        this.type = loggingInfo.getType();
        this.comment = loggingInfo.getComment();
        this.actionType = loggingInfo.getActionType();
    }

    public enum Types {
        REGISTERED("registered"),
        UPDATED("updated"),
        DELETED("deleted"), // deleted Provider (DELETED) or deleted Service (DELETED)
        ACTIVATED("activated"),
        DEACTIVATED("deactivated"),
        APPROVED("approved"), // approved Provider (APPROVED) or approved Service (APPROVED)
        VALIDATED("validated"), // validated Provider (ST_SUBMISSION)
        REJECTED("rejected"), // rejected Provider (REJECTED) or rejected Service (REJECTED_ST),
        AUDITED("audited"),
        INITIALIZATION("initialization");

        private final String type;

        Types(final String type) {
            this.type = type;
        }

        public String getKey() {
            return type;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static LoggingInfo.Types fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(LoggingInfo.Types.values())
                    .filter(v -> v.type.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }

    public enum ActionType {
        VALID("valid"),
        INVALID("invalid"),
        UPDATED_INVALID("updated invalid");

        private final String actionType;

        ActionType(final String actionType) {
            this.actionType = actionType;
        }

        public String getKey() {
            return actionType;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static ActionType fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(ActionType.values())
                    .filter(v -> v.actionType.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }

    public static LoggingInfo createLoggingInfo(String userEmail, String role){
        LoggingInfo ret = new LoggingInfo();
        ret.setDate(String.valueOf(System.currentTimeMillis()));
        ret.setType(Types.REGISTERED.getKey());
        ret.setUserEmail(userEmail);
        ret.setUserRole(role);
        return ret;
    }

    public static LoggingInfo updateLoggingInfo(String userEmail, String role, String type){
        LoggingInfo ret = new LoggingInfo();
        ret.setDate(String.valueOf(System.currentTimeMillis()));
        ret.setType(type);
        ret.setUserEmail(userEmail);
        ret.setUserRole(role);
        return ret;
    }

    public static LoggingInfo updateLoggingInfo(String type){
        LoggingInfo ret = new LoggingInfo();
        ret.setDate(String.valueOf(System.currentTimeMillis()));
        ret.setType(type);
        ret.setUserEmail("-");
        ret.setUserRole("system");
        return ret;
    }

    // already registered Providers / Resources
    public static LoggingInfo createLoggingInfoForExistingEntry(){
        LoggingInfo ret = new LoggingInfo();
        ret.setDate("1609491600");
        ret.setType(Types.INITIALIZATION.getKey());
        ret.setUserEmail("-");
        ret.setUserRole("system");
        return ret;
    }

    @Override
    public String toString() {
        return "LoggingInfo{" +
                "date='" + date + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", userRole='" + userRole + '\'' +
                ", type='" + type + '\'' +
                ", comment='" + comment + '\'' +
                ", actionType='" + actionType + '\'' +
                '}';
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType.getKey();
    }
}
