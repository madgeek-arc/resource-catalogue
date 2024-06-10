package gr.uoa.di.madgik.resourcecatalogue.manager;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.EmailMessage;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderRequest;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

@Component
@Deprecated
public class ProviderRequestManager extends ResourceManager<ProviderRequest> implements ProviderRequestService {

    private static final Logger logger = LoggerFactory.getLogger(ProviderRequestManager.class);
    private final MailService mailService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final ProviderService providerService;
    private final SecurityService securityService;
    private final Configuration cfg;

    @Value("${catalogue.name:Resource Catalogue}")
    private String catalogueName;

    @Autowired
    public ProviderRequestManager(MailService mailService, Configuration cfg,
                                  ServiceBundleService<ServiceBundle> serviceBundleService,
                                  ProviderService providerService,
                                  SecurityService securityService) {
        super(ProviderRequest.class);
        this.mailService = mailService;
        this.serviceBundleService = serviceBundleService;
        this.providerService = providerService;
        this.securityService = securityService;
        this.cfg = cfg;
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
    public List<ProviderRequest> getAllProviderRequests(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("provider_id", providerId);
        ff.setQuantity(maxQuantity);
        return getAll(ff, auth).getResults();
    }

    @Async
    public void sendMailsToProviders(List<String> serviceIds, EmailMessage message, Authentication auth) {

        boolean sendOnce = false;
        Map<String, String> providersToBeMailed = new LinkedHashMap<>();
        Map<String, String> providerContactNames = new LinkedHashMap<>();
        List<String> serviceNames = new ArrayList<>();
        for (String serviceId : serviceIds) {
            ServiceBundle service = serviceBundleService.get(serviceId);
            serviceNames.add(service.getService().getName());
            List<String> providerIds = service.getService().getResourceProviders();
            providerIds.add(service.getService().getResourceOrganisation());
            for (String providerId : providerIds) {
                ProviderBundle providerBundle = providerService.get(providerId, securityService.getAdminAccess());
                providerContactNames.put(providerBundle.getProvider().getMainContact().getLastName(), providerBundle.getProvider().getMainContact().getFirstName());
                providersToBeMailed.put(providerBundle.getProvider().getId(), providerBundle.getProvider().getMainContact().getEmail());
            }
        }

        Map<String, Object> root = new HashMap<>();
        StringWriter out1 = new StringWriter();
        StringWriter out2 = new StringWriter();

        for (Map.Entry<String, String> entry : providersToBeMailed.entrySet()) {
            Map.Entry<String, String> fullname = providerContactNames.entrySet().iterator().next();
            String key = fullname.getKey();
            providerContactNames.remove(key);

            String providerSubject = String.format("[%s] You have a new message from user [%s]-[%s], considering the Provider [%s]", catalogueName, message.getSenderName(), message.getSenderEmail(), entry.getKey());
            String userSubject = String.format("[%s] Your message considering the [%s] Services has been sent successfully", catalogueName, catalogueName);

            root.put("providerContactLastName", fullname.getKey());
            root.put("providerContactFirstName", fullname.getValue());
            root.put("project", catalogueName);
            root.put("provider", entry.getKey());
            root.put("message", message);
            root.put("services", serviceNames);

            try {
                Template temp = cfg.getTemplate("providerRequestProviderTemplate.ftl");
                temp.process(root, out1);
                String providerMail = out1.getBuffer().toString();
                mailService.sendMail(entry.getValue(), providerSubject, providerMail);
                logger.info("Recipient: {}\nTitle: {}\nMail body: \n{}", entry.getValue(), providerSubject, providerMail);

                temp = cfg.getTemplate("providerRequestUserTemplate.ftl");
                temp.process(root, out2);
                String userMail = out2.getBuffer().toString();
                if (!sendOnce) {
                    mailService.sendMail(message.getSenderEmail(), userSubject, userMail);
                    sendOnce = true;
                }
                logger.info("Recipient: {}\nTitle: {}\nMail body: \n{}", message.getSenderEmail(), userSubject, userMail);

                out1.getBuffer().setLength(0);
                out2.getBuffer().setLength(0);
                out1.close();
                out2.close();

            } catch (IOException e) {
                logger.error("Error finding mail template", e);
            } catch (TemplateException e) {
                logger.error("ERROR", e);
            } catch (MessagingException e) {
                logger.error("Could not send mail", e);
            }

            ProviderRequest providerRequest = new ProviderRequest();
            message.setRecipientEmail(entry.getValue());
            providerRequest.setDate(String.valueOf(System.currentTimeMillis()));
            providerRequest.setMessage(message);
            providerRequest.setProviderId(entry.getKey());
            providerRequest.setRead(false);
            add(providerRequest, auth);

        }
    }
}
