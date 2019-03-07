package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Funder;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.registry.service.FunderService;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
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

    public Map<String, Double> getFunderStats(String funderId, String field, Authentication auth) {
        if (funderId.equals("all") && field.equals("services")){
            List<Funder> funderList = this.getAll(new FacetFilter(), null).getResults();
            Map<String, Double> funderNoOfServices = new HashMap<>();
            for (int i=0; i<funderList.size(); i++){
                funderNoOfServices.put(funderList.get(i).getId(), (double) funderList.get(i).getServices().size());
            }
            return funderNoOfServices;
        } else{
            Funder funder = this.get(funderId);
            List<String> funderServices = funder.getServices();
            FacetFilter ff = new FacetFilter();
            ff.setQuantity(10000);
            ff.addFilter("latest", "true");
            ff.addFilter("active", "true");
            List<InfraService> services = infraServiceService.getAll(ff, null).getResults();
            Map<String, Double> funderStats = new HashMap<>();
            int count = 0;
            switch (field){
                case "category":
                    for (int i=0; i<services.size(); i++){
                        for (int j=0; j<funderServices.size(); j++){
                            if (services.get(i).getId().matches(funderServices.get(j))){
                                if (!funderStats.containsKey(services.get(i).getCategory())){
                                    funderStats.put(services.get(i).getCategory(), 1.0);
                                    count++;
                                } else{
                                    double value = funderStats.get(services.get(i).getCategory());
                                    funderStats.put(services.get(i).getCategory(), value+1);
                                    count++;
                                }
                            }
                        }
                    }
                    break;
                case "subcategory":
                    for (int i=0; i<services.size(); i++){
                        for (int j=0; j<funderServices.size(); j++){
                            if (services.get(i).getId().matches(funderServices.get(j))){
                                if (!funderStats.containsKey(services.get(i).getSubcategory())){
                                    funderStats.put(services.get(i).getSubcategory(), 1.0);
                                    count++;
                                } else{
                                    double value = funderStats.get(services.get(i).getSubcategory());
                                    funderStats.put(services.get(i).getSubcategory(), value+1);
                                    count++;
                                }
                            }
                        }
                    }
                    break;
                case "lifecyclestatus":
                    for (int i=0; i<services.size(); i++){
                        for (int j=0; j<funderServices.size(); j++){
                            if (services.get(i).getId().matches(funderServices.get(j))){
                                if (!funderStats.containsKey(services.get(i).getLifeCycleStatus())){
                                    funderStats.put(services.get(i).getLifeCycleStatus(), 1.0);
                                    count++;
                                } else{
                                    double value = funderStats.get(services.get(i).getLifeCycleStatus());
                                    funderStats.put(services.get(i).getLifeCycleStatus(), value+1);
                                    count++;
                                }
                            }
                        }
                    }
                    break;
                case "trl":
                    for (int i=0; i<services.size(); i++){
                        for (int j=0; j<funderServices.size(); j++){
                            if (services.get(i).getId().matches(funderServices.get(j))){
                                if (!funderStats.containsKey(services.get(i).getTrl())){
                                    funderStats.put(services.get(i).getTrl(), 1.0);
                                    count++;
                                } else{
                                    double value = funderStats.get(services.get(i).getTrl());
                                    funderStats.put(services.get(i).getTrl(), value+1);
                                    count++;
                                }
                            }
                        }
                    }
                    break;
                case "places":
                    for (int i=0; i<services.size(); i++){
                        for (int j=0; j<funderServices.size(); j++){
                            if (services.get(i).getId().matches(funderServices.get(j))){
                                for (int k=0; k<services.get(i).getPlaces().size(); k++){
                                    if (!funderStats.containsKey(services.get(i).getPlaces().get(k))){
                                        funderStats.put(services.get(i).getPlaces().get(k), 1.0);
                                        count++;
                                    } else{
                                        double value = funderStats.get(services.get(i).getPlaces().get(k));
                                        funderStats.put(services.get(i).getPlaces().get(k), value+1);
                                        count++;
                                    }
                                }
                            }
                        }
                    }
                    break;
                case "languages":
                    for (int i=0; i<services.size(); i++){
                        for (int j=0; j<funderServices.size(); j++){
                            if (services.get(i).getId().matches(funderServices.get(j))){
                                for (int k=0; k<services.get(i).getLanguages().size(); k++){
                                    if (!funderStats.containsKey(services.get(i).getLanguages().get(k))){
                                        funderStats.put(services.get(i).getLanguages().get(k), 1.0);
                                        count++;
                                    } else{
                                        double value = funderStats.get(services.get(i).getLanguages().get(k));
                                        funderStats.put(services.get(i).getLanguages().get(k), value+1);
                                        count++;
                                    }
                                }
                            }
                        }
                    }
                    break;
                case "providers":
                    for (int i=0; i<services.size(); i++){
                        for (int j=0; j<funderServices.size(); j++){
                            if (services.get(i).getId().matches(funderServices.get(j))){
                                for (int k=0; k<services.get(i).getProviders().size(); k++){
                                    if (!funderStats.containsKey(services.get(i).getProviders().get(k))){
                                        funderStats.put(services.get(i).getProviders().get(k), 1.0);
                                        count++;
                                    } else{
                                        double value = funderStats.get(services.get(i).getProviders().get(k));
                                        funderStats.put(services.get(i).getProviders().get(k), value+1);
                                        count++;
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
            for (Map.Entry<String, Double> entry : funderStats.entrySet()){
                entry.setValue(entry.getValue()/count);
            }
            return funderStats;
        }

    }

}
