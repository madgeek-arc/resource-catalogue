package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Funder;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.RichService;
import eu.einfracentral.registry.service.FunderService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.TextUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Component
public class FunderManager extends ResourceManager<Funder> implements FunderService {

    private static final Logger logger = LogManager.getLogger(FunderManager.class);

    public FunderManager() {
        super(Funder.class);
    }

    @Override
    public String getResourceType() {
        return "funder";
    }

    @Override
    public Funder add(Funder funder, Authentication auth) {
        funder.setId(UUID.randomUUID().toString());
        super.add(funder, auth);
        logger.debug("Adding Funder: {}", funder);
        return funder;
    }

    public void addAll(List<Funder> funders, Authentication auth) {
        for (Funder funder : funders){
            funder.setId(UUID.randomUUID().toString());
            logger.debug(String.format("Adding Funder %s", funder.getFundingOrganisation()));
            super.add(funder, auth);
        }
    }
}
