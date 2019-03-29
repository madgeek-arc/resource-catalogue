package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.InfraService;
import eu.openminted.registry.core.service.AbstractGenericService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestClass extends AbstractGenericService<InfraService> {

    private static final Logger logger = LogManager.getLogger(ServiceResourceManager.class);

    public TestClass(Class<InfraService> typeParameterClass) {
        super(typeParameterClass);
    }

    @Override
    public String getResourceType() {
        return null;
    }


}
