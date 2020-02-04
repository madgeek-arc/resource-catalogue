package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
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
public class ProviderRequestManager extends ResourceManager<ProviderRequest> implements ProviderRequestService<Authentication> {

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

    @Override
    public ProviderRequest add(ProviderRequest providerRequest, Authentication auth) {
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
        FacetFilter ff = new FacetFilter();
        ff.addFilter("provider_id", providerId);
        ff.setQuantity(1000);
        return getAll(ff, auth).getResults();
    }

    public void sendMailsToProviders(List<String> serviceIds, EmailMessage message, Authentication auth) {
        Map<String, String> providersToBeMailed = new HashMap<>();
        for (String serviceId : serviceIds) {
            InfraService service = infraServiceService.get(serviceId);
            List<String> providerIds = service.getService().getProviders();
            for (String providerId : providerIds) {
                ProviderBundle providerBundle = providerService.get(providerId, (Authentication) null);
                providersToBeMailed.put(providerId, providerBundle.getProvider().getContacts().get(0).getEmail());
            }
        }
        // FIXME:
        for (Map.Entry<String, String> entry : providersToBeMailed.entrySet()) {
            try {
                mailService.sendMail(entry.getValue(), message.getSenderEmail(), message.getSubject(), message.getMessage());
                logger.debug("Sending mail to {}", entry.getValue());
                ProviderRequest providerRequest = new ProviderRequest();
                message.setRecipientEmail(entry.getValue());
                providerRequest.setDate(String.valueOf(System.currentTimeMillis()));
                providerRequest.setMessage(message);
                providerRequest.setProviderId(entry.getKey());
                providerRequest.setRead(false);
                add(providerRequest, auth);
            } catch (MessagingException e) {
                logger.error(e);
            }
        }
    }

}
