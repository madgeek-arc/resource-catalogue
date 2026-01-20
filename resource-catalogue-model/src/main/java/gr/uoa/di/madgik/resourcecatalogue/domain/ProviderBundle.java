package gr.uoa.di.madgik.resourcecatalogue.domain;

import java.util.LinkedHashMap;

public class ProviderBundle extends Bundle {

    private String templateStatus;

    public LinkedHashMap<String, Object> getProvider() {
        return this.getPayload();
    }

    public void setProvider(LinkedHashMap<String, Object> payload) {
        this.setPayload(payload);
    }

    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    public String getTemplateStatus() {
        return templateStatus;
    }

    public void setTemplateStatus(String templateStatus) {
        this.templateStatus = templateStatus;
    }
}
