package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.BundledService;
import eu.einfracentral.utils.FacetLabelService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.index.IndexField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service("bundleService")
public class BundleServiceManager extends BundleManager<BundledService, BundledService, Authentication> {

    private static final Logger logger = LogManager.getLogger(BundleServiceManager.class);

    private List<String> browseBy;
    private Map<String, String> labels;

    private FacetLabelService facetLabelService;

    @Autowired
    public BundleServiceManager(FacetLabelService facetLabelService) {
        super(BundledService.class, BundledService.class);
        this.facetLabelService = facetLabelService;
    }

    @PostConstruct
    void initLabels() {
        resourceType = resourceTypeService.getResourceType(getResourceType());
        Set<String> browseSet = new HashSet<>();
        Map<String, Set<String>> sets = new HashMap<>();
        labels = new HashMap<>();
        labels.put("resourceType", "Resource Type");
        for (IndexField f : resourceTypeService.getResourceTypeIndexFields(getResourceType())) {
            sets.putIfAbsent(f.getResourceType().getName(), new HashSet<>());
            labels.put(f.getName(), f.getLabel());
            if (f.getLabel() != null) {
                sets.get(f.getResourceType().getName()).add(f.getName());
            }
        }
        boolean flag = true;
        for (Map.Entry<String, Set<String>> entry : sets.entrySet()) {
            if (flag) {
                browseSet.addAll(entry.getValue());
                flag = false;
            } else {
                browseSet.retainAll(entry.getValue());
            }
        }
        browseBy = new ArrayList<>();
        browseBy.addAll(browseSet);
        browseBy.add("resourceType");
        java.util.Collections.sort(browseBy);
        logger.info("Generated generic service for '{}'[{}]", getResourceType(), getClass().getSimpleName());
    }

    @Override
    public Browsing<BundledService> getAll(FacetFilter filter, Authentication auth) {
        List<String> orderedBrowseBy = new ArrayList<>();

        //Order Service's facets as we like (+removed Service Name - no4)
        orderedBrowseBy.add(browseBy.get(11));   //no11 - Subcategories
        orderedBrowseBy.add(browseBy.get(8));    // no8 - Providers
        orderedBrowseBy.add(browseBy.get(10));   //no10 - Scientific Subdomains
        orderedBrowseBy.add(browseBy.get(6));    // no6 - Phase
        orderedBrowseBy.add(browseBy.get(13));   //no13 - TRL
        orderedBrowseBy.add(browseBy.get(7));    // no7 - Places
        orderedBrowseBy.add(browseBy.get(3));    // no3 - Languages
        orderedBrowseBy.add(browseBy.get(1));    // no1 - Access Types
        orderedBrowseBy.add(browseBy.get(0));    // no0 - Access Modes
        orderedBrowseBy.add(browseBy.get(5));    // no5 - Order Type
        orderedBrowseBy.add(browseBy.get(12));   //no12 - Target Users
        orderedBrowseBy.add(browseBy.get(2));    // no2 - Funders
        orderedBrowseBy.add(browseBy.get(9));    // no9 - Resource Type

        filter.setBrowseBy(orderedBrowseBy);

        filter.setResourceType(getResourceType());
        return getMatchingServices(filter);
    }


    private Browsing<BundledService> getMatchingServices(FacetFilter ff) {
        Browsing<BundledService> services;

        services = getResults(ff);
        if (!services.getResults().isEmpty() && !services.getFacets().isEmpty()) {
            services.setFacets(facetLabelService.createLabels(services.getFacets()));
        }
        return services;
    }
}
