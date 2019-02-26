package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Funder;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.registry.service.FunderService;
import eu.openminted.registry.core.domain.FacetFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class FunderManager extends ResourceManager<Funder> implements FunderService {

    private InfraServiceManager infraServiceManager;

    @Autowired
    public FunderManager(@Lazy InfraServiceManager infraServiceManager) {
        super(Funder.class);
        this.infraServiceManager = infraServiceManager;
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
    public Funder validateFunderServices(Funder funder){
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<String> verifiedServices = new ArrayList<>();
        List<InfraService> infraServiceList = infraServiceManager.getAll(ff, null).getResults();
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

}
