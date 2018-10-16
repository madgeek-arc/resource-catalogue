package eu.einfracentral.registry.manager;

import eu.einfracentral.config.security.EICAuthoritiesMapper;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.User;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.MailService;
import eu.einfracentral.utils.ObjectUtils;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service("providerManager")
public class ProviderManager extends ResourceManager<Provider> implements ProviderService<Provider, Authentication> {

    private static final Logger logger = LogManager.getLogger(ProviderManager.class);
    private InfraServiceService<InfraService, InfraService> infraServiceService;
    private MailService mailService;
    private EICAuthoritiesMapper authoritiesMapper;
    private Configuration cfg;

    @Value("${webapp.front:beta.einfracentral.eu}")
    private String endpoint;

    @Autowired
    public ProviderManager(InfraServiceService<InfraService, InfraService> infraServiceService, MailService mailService,
                           @Lazy EICAuthoritiesMapper authoritiesMapper, Configuration cfg) {
        super(Provider.class);
        this.infraServiceService = infraServiceService;
        this.mailService = mailService;
        this.authoritiesMapper = authoritiesMapper;
        this.cfg = cfg;
    }


    @Override
    public String getResourceType() {
        return "provider";
    }

    @Override
    public Provider add(Provider provider, Authentication auth) {
        List<User> users;
        User authUser = new User(auth);
        Provider ret;
        provider.setId(StringUtils
                .stripAccents(provider.getId())
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replaceAll(" ", "_"));

        users = provider.getUsers();
        if (users == null) {
            users = new ArrayList<>();
        }
        if (users.stream().noneMatch(u -> u.getEmail().equals(authUser.getEmail()))) {
            users.add(authUser);
            provider.setUsers(users);
        }
        provider.setStatus(Provider.States.INIT.getKey());
        sendProviderMails(provider, new User(auth), Provider.States.INIT);

        provider.setActive(false);
        provider.setStatus(Provider.States.PENDING_1.getKey());

        ret = super.add(provider, null);
        authoritiesMapper.mapProviders(provider.getUsers());

        // TODO: fix function
//        createProviderMail(provider, new User(auth), Provider.States.INIT);
        return ret;
    }

    @Override
    public Provider update(Provider provider, Authentication auth) {
        Resource existing = whereID(provider.getId(), true);
        Provider ex = deserialize(existing);
        provider.setActive(ex.getActive());
        provider.setStatus(ex.getStatus());
        existing.setPayload(serialize(provider));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        authoritiesMapper.mapProviders(provider.getUsers());
        return ex;
    }

    @Override
    public Provider verifyProvider(String id, Provider.States status, Boolean active, Authentication auth) {
        Provider provider = get(id);
        User user = new User(auth);
        Template tmpl = null;
        Writer out = new StringWriter();


        switch (status) {
            case REJECTED:
                logger.info("Deleting provider: " + provider.getName());
                List<InfraService> services = this.getInfraServices(provider.getId());
                services.forEach(s -> {
                    try {
                        infraServiceService.delete(s);
                    } catch (ResourceNotFoundException e) {
                        logger.error("Error deleting Service", e);
                    }
                });
                this.delete(provider);
                return null;
            case APPROVED:
                provider.setActive(true);
                break;
            case PENDING_1:

                provider.setActive(false);
                break;
            case PENDING_2:
                provider.setActive(false);
                break;
            case REJECTED_ST:
                provider.setActive(false);
                break;
            default:
        }
        sendProviderMails(provider, user, status);

        if (active != null) {
            provider.setActive(active);
            // FIXME: temporary solution to keep service active field value when provider is deactivated, and restore it when activated
            List<InfraService> services = this.getInfraServices(provider.getId());
            if (!active) {
                for (InfraService service : services) {
                    service.setStatus(service.getActive() != null ? service.getActive().toString() : "true");
                    service.setActive(false);
                    try {
                        infraServiceService.update(service, null);
                    } catch (ResourceNotFoundException e) {
                        logger.error("Could not update service " + service.getName());
                    }
                }
            } else {
                for (InfraService service : services) {
                    service.setActive(service.getStatus() == null || service.getStatus().equals("true"));
                    service.setStatus(null);
                    try {
                        infraServiceService.update(service, null);
                    } catch (ResourceNotFoundException e) {
                        logger.error("Could not update service " + service.getName());
                    }
                }
            }
        }
        provider.setStatus(status.getKey());
        return super.update(provider, auth);
    }

