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

    public LoggingInfo() {
    }

    public LoggingInfo(LoggingInfo loggingInfo) {
        this.date = loggingInfo.getDate();
        this.userEmail = loggingInfo.getUserEmail();
        this.userRole = loggingInfo.getUserRole();
        this.type = loggingInfo.getType();
    }

    public enum Types {
        REGISTERED("registered"),
        UPDATED("updated"),
        DELETED("deleted"), // deleted Provider (DELETED) or deleted Service (DELETED)
        ACTIVATED("activated"),
        DEACTIVATED("deactivated"),
        APPROVED("approved"), // approved Provider (APPROVED) or approved Service (APPROVED)
        VALIDATED("validated"), // validated Provider (ST_SUBMISSION)
        REJECTED("rejected"), // rejected Provider (REJECTED) or rejected Service (REJECTED_ST)
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
    public static LoggingInfo createLoggingInfoForExistingEntry(String userEmail, String role){
        LoggingInfo ret = new LoggingInfo();
        ret.setDate("1609491600");
        ret.setType(Types.INITIALIZATION.getKey());
        ret.setUserEmail(userEmail);
        ret.setUserRole(role);
        return ret;
    }

    @Override
    public String toString() {
        return "LoggingInfo{" +
                "date=" + date +
                ", userEmail='" + userEmail + '\'' +
                ", userRole='" + userRole + '\'' +
                ", type='" + type + '\'' +
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
}
