package gr.uoa.di.madgik.resourcecatalogue.domain;

import java.beans.Transient;
import java.util.LinkedHashMap;

public class NewProviderBundle extends NewBundle {

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
}