    @Override
    public List<Provider> getMyServiceProviders(String email) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        return getAll(ff, null).getResults()
                .stream().map(p -> {
                    if (p.getUsers().stream().filter(Objects::nonNull).anyMatch(u -> u.getEmail().equals(email))) {
                        return p;
                    } else return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<InfraService> getInfraServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("providers", providerId);
        ff.setQuantity(10000);
        return infraServiceService.getAll(ff, null).getResults();
    }

    @Override
    public List<Service> getServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("providers", providerId);
        ff.setQuantity(10000);
        return infraServiceService.getAll(ff, null).getResults().stream().map(Service::new).collect(Collectors.toList());
    }

    @Override
    public Service getFeaturedService(String providerId) {
        List<Service> services = getServices(providerId);
        Service featuredService = null;
        if (!services.isEmpty()) {
            Random random = new Random();
            featuredService = services.get(random.nextInt(services.size()));
        }
        return featuredService;
    }

    @Override
    public List<Provider> getInactive() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(10000);
        return getAll(ff, null).getResults();
    }

    @Override
    public List<InfraService> getInactiveServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("providers", providerId);
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(10000);
        return infraServiceService.getAll(ff, null).getResults();
    }

    // TODO: complete this method
    private void sendProviderMails(Provider provider, User user, Provider.States state) {
        Map<String, Object> root = new HashMap<>();
        StringWriter out = new StringWriter();
        String providerMail = null;
        String regTeamMail = null;
        root.put("user", user);
        root.put("provider", provider);
        root.put("endpoint", endpoint);

        String providerSubject = null;
        String regTeamSubject = null;

        List<Service> serviceList = getServices(provider.getId());
        Service serviceTemplate = null;
        if (!serviceList.isEmpty()) {
            root.put("service", serviceList.get(0));
            serviceTemplate = serviceList.get(0);
        }
//        switch (Provider.States.valueOf(provider.getStatus())) {
        switch (state) {
            case INIT:
                providerSubject = String.format("[eInfraCentral] Your application for registering [%s] as a new service provider has been received", provider.getName());
                regTeamSubject = String.format("[eInfraCentral] A new application for registering [%s] as a new service provider has been submitted", provider.getName());
                break;
            case PENDING_1:
                providerSubject = String.format("[eInfraCentral] Your application for registering [%s] as a new service provider has been accepted", provider.getName());
                regTeamSubject = String.format("[eInfraCentral] The application of [%s] for registering as a new service provider has been accepted", provider.getName());
                break;
            case PENDING_2:
                assert serviceTemplate != null;
                providerSubject = String.format("[eInfraCentral] Your service [%s] has been received and its approval is pending ", serviceTemplate.getName());
                regTeamSubject = String.format("[eInfraCentral] Approve or reject the information about the new service: [%s] – [%s] ", provider.getName(), serviceTemplate.getName());
                break;
            case APPROVED:
                assert serviceTemplate != null;
                providerSubject = String.format("[eInfraCentral] Your service [%s] – [%s]  has been accepted", provider.getName(), serviceTemplate.getName());
                regTeamSubject = String.format("[eInfraCentral] The service [%s] has been accepted", serviceTemplate.getId());
                break;
            case REJECTED_ST:
                assert serviceTemplate != null;
                providerSubject = String.format("[eInfraCentral] Your service [%s] – [%s]  has been rejected", provider.getName(), serviceTemplate.getName());
                regTeamSubject = String.format("[eInfraCentral] The service [%s] has been rejected", serviceTemplate.getId());
                break;
            case REJECTED:
                providerSubject = String.format("[eInfraCentral] Your application for registering [%s] as a new service provider has been rejected", provider.getName());
                regTeamSubject = String.format("[eInfraCentral] The application of [%s] for registering as a new service provider has been rejected", provider.getName());
                break;
        }

        try {
            Template temp = cfg.getTemplate("providerMailTemplate.ftl");
            temp.process(root, out);
            providerMail = out.getBuffer().toString();
            out.flush();
//            out.close();
            mailService.sendMail(user.getEmail(), providerSubject, providerMail);
            logger.info(String.format("Recipient: %s%nTitle: %s%nMail body: %n%s", user.getEmail(), providerSubject, providerMail));
            temp = cfg.getTemplate("registrationTeamMailTemplate.ftl");
            temp.process(root, out);
            regTeamMail = out.getBuffer().toString();
            out.flush();
            mailService.sendMail("registration@einfracentral.eu", regTeamSubject, regTeamMail);
            logger.info(String.format("Recipient: %s%nTitle: %s%nMail body: %n%s", "registration@einfracentral.eu", regTeamSubject, regTeamMail));
            out.close();
        } catch (IOException e) {
            logger.error("Error finding mail template", e);
        } catch (TemplateException e) {
            logger.error("ERROR", e);
        } catch (MessagingException e) {
            logger.error("Could not send mail", e);
        }
    }
}
