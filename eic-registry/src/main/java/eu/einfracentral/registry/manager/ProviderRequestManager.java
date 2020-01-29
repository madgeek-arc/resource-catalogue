package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderRequestService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.MailService;
import eu.einfracentral.validator.FieldValidator;
import eu.openminted.registry.core.domain.FacetFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.sql.Timestamp;
import java.util.*;

@Component
public class ProviderRequestManager extends ResourceManager<ProviderRequest> implements ProviderRequestService<ProviderRequest, Authentication> {

    private static final Logger logger = LogManager.getLogger(ProviderRequestManager.class);
    private FieldValidator fieldValidator;
    private MailService mailService;
    private InfraServiceService<InfraService, InfraService> infraServiceService;
    private ProviderService<ProviderBundle, Authentication> providerService;

    @Autowired
    public ProviderRequestManager(FieldValidator fieldValidator, MailService mailService,
                                  InfraServiceService<InfraService, InfraService> infraServiceService,
                                  ProviderService<ProviderBundle, Authentication> providerService) {
        super(ProviderRequest.class);
        this.fieldValidator = fieldValidator;
        this.mailService = mailService;
        this.infraServiceService = infraServiceService;
        this.providerService = providerService;
    }

    @Override
    public String getResourceType() {
        return "provider_request";
    }

    public ProviderRequest add(ProviderRequest providerRequest) {
        providerRequest.setId(UUID.randomUUID().toString());
        validate(providerRequest);
        super.add(providerRequest, null);
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
    public List<ProviderRequest> getAllProviderRequests(String providerId, Authentication auth) {
        List<ProviderRequest> ret = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<ProviderRequest> providerRequests = getAll(ff, auth).getResults();
        for (ProviderRequest providerRequest : providerRequests) {
            if (providerRequest.getProviderId().equals(providerId)) {
                ret.add(providerRequest);
            }
        }
        if (ret.isEmpty()) {
            throw new ValidationException("Provider with id " + providerId + " does not exist.");
        }

        return ret;
    }

    @Override
    public ProviderRequest validate(ProviderRequest providerRequest) {
        logger.debug("Validating ProviderRequest: {}", providerRequest);
        super.validate(providerRequest);
        return providerRequest;
    }

    public void sendMailsToProviders(List<String> serviceIds, EmailMessage message) {
        Map<String, String> providersToBeMailed = new HashMap<>();
        for (String serviceId : serviceIds) {
            InfraService service = infraServiceService.get(serviceId);
            List<String> providerIds = service.getService().getProviders();
            for (String providerId : providerIds) {
                ProviderBundle providerBundle = providerService.get(providerId, (Authentication) null);
                providersToBeMailed.put(providerId, providerBundle.getProvider().getContacts().get(0).getEmail());
            }
        }
        for (Map.Entry<String, String> entry : providersToBeMailed.entrySet()) {
            try {
                mailService.sendMail(entry.getValue(), message.getSenderEmail(), message.getSubject(), message.getMessage());
                logger.debug("Sending mail to {}", entry.getValue());
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                ProviderRequest providerRequest = new ProviderRequest();
                message.setRecipientEmail(entry.getValue());
                providerRequest.setDate(timestamp);
                providerRequest.setMessage(message);
                providerRequest.setProviderId(entry.getKey());
                providerRequest.setStatus(false);
                add(providerRequest);
            } catch (MessagingException e) {
                logger.error(e);
            }
        }
    }

}
