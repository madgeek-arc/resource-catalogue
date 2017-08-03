package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Provider;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.log4j.Logger;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * Created by pgl on 26/7/2017.
 */
@org.springframework.stereotype.Service("providerService")
public class ProviderServiceImpl<T> extends BaseGenericResourceCRUDService<Provider> implements ProviderService {

    public ProviderServiceImpl() {
        super(Provider.class);
    }

    @Override
    public String getResourceType() {
        return "provider";
    }

}
