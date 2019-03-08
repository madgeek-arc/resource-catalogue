package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Funder;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.registry.service.FunderService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import org.omg.CORBA.INTERNAL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FunderManager extends ResourceManager<Funder> implements FunderService {

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

//    @Override
    public Paging<Funder> getAll(Authentication authentication) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        return getAll(ff, authentication);
    }

    // Validates Funder's services
    public Funder validateFunderServices(Funder funder){
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<String> verifiedServices = new ArrayList<>();
        List<InfraService> infraServiceList = infraServiceService.getAll(ff, null).getResults();
        for (int i=0; i<infraServiceList.size(); i++){
            for (int j=0; j<funder.getServices().size(); j++){
                if (infraServiceList.get(i).getId().matches(funder.getServices().get(j))) {
                    if (!verifiedServices.contains(funder.getServices().get(j))){
                        verifiedServices.add(funder.getServices().get(j));
                    }
                }
            }
        }
        verifiedServices.sort(null);
        funder.setServices(verifiedServices);
        return funder;
    }

    public Map<String, Map<String, Double>> getFunderStats(String funderId, Authentication auth) {

        List<Funder> funderList = this.getAll(new FacetFilter(), null).getResults();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("latest", "true");
        ff.addFilter("active", "true");
        List<InfraService> serviceList = infraServiceService.getAll(ff, null).getResults();
        List<String> serviceListIds = new ArrayList<>();
        List<String> serviceListCategories = new ArrayList<>();
        for (InfraService infraService : serviceList){
            serviceListIds.add(infraService.getId());
            if (!serviceListCategories.contains(infraService.getCategory())){
                serviceListCategories.add(infraService.getCategory());
            }
        }
        Map<String, Map<String, Double>> funderStats = new HashMap<>();
        Map<String, Double> innerMap;

        if (funderId.matches("all")){
            innerMap = new HashMap<>();
            for (Funder funder : funderList){
                int count = 0;
                for (int i=0; i<funder.getServices().size(); i++){
                    if (serviceListIds.contains(funder.getServices().get(i))){
                        innerMap.put(funder.getName(), (double) ++count);
                    }
                }
                funderStats.put("all", innerMap);
            }
            return funderStats;
        }

        Funder validatedFunder = get(funderId);

        innerMap = new HashMap<>();
        for (int i=0; i<serviceList.size(); i++){
            for (int j=0; j<validatedFunder.getServices().size(); j++){
                if (serviceList.get(i).getId().matches(validatedFunder.getServices().get(j))){
                    if (innerMap.containsKey(serviceList.get(i).getCategory())){
                        innerMap.put(serviceList.get(i).getCategory(), (innerMap.get(serviceList.get(i).getCategory()) + 1));
                    } else{
                        innerMap.put(serviceList.get(i).getCategory(), 1.0);
                    }
                }
            }
        }
        funderStats.put("categories", innerMap);

        innerMap = new HashMap<>();
        for (int i=0; i<serviceList.size(); i++){
            for (int j=0; j<validatedFunder.getServices().size(); j++){
                if (serviceList.get(i).getId().matches(validatedFunder.getServices().get(j))){
                    if (innerMap.containsKey(serviceList.get(i).getSubcategory())){
                        innerMap.put(serviceList.get(i).getSubcategory(), (innerMap.get(serviceList.get(i).getSubcategory()) + 1));
                    } else{
                        innerMap.put(serviceList.get(i).getSubcategory(), 1.0);
                    }
                }
            }
        }
        funderStats.put("subcategories", innerMap);

        innerMap = new HashMap<>();
        for (int i=0; i<serviceList.size(); i++){
            for (int j=0; j<validatedFunder.getServices().size(); j++){
                if (serviceList.get(i).getId().matches(validatedFunder.getServices().get(j))){
                    if (innerMap.containsKey(serviceList.get(i).getTrl())){
                        innerMap.put(serviceList.get(i).getTrl(), (innerMap.get(serviceList.get(i).getTrl()) + 1));
                    } else{
                        innerMap.put(serviceList.get(i).getTrl(), 1.0);
                    }
                }
            }
        }
        funderStats.put("trl", innerMap);

        innerMap = new HashMap<>();
        for (int i=0; i<serviceList.size(); i++){
            for (int j=0; j<validatedFunder.getServices().size(); j++){
                if (serviceList.get(i).getId().matches(validatedFunder.getServices().get(j))){
                    if (innerMap.containsKey(serviceList.get(i).getLifeCycleStatus())){
                        innerMap.put(serviceList.get(i).getLifeCycleStatus(), (innerMap.get(serviceList.get(i).getLifeCycleStatus()) + 1));
                    } else{
                        innerMap.put(serviceList.get(i).getLifeCycleStatus(), 1.0);
                    }
                }
            }
        }
        funderStats.put("lifecyclestatus", innerMap);

        innerMap = new HashMap<>();
        for (int i=0; i<serviceList.size(); i++){
            for (int j=0; j<validatedFunder.getServices().size(); j++){
                if (serviceList.get(i).getId().matches(validatedFunder.getServices().get(j))){
                    for (int k=0; k<serviceList.get(i).getLanguages().size(); k++){
                        if (innerMap.containsKey(serviceList.get(i).getLanguages().get(k))){
                            innerMap.put(serviceList.get(i).getLanguages().get(k), (innerMap.get(serviceList.get(i).getLanguages().get(k)) + 1));
                        } else{
                            innerMap.put(serviceList.get(i).getLanguages().get(k), 1.0);
                        }
                    }
                }
            }
        }
        funderStats.put("languages", innerMap);

        innerMap = new HashMap<>();
        for (int i=0; i<serviceList.size(); i++){
            for (int j=0; j<validatedFunder.getServices().size(); j++){
                if (serviceList.get(i).getId().matches(validatedFunder.getServices().get(j))){
                    for (int k=0; k<serviceList.get(i).getPlaces().size(); k++){
                        if (innerMap.containsKey(serviceList.get(i).getPlaces().get(k))){
                            innerMap.put(serviceList.get(i).getPlaces().get(k), (innerMap.get(serviceList.get(i).getPlaces().get(k)) + 1));
                        } else{
                            innerMap.put(serviceList.get(i).getPlaces().get(k), 1.0);
                        }
                    }
                }
            }
        }
        funderStats.put("places", innerMap);

        innerMap = new HashMap<>();
        for (int i=0; i<serviceList.size(); i++){
            for (int j=0; j<validatedFunder.getServices().size(); j++){
                if (serviceList.get(i).getId().matches(validatedFunder.getServices().get(j))){
                    for (int k=0; k<serviceList.get(i).getProviders().size(); k++){
                        if (innerMap.containsKey(serviceList.get(i).getProviders().get(k))){
                            innerMap.put(serviceList.get(i).getProviders().get(k), (innerMap.get(serviceList.get(i).getProviders().get(k)) + 1));
                        } else{
                            innerMap.put(serviceList.get(i).getProviders().get(k), 1.0);
                        }
                    }
                }
            }
        }
        funderStats.put("providers", innerMap);

        return funderStats;

    }

}
