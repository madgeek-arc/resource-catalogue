package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.*;
import eu.openminted.registry.core.domain.FacetFilter;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by pgl on 26/7/2017.
 */
@org.springframework.stereotype.Service("providerService")
public class ProviderManager extends ResourceManager<Provider> implements ProviderService {

    public ProviderManager() {
        super(Provider.class);
    }

    @Override
    public String getResourceType() {
        return "provider";
    }
}
