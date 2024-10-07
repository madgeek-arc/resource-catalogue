package gr.uoa.di.madgik.resourcecatalogue.utils;

import org.springframework.stereotype.Component;

@Component
public class PublicResourceUtils {

    /**
     * Creates public ID for the following resources:
     * Provider, Service, Datasource, Helpdesk, Monitoring, Training Resource, Interoperability Record
     * Configuration Template Instance, Resource Interoperability Record
     *
     * @param id          resource ID
     * @param catalogueId resource catalogue ID
     * @return public id
     */
    public String createPublicResourceId(String id, String catalogueId) {
        String[] parts = id.split("/");
        String prefix = parts[0];
        String suffix = parts[1];
        return prefix + '/' + catalogueId + '.' + suffix;
    }
}
