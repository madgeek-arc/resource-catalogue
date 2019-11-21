package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.PendingService;
import eu.einfracentral.registry.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class PendingServiceManager extends ResourceManager<PendingService> implements ResourceService<PendingService, Authentication> {

    @Autowired
    public PendingServiceManager() {
        super(PendingService.class);
    }

    @Override
    public String getResourceType() {
        return "pending_service";
    }
}
