package eu.einfracentral.utils;

public class LoggingUtils {

    public static <T> String addResource(String resourceTypeName, String id, T obj) {
        return String.format("adding : [resourceType=%s] with [id=%s] : [body=%s]", resourceTypeName, id, obj);
    }

    public static <T> String updateResource(String resourceTypeName, String id, T obj) {
        return String.format("updating : [resourceType=%s] with [id=%s] : [body=%s]", resourceTypeName, id, obj);
    }

    public static <T> String deleteResource(String resourceTypeName, String id, T obj) {
        return String.format("deleting : [resourceType=%s] with [id=%s] : [body=%s]", resourceTypeName, id, obj);
    }

    private LoggingUtils() {}
}
