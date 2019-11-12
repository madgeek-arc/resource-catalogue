package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Funder;
import eu.einfracentral.registry.service.FunderService;
import eu.openminted.registry.core.domain.FacetFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Funder add(Funder funder, Authentication auth) {
        funder.setId(funder.getAcronym().toLowerCase());
        super.add(funder, auth);
        logger.debug("Adding Funder: {}", funder);
        return funder;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addAll(List<Funder> funders, Authentication auth) {
        for (Funder funder : funders){
            funder.setId(funder.getAcronym().toLowerCase());
            logger.debug(String.format("Adding Funder %s", funder.getFundingOrganisation()));
            super.add(funder, auth);
        }
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void deleteAll(Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<Funder> allFunders = getAll(ff, auth).getResults();
        for (Funder funder : allFunders) {
            logger.debug("Deleting Funder {}", funder.getFundingOrganisation());
            delete(funder);
        }
    }

    @Override
    public Map<String, Funder> getFundersMap() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        Map<String, Funder> fundersMap;
        fundersMap = getAll(ff, null)
                .getResults()
                .stream()
                .collect(Collectors.toMap(Funder::getId, v -> v));
        return fundersMap;
    }

}
