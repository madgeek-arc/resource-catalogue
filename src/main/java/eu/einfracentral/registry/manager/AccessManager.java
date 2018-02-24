package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Access;
import eu.einfracentral.registry.service.AccessService;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 05/01/18.
 */
@Service("accessService")
public class AccessManager extends ResourceManager<Access> implements AccessService {
    public AccessManager() {
        super(Access.class);
    }

    @Override
    public String getResourceType() {
        return "access";
    }
}
