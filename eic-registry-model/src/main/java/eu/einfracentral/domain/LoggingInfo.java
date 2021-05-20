package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;
import java.util.List;

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

//    @XmlElement(defaultValue = "null")
    private List<AuditingInfo> audit;

//    @XmlElement(defaultValue = "null")
    private List<AuditingInfo> update;

//    @XmlElement(defaultValue = "null")
    private List<AuditingInfo> onboarding;

    public LoggingInfo() {
    }

    public LoggingInfo(LoggingInfo loggingInfo) {
        this.date = loggingInfo.getDate();
        this.userEmail = loggingInfo.getUserEmail();
        this.userRole = loggingInfo.getUserRole();
        this.type = loggingInfo.getType();
        this.audit = loggingInfo.getAudit();
        this.update = loggingInfo.getUpdate();
        this.onboarding = loggingInfo.getOnboarding();
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

    public static LoggingInfo createLoggingInfo(String userEmail, String role){
        LoggingInfo ret = new LoggingInfo();
        ret.setDate(now());
        ret.setType(Types.REGISTERED.getKey());
        ret.setUserEmail(userEmail);
        ret.setUserRole(role);
        return ret;
    }

    public static LoggingInfo updateLoggingInfo(String userEmail, String role, String type){
        LoggingInfo ret = new LoggingInfo();
        ret.setDate(now());
        ret.setType(type);
        ret.setUserEmail(userEmail);
        ret.setUserRole(role);
        return ret;
    }

    public static LoggingInfo updateLoggingInfo(String type){
        LoggingInfo ret = new LoggingInfo();
        ret.setDate(now());
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
                ", audit=" + audit +
                ", update=" + update +
                ", onboarding=" + onboarding +
                '}';
    }

    public static String now(){
        return String.valueOf(System.currentTimeMillis());
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

    public List<AuditingInfo> getAudit() {
        return audit;
    }

    public void setAudit(List<AuditingInfo> audit) {
        this.audit = audit;
    }

    public List<AuditingInfo> getUpdate() {
        return update;
    }

    public void setUpdate(List<AuditingInfo> update) {
        this.update = update;
    }

    public List<AuditingInfo> getOnboarding() {
        return onboarding;
    }

    public void setOnboarding(List<AuditingInfo> onboarding) {
        this.onboarding = onboarding;
    }
}
