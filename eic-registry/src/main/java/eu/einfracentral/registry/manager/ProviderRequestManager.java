package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderRequest;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ProviderRequestService;
import eu.openminted.registry.core.domain.FacetFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ProviderRequestManager extends ResourceManager<ProviderRequest> implements ProviderRequestService<ProviderRequest, Authentication> {

    private static final Logger logger = LogManager.getLogger(ProviderRequest.class);
    private ProviderManager providerManager;

    @Autowired
    public ProviderRequestManager(ProviderManager providerManager) {
        super(ProviderRequest.class);
        this.providerManager = providerManager;
    }

    @Override
    public String getResourceType() {
        return "provider_request";
    }

    public ProviderRequest get(String id, Authentication auth) {
        return null;
    }

    @Override
    public ProviderRequest add(ProviderRequest providerRequest, Authentication auth) {
        validate(providerRequest);
        providerRequest.setId(UUID.randomUUID().toString());
        super.add(providerRequest, auth);
        logger.debug("Adding ProviderRequest {}", providerRequest);
        return providerRequest;
    }

    @Override
    public ProviderRequest update(ProviderRequest providerRequest, Authentication auth) {
        validate(providerRequest);
        super.update(providerRequest, auth);
        logger.info("Updating ProviderRequest {}", providerRequest);
        return providerRequest;
    }

    @Override
    public void delete(ProviderRequest providerRequest) {
        logger.info("Deleting ProviderRequest {}", providerRequest);
        super.delete(providerRequest);
    }

    public List<ProviderRequest> getAllProviderRequests (String providerId, Authentication auth){
        List<ProviderRequest> ret = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<ProviderRequest> providerRequests = getAll(ff, auth).getResults();
        for (ProviderRequest providerRequest : providerRequests){
            if (providerRequest.getProviderId().equals(providerId)){
                ret.add(providerRequest);
            }
        }
        if (ret.isEmpty()){
            throw new ValidationException("Provider with id " +providerId+ " does not exist.");
        }

        return ret;
    }

    public ProviderRequest validate(ProviderRequest providerRequest) {

        // Validates ProviderRequest's providerId
        if (providerRequest.getProviderId() == null || providerRequest.getProviderId().equals("")) {
            throw new ValidationException("ProviderRequest's providerId cannot be 'null' or 'empty'");
        }
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<String> providerIds = new ArrayList<>();
        for (Provider provider : providerManager.getAll(ff, null).getResults()){
            providerIds.add(provider.getId());
        }
        if (!providerIds.contains(providerRequest.getProviderId())){
            throw new ValidationException("Provider with id " +providerRequest.getProviderId()+ " does not exist.");
        }

        // Validates ProviderRequest's date
        if (providerRequest.getDate() != null && "".equals(providerRequest.getDate().toString())) {
            throw new ValidationException("Measurement's time cannot be empty");
        }

        // Validates ProviderRequest's message
        if (providerRequest.getMessage() == null) {
            throw new ValidationException("ProviderRequest's message cannot be null");
        }

        return null;
    }

}
