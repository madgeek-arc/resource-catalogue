package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.service.AuthoritiesMapper;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.Paging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Collection;

@ControllerAdvice
public class SecureResponseAdvice<T> implements ResponseBodyAdvice<T> {

    private final SecurityService securityService;
    private final AuthoritiesMapper authoritiesMapper;

    private final String epotEmail;

    @Autowired
    public SecureResponseAdvice(SecurityService securityService, AuthoritiesMapper authoritiesMapper,
                                @Value("${project.registration.email:registration@catalogue.eu}") String epotEmail) {
        this.securityService = securityService;
        this.authoritiesMapper = authoritiesMapper;
        this.epotEmail = epotEmail;
    }

    private static final Logger logger = LogManager.getLogger(SecureResponseAdvice.class);

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    @Override
    public T beforeBodyWrite(T t, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (t != null && !securityService.hasRole(auth, "ROLE_ADMIN") && !securityService.hasRole(auth, "ROLE_EPOT")) {
            logger.trace("User is not Admin nor EPOT: attempting to remove sensitive information");
            if (Collection.class.isAssignableFrom(t.getClass())) {
                for (T object : ((Collection<T>) t)) {
                    modifyContent(object, auth);
                }
            } else if (Paging.class.isAssignableFrom(t.getClass())) {
                for (T object : ((Paging<T>) t).getResults()) {
                    modifyContent(object, auth);
                }
            } else {
                modifyContent(t, auth);
            }
            logger.debug("Final Object: {}", t);
        }

        return t;
    }

    protected void modifyContent(T t, Authentication auth) {
        if (t instanceof Service) {
            modifyService(t, auth);
        } else if (t instanceof Provider) {
            modifyProvider(t, auth);
        } else if (t instanceof InfraService) {
            modifyInfraService(t, auth);
        } else if (t instanceof ProviderBundle) {
            modifyProviderBundle(t, auth);
        } else if (t instanceof RichService) {
            modifyRichService(t, auth);
        } else if (t instanceof LoggingInfo) {
            modifyLoggingInfo(t);
        } else if (t instanceof Catalogue) {
            modifyCatalogue(t, auth);
        } else if (t instanceof CatalogueBundle) {
            modifyCatalogueBundle(t, auth);
        }
    }

    private void modifyService(T service, Authentication auth) {
        if (!this.securityService.isServiceProviderAdmin(auth, ((Service) service), true)) {
            ((Service) service).setMainContact(null);
            ((Service) service).setSecurityContactEmail(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyInfraService(T infraService, Authentication auth) {
        modifyLoggingInfoList((T) ((InfraService) infraService).getLoggingInfo());
        modifyLoggingInfo((T) ((InfraService) infraService).getLatestAuditInfo());
        modifyLoggingInfo((T) ((InfraService) infraService).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((InfraService) infraService).getLatestOnboardingInfo());

        if (!this.securityService.isServiceProviderAdmin(auth, ((InfraService) infraService).getService().getId(), true)) {
            ((InfraService) infraService).getService().setMainContact(null);
            ((InfraService) infraService).getService().setSecurityContactEmail(null);
            ((InfraService) infraService).getMetadata().setTerms(null);
        }
    }

    private void modifyRichService(T richService, Authentication auth) {
        if (!this.securityService.isServiceProviderAdmin(auth, ((RichService) richService).getService().getId(), true)) {
            ((RichService) richService).getService().setMainContact(null);
            ((RichService) richService).getService().setSecurityContactEmail(null);
            ((RichService) richService).getMetadata().setTerms(null);
        }
    }

    private void modifyProvider(T provider, Authentication auth) {
        if(!this.securityService.isProviderAdmin(auth, ((Provider) provider).getId(), true)) {
            ((Provider) provider).setMainContact(null);
            ((Provider) provider).setUsers(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyProviderBundle(T bundle, Authentication auth) {
        modifyLoggingInfoList((T) ((ProviderBundle) bundle).getLoggingInfo());
        modifyLoggingInfo((T) ((ProviderBundle) bundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((ProviderBundle) bundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((ProviderBundle) bundle).getLatestOnboardingInfo());

        if(!this.securityService.isProviderAdmin(auth, ((ProviderBundle) bundle).getId(), true)) {
            ((ProviderBundle) bundle).getProvider().setMainContact(null);
            ((ProviderBundle) bundle).getProvider().setUsers(null);
            ((ProviderBundle) bundle).getMetadata().setTerms(null);
        }
    }

    private void modifyCatalogue(T catalogue, Authentication auth) {
        if(!this.securityService.isCatalogueAdmin(auth, ((Catalogue) catalogue).getId(), true)) {
            ((Catalogue) catalogue).setMainContact(null);
            ((Catalogue) catalogue).setUsers(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyCatalogueBundle(T bundle, Authentication auth) {
        modifyLoggingInfoList((T) ((CatalogueBundle) bundle).getLoggingInfo());
        modifyLoggingInfo((T) ((CatalogueBundle) bundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((CatalogueBundle) bundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((CatalogueBundle) bundle).getLatestOnboardingInfo());

        if(!this.securityService.isCatalogueAdmin(auth, ((CatalogueBundle) bundle).getId(), true)) {
            ((CatalogueBundle) bundle).getCatalogue().setMainContact(null);
            ((CatalogueBundle) bundle).getCatalogue().setUsers(null);
            ((CatalogueBundle) bundle).getMetadata().setTerms(null);
        }
    }

    private void modifyLoggingInfo(T loggingInfo) {
        if (loggingInfo != null) {
            if (authoritiesMapper.isAdmin(((LoggingInfo) loggingInfo).getUserEmail())) {
                ((LoggingInfo) loggingInfo).setUserEmail(epotEmail);
                ((LoggingInfo) loggingInfo).setUserFullName("Administrator");
            } else if (authoritiesMapper.isEPOT(((LoggingInfo) loggingInfo).getUserEmail())) {
                ((LoggingInfo) loggingInfo).setUserEmail(epotEmail);
                ((LoggingInfo) loggingInfo).setUserFullName("EPOT");
            } else {
                ((LoggingInfo) loggingInfo).setUserEmail(null);
            }

            ((LoggingInfo) loggingInfo).setUserRole(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyLoggingInfoList(T loggingInfoList) {
        if (loggingInfoList != null) {
            for (T loggingInfo : (Collection<T>) loggingInfoList) {
                modifyLoggingInfo(loggingInfo);
            }
        }
    }
}
