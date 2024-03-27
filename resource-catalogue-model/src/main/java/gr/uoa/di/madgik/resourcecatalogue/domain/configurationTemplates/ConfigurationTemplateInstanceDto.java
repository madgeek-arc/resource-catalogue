package gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates;


import org.json.simple.JSONObject;

public class ConfigurationTemplateInstanceDto {

    private String id;
    private String resourceId;
    private String configurationTemplateId;
    private JSONObject payload;

    public ConfigurationTemplateInstanceDto() {
    }

    public ConfigurationTemplateInstanceDto(String id, String resourceId, String configurationTemplateId, JSONObject payload) {
        this.id = id;
        this.resourceId = resourceId;
        this.configurationTemplateId = configurationTemplateId;
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getConfigurationTemplateId() {
        return configurationTemplateId;
    }

    public void setConfigurationTemplateId(String configurationTemplateId) {
        this.configurationTemplateId = configurationTemplateId;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
