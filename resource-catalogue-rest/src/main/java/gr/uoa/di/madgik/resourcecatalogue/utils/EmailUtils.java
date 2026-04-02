package gr.uoa.di.madgik.resourcecatalogue.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmailUtils {

    public static List<String> getUserEmails(Map<String, Object> organisation) {
        List<String> emails = new ArrayList<>();
        Object usersObj = organisation.get("users");
        if (usersObj instanceof List<?> usersList) {
            for (Object userObj : usersList) {
                if (userObj instanceof Map<?, ?> userMap) {
                    Object email = userMap.get("email");
                    if (email instanceof String emailStr) {
                        emails.add(emailStr);
                    }
                }
            }
        }
        return emails;
    }

    public static String getMainContactEmail(Map<String, Object> resource) {
        Object mainContactObj = resource.get("mainContact");
        if (mainContactObj instanceof Map<?, ?> mainContactMap) {
            Object email = mainContactMap.get("email");
            if (email instanceof String emailStr) {
                return emailStr;
            }
        }
        return null;
    }

    public static List<String> getCreatorEmails(Map<String, Object> resource) {
        List<String> emails = new ArrayList<>();
        Object creators = resource.get("creators");
        if (creators instanceof List<?> creatorList) {
            for (Object creatorObj : creatorList) {
                if (creatorObj instanceof Map<?, ?> contactMap) {
                    Object email = contactMap.get("email");
                    if (email instanceof String emailStr) {
                        emails.add(emailStr);
                    }
                }
            }
        }
        return emails;
    }

    public static List<String> getPublicContactEmails(Map<String, Object> resource) {
        List<String> emails = new ArrayList<>();
        Object publicContactsObj = resource.get("publicContacts");
        if (publicContactsObj instanceof List<?> contactList) {
            for (Object contact : contactList) {
                if (contact instanceof String emailStr) {
                    emails.add(emailStr);
                }
            }
        }
        return emails;
    }
}