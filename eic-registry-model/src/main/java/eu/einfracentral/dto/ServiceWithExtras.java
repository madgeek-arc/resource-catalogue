package eu.einfracentral.dto;

import eu.einfracentral.domain.DynamicField;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;

import java.util.HashMap;
import java.util.Map;

public class ServiceWithExtras {

    private Service service;
    private Map<String, Object> extras;

    public ServiceWithExtras() {}

    public static ServiceWithExtras create(InfraService service) {
        ServiceWithExtras serviceWithExtras = new ServiceWithExtras();
        serviceWithExtras.service = service.getService();
        serviceWithExtras.extras = new HashMap<>();
        for (DynamicField<?> field : service.getExtras()) {
            serviceWithExtras.extras.put(field.getName(), field.getValue());
        }
        return serviceWithExtras;
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
