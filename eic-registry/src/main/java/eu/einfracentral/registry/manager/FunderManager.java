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
    private InfraServiceService<InfraService, InfraService> infraServiceService;
    private ProviderService providerService;
    private SecurityService securityService;
    private ServiceResourceManager serviceResourceManager;

    @Autowired
    public FunderManager(InfraServiceService<InfraService, InfraService> infraServiceService,
                         ProviderService providerService, SecurityService securityService, ServiceResourceManager serviceResourceManager) {
        super(Funder.class);
        this.infraServiceService = infraServiceService;
        this.providerService = providerService;
        this.securityService = securityService;
        this.serviceResourceManager = serviceResourceManager;
    }

    @Override
    public String getResourceType() {
        return "funder";
    }

    @Override
    public Funder add(Funder funder, Authentication auth) {
        validateFunderServices(funder);
        super.add(funder, auth);
        logger.debug("Adding Funder: {}", funder);
        return funder;
    }

    // Validates Funder's services
    public Funder validateFunderServices(Funder funder) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<String> verifiedServices = new ArrayList<>();
        List<InfraService> infraServiceList = infraServiceService.getAll(ff, null).getResults();
        for (int i = 0; i < infraServiceList.size(); i++) {
            for (int j = 0; j < funder.getServices().size(); j++) {
                if (infraServiceList.get(i).getId().matches(funder.getServices().get(j))) {
                    if (!verifiedServices.contains(funder.getServices().get(j))) {
                        verifiedServices.add(funder.getServices().get(j));
                    }
                }
            }
        }
        verifiedServices.sort(null);
        funder.setServices(verifiedServices);
        return funder;
    }

    @Override
    public Map<String, Map<String, Double>> getFunderStats(String funderId, Authentication auth) {

        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<Funder> funderList = this.getAll(ff, null).getResults();
        ff.addFilter("latest", "true");
        ff.addFilter("active", "true");
        List<RichService> richServiceList = serviceResourceManager.createRichVocabularies(infraServiceService.getAll(ff, auth).getResults());
        Map<String, Map<String, Double>> funderStats = new LinkedHashMap<>();
        Map<String, Double> servicesMap;

        if (funderId.matches("all")) {
            List<String> serviceListIds = new ArrayList<>();
            for (RichService richService : richServiceList) {
                serviceListIds.add(richService.getService().getId());
            }
            servicesMap = new HashMap<>();
            for (Funder funder : funderList) {
                int count = 0;
                for (int i = 0; i < funder.getServices().size(); i++) {
                    if (serviceListIds.contains(funder.getServices().get(i))) {
                        servicesMap.put(funder.getName(), (double) ++count);
                    }
                }

            }
            funderStats.put("Funders", servicesMap);

            return createFunderStats(funderStats, richServiceList);

        } else {
            Funder funder = get(funderId);
            List<RichService> funderServices = new ArrayList<>();
            for (RichService richService : richServiceList) {
                if (funder.getServices().contains(richService.getService().getId())) {
                    funderServices.add(richService);
                }
            }

            return createFunderStats(funderStats, funderServices);
        }

    }

    private Map<String, Map<String, Double>> createFunderStats(Map<String, Map<String, Double>> funderStats, List<RichService> services) {
        funderStats.put("Supercategories", createMap("SuperCategoryNames", services));
        funderStats.put("Categories", createMap("CategoryNames", services));
        funderStats.put("Subcategories", createMap("SubCategoryNames", services));
        funderStats.put("TRL", createMap("TrlName", services));
        funderStats.put("Phase", createMap("PhaseName", services));
        funderStats.put("Languages", createMap("LanguageNames", services));
        funderStats.put("Places", createMap("PlaceNames", services));

        Map<String, Double> providerMap = new HashMap<>();
        for (RichService richService : services) {
            for (String provider : richService.getService().getProviders()) {
                String providerName = providerService.get(provider, securityService.getAdminAccess()).getName();
                if (providerMap.containsKey(providerName)) {
                    providerMap.put(providerName, (providerMap.get(providerName) + 1));
                } else {
                    providerMap.put(providerName, 1.0);
                }
            }
        }
        funderStats.put("Providers", providerMap);

        return funderStats;
    }

    private Map<String, Double> createMap(String fieldName, List<RichService> services) {
        Map<String, Double> data = new HashMap<>();

        // create getter method name
        String methodName = "get" + TextUtils.capitalizeFirstLetter(fieldName);

        for (RichService service : services) {
            Object typeValue;
            try {
                Method getter = RichService.class.getMethod(methodName);
                typeValue = getter.invoke(service);
                List<?> values = null;
                if (String.class.isAssignableFrom(getter.getReturnType())) {
                    values = Collections.singletonList(typeValue.toString());
                } else if (List.class.isAssignableFrom(getter.getReturnType())) {
                    values = (List<?>) typeValue;
                }
                if (values != null && !values.isEmpty()) {
                    for (int i = 0; i < values.size(); i++) {
                        String value = values.get(i).toString();
                        if (data.containsKey(value)) {
                            data.put(value, (data.get(value) + 1));
                        } else {
                            data.put(value, 1.0);
                        }
                    }
                }
            } catch (NoSuchMethodException e) {
                logger.error("ERROR: could not find method: {}", methodName, e);
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.error("ERROR: {}", methodName, e);
            }
        }

        return data;
    }
}
