package gr.uoa.di.madgik.resourcecatalogue.utils;

import org.springframework.stereotype.Component;

@Component
public class PublicResourceUtils {

    public String createPublicResourceId(String id, String catalogueId) {
        String[] parts = id.split("/");
        String prefix = parts[0];
        String suffix = parts[1];
        return prefix + '/' + catalogueId + '.' + suffix;
    }
}
