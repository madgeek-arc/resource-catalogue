package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.ProviderRequest;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ProviderRequestService;
import eu.einfracentral.validator.FieldValidator;
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

    private static final Logger logger = LogManager.getLogger(ProviderRequestManager.class);
    private FieldValidator fieldValidator;

    @Autowired
    public ProviderRequestManager(FieldValidator fieldValidator) {
        super(ProviderRequest.class);
        this.fieldValidator = fieldValidator;
    }

    @Override
    public String getResourceType() {
        return "provider_request";
    }

    @Override
    public ProviderRequest add(ProviderRequest providerRequest, Authentication auth) {
        providerRequest.setId(UUID.randomUUID().toString());
        validate(providerRequest);
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

    @Override
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

        try {
            fieldValidator.validateFields(providerRequest);
        } catch (IllegalAccessException e) {
            logger.error("Validation error:", e);
        }

        return null;
    }

}
