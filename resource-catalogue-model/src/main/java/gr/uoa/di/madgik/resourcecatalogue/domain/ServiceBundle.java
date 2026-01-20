package gr.uoa.di.madgik.resourcecatalogue.domain;

import java.util.LinkedHashMap;

public class ServiceBundle extends Bundle {

    public LinkedHashMap<String, Object> getService() {
        return this.getPayload();
    }

    public void setService(LinkedHashMap<String, Object> payload) {
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
}
