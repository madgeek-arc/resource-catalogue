package eu.einfracentral.domain;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class LoggingInfo {

    @XmlElement(defaultValue = "null")
    private String date;

    @XmlElement(defaultValue = "null")
    private String userEmail;

    @XmlElement(defaultValue = "null")
    private String userFullName;

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
        this.userFullName = loggingInfo.getUserFullName();
        this.userRole = loggingInfo.getUserRole();
        this.type = loggingInfo.getType();
        this.comment = loggingInfo.getComment();
        this.actionType = loggingInfo.getActionType();
    }

    public enum Types {
        ONBOARD("onboard"),
        UPDATE("update"),
        AUDIT("audit"),
        DRAFT("draft");

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
        // Onboard
        REGISTERED("registered"),
        APPROVED("approved"),
        VALIDATED("validated"),
        REJECTED("rejected"),
        // Update
        UPDATED("updated"),
        UPDATED_VERSION("updated version"),
        DELETED("deleted"),
        ACTIVATED("activated"),
        DEACTIVATED("deactivated"),
        // Audit
        VALID("valid"),
        INVALID("invalid"),
        // Draft
        CREATED("drafted");

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

    public static LoggingInfo createLoggingInfoEntry(String userEmail, String userFullName, String role, String type, String actionType){
        LoggingInfo ret = new LoggingInfo();
        ret.setDate(String.valueOf(System.currentTimeMillis()));
        ret.setType(type);
        ret.setActionType(actionType);
        ret.setUserEmail(userEmail);
        ret.setUserFullName(userFullName);
        ret.setUserRole(role);
        return ret;
    }

    public static LoggingInfo createLoggingInfoEntry(String userEmail, String userFullName, String role, String type, String actionType, String comment){
        LoggingInfo ret = new LoggingInfo();
        ret.setDate(String.valueOf(System.currentTimeMillis()));
        ret.setType(type);
        ret.setActionType(actionType);
        ret.setUserEmail(userEmail);
        ret.setUserFullName(userFullName);
        ret.setUserRole(role);
        ret.setComment(comment);
        return ret;
    }

    public static LoggingInfo systemUpdateLoggingInfo(String actionType){
        LoggingInfo ret = new LoggingInfo();
        ret.setDate(String.valueOf(System.currentTimeMillis()));
        ret.setType(Types.UPDATE.getKey());
        ret.setActionType(actionType);
        ret.setUserRole("system");
        return ret;
    }

    // find the AUDIT_STATE of a specific Provider or Resource through its LoggingInfo list
    //TODO: decide if we need this method
    public static String createAuditVocabularyStatuses(List<LoggingInfo> loggingInfoList){
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
        Boolean hasBeenAudited = false;
        Boolean hasBeenUpdatedAfterAudit = false;
        String auditActionType = "";
        int auditIndex = -1;
        for (LoggingInfo loggingInfo : loggingInfoList){
            auditIndex++;
            if (loggingInfo.getType().equals(Types.AUDIT.getKey())){
                hasBeenAudited = true;
                auditActionType = loggingInfo.getActionType();
                break;
            }
        }
        // if we have an update after the audit
        if (hasBeenAudited){
            for (int i=0; i<auditIndex; i++){
                if (loggingInfoList.get(i).getType().equals(Types.UPDATE.getKey())){
                    hasBeenUpdatedAfterAudit = true;
                    break;
                }
            }
        }
        String ret = null;
        if (!hasBeenAudited){
            ret = "Not Audited";
        } else if (hasBeenAudited && !hasBeenUpdatedAfterAudit){
            if (auditActionType.equals(ActionType.INVALID.getKey())){
                ret = "Invalid and not updated";
            }
        } else if (hasBeenAudited && hasBeenUpdatedAfterAudit){
            if (auditActionType.equals(ActionType.INVALID.getKey())){
                ret = "Invalid and updated";
            }
        } else{
            ret = "Valid";
        }
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

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
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

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
}
