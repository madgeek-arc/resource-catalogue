package eu.openminted.registry.service;

import eu.openminted.registry.domain.Component;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service("componentService")
@Primary
public class ComponentServiceImpl extends AbstractGenericService<Component>{

    public ComponentServiceImpl() {
        super(Component.class);
    }

    @Override
    public String getResourceType() {
        return "component";
    }
}
