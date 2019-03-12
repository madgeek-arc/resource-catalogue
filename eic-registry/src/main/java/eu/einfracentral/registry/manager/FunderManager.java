package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Funder;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.registry.service.FunderService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.utils.TextUtils;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
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

    @Autowired
    public FunderManager(InfraServiceService<InfraService, InfraService> infraServiceService) {
        super(Funder.class);
        this.infraServiceService = infraServiceService;
    }

    @Override
    public String getResourceType() {
        return "funder";
    }

    @Override
    public Funder add(Funder funder, Authentication auth) {
        validateFunderServices(funder);
        super.add(funder, auth);
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
        List<InfraService> serviceList = infraServiceService.getAll(ff, null).getResults();

        List<String> serviceListIds = new ArrayList<>();
        List<String> serviceListCategories = new ArrayList<>();
        for (InfraService infraService : serviceList) {
            serviceListIds.add(infraService.getId());
            if (!serviceListCategories.contains(infraService.getCategory())) {
                serviceListCategories.add(infraService.getCategory());
            }
        }
        Map<String, Map<String, Double>> funderStats = new HashMap<>();
        Map<String, Double> innerMap;

        if (funderId.matches("all")) {
            innerMap = new HashMap<>();
            for (Funder funder : funderList) {
                int count = 0;
                for (int i = 0; i < funder.getServices().size(); i++) {
                    if (serviceListIds.contains(funder.getServices().get(i))) {
                        innerMap.put(funder.getName(), (double) ++count);
                    }
                }
                funderStats.put("all", innerMap);
            }
            return funderStats;
        }

        Funder validatedFunder = get(funderId);

        funderStats.put("Categories", createMap("Category", serviceList, validatedFunder));
        funderStats.put("Subcategories", createMap("Subcategory", serviceList, validatedFunder));
        funderStats.put("TRL", createMap("trl", serviceList, validatedFunder));
        funderStats.put("Lifecycle Status", createMap("LifeCycleStatus", serviceList, validatedFunder));
        funderStats.put("Languages", createMap("Languages", serviceList, validatedFunder));
        funderStats.put("Places", createMap("Places", serviceList, validatedFunder));
        funderStats.put("Providers", createMap("Providers", serviceList, validatedFunder));

        return funderStats;

    }

    Map<String, Double> createMap(String fieldName, List<InfraService> services, Funder funder) {
        Map<String, Double> data = new HashMap<>();

        // create getter method name
        String methodName = "get" + TextUtils.capitalizeFirstLetter(fieldName);

        for (InfraService service : services) {
            if (funder.getServices().contains(service.getId())) {
                Object typeValue;
                try {
                    Method getter = InfraService.class.getMethod(methodName);
                    typeValue = getter.invoke(service);
                    List<?> values = null;
                    if (String.class.isAssignableFrom(getter.getReturnType())) {
                        values = Collections.singletonList(typeValue.toString());
                    } else if (List.class.isAssignableFrom(getter.getReturnType())) {
                        values = (List<?>) typeValue;
                    }
                    if (values != null && !values.isEmpty()) {
                        for (int i = 0; i < values.size(); i++) {
                            String value = ((ArrayList) typeValue).get(i).toString();
                            if (data.containsKey(value)) {
                                data.put(value, (data.get(value) + 1));
                            } else {
                                data.put(value, 1.0);
                            }
                        }
                    }
                } catch (NoSuchMethodException e) {
                    logger.error("ERROR: could not find method " + methodName, e);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    logger.error("ERROR: " + methodName, e);
                }
            }
        }

        return data;
    }
}
