package eu.einfracentral.dto;

import eu.einfracentral.domain.Service;

import java.util.Map;

public class UiService {

    private Service service;
    private Map<String, Object> extras;

    public UiService() {
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }

    public void setExtras(Map<String, Object> extras) {
        this.extras = extras;
    }
}
