/*
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

package gr.uoa.di.madgik.resourcecatalogue.domain;


import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Objects;

public class LoggingInfo {

    private String date;

    private String userEmail;

    private String userFullName;

    private String userRole;

    private String type;

    private String comment;

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
        DRAFT("draft"),
        MOVE("move");

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
        REJECTED("rejected"),
        // Update
        UPDATED("updated"),
        UPDATED_VERSION("updated version"),
        ACTIVATED("activated"),
        DEACTIVATED("deactivated"),
        SUSPENDED("suspended"),
        UNSUSPENDED("unsuspended"),
        // Audit
        VALID("valid"),
        INVALID("invalid"),
        // Draft
        CREATED("drafted"),
        // Resource change Provider
        MOVED("moved");

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

    public static LoggingInfo createLoggingInfoEntry(Authentication auth, String userRole, String type, String actionType) {
        return createLoggingInfoEntry(auth, userRole, type, actionType, null);
    }

    public static LoggingInfo createLoggingInfoEntry(Authentication auth, String userRole, String type, String actionType,
                                                     String comment) {
        validateLoggingInfoEnums(type, actionType);
        LoggingInfo ret = new LoggingInfo();
        User user = Objects.requireNonNull(User.of(auth));
        ret.setDate(String.valueOf(System.currentTimeMillis()));
        ret.setType(type);
        ret.setActionType(actionType);
        ret.setUserEmail(user.getEmail());
        ret.setUserFullName(user.getFullName());
        ret.setUserRole(userRole);
        ret.setComment(comment);
        return ret;
    }

    private static void validateLoggingInfoEnums(String type, String actionType) {
        if (type == null || actionType == null) {
            throw new IllegalArgumentException("LoggingInfo Type and ActionType cannot be null");
        }
        validateLoggingInfoType(type);
        validateLoggingInfoActionType(actionType);
    }

    private static void validateLoggingInfoType(String type) {
        boolean isTypeValid = Arrays.stream(Types.values())
                .map(Types::getKey)
                .anyMatch(type::equals);
        if (!isTypeValid) {
            throw new IllegalArgumentException("Invalid type: " + type);
        }
    }

    private static void validateLoggingInfoActionType(String actionType) {
        boolean isActionTypeValid = Arrays.stream(ActionType.values())
                .map(ActionType::getKey)
                .anyMatch(actionType::equals);
        if (!isActionTypeValid) {
            throw new IllegalArgumentException("Invalid action type: " + actionType);
        }
    }

    public static LoggingInfo systemUpdateLoggingInfo(String actionType) {
        LoggingInfo ret = new LoggingInfo();
        ret.setDate(String.valueOf(System.currentTimeMillis()));
        ret.setType(Types.UPDATE.getKey());
        ret.setActionType(actionType);
        ret.setUserRole("system");
        ret.setUserFullName("system");
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
        this.userEmail = userEmail != null ? userEmail.toLowerCase() : null;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoggingInfo that)) return false;
        return Objects.equals(date, that.date) && Objects.equals(userEmail, that.userEmail) && Objects.equals(userFullName, that.userFullName) && Objects.equals(userRole, that.userRole) && Objects.equals(type, that.type) && Objects.equals(comment, that.comment) && Objects.equals(actionType, that.actionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, userEmail, userFullName, userRole, type, comment, actionType);
    }
}
