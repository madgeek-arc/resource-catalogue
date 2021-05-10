package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.Paging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public SecureResponseAdvice(SecurityService securityService) {
        this.securityService = securityService;
    }

    private static final Logger logger = LogManager.getLogger(SecureResponseAdvice.class);

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    // this method works only with ProviderBundle, Provider, Service and InfraService + List and Paging of these classes as well.
    @Override
    public T beforeBodyWrite(T t, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (t != null && !securityService.hasRole(auth, "ROLE_ADMIN")) {
            if (t instanceof ProviderBundle
                    && !this.securityService.isProviderAdmin(auth, ((ProviderBundle) t).getId(), true)) {
                ((ProviderBundle) t).getProvider().setMainContact(null);
                ((ProviderBundle) t).getProvider().setUsers(null);
                ((ProviderBundle) t).getMetadata().setTerms(null);
            } else if (t instanceof Provider
                    && !this.securityService.isProviderAdmin(auth, ((Provider) t).getId(), true)) {
                ((Provider) t).setMainContact(null);
                ((Provider) t).setUsers(null);
            } else if (t instanceof InfraService
                    && !this.securityService.isServiceProviderAdmin(auth, ((InfraService) t).getService().getId(), true)) {
                ((InfraService) t).getService().setMainContact(null);
                ((InfraService) t).getService().setSecurityContactEmail(null);
                ((InfraService) t).getMetadata().setTerms(null);
            } else if (t instanceof Service
                    && !this.securityService.isServiceProviderAdmin(auth, ((Service) t), true)) {
                ((Service) t).setMainContact(null);
                ((Service) t).setSecurityContactEmail(null);
            } else if (Collection.class.isAssignableFrom(t.getClass())) {
                for (Object object : ((Collection) t)) {
                    if (object instanceof Service) {
                        ((Service) object).setMainContact(null);
                        ((Service) object).setSecurityContactEmail(null);
                    } else if (object instanceof Provider) {
                        ((Provider) object).setMainContact(null);
                        ((Provider) object).setUsers(null);
                    } else if (object instanceof InfraService) {
                        ((InfraService) object).getService().setMainContact(null);
                        ((InfraService) object).getService().setSecurityContactEmail(null);
                        ((InfraService) object).getMetadata().setTerms(null);
                    } else if (object instanceof ProviderBundle) {
                        ((ProviderBundle) object).getProvider().setMainContact(null);
                        ((ProviderBundle) object).getProvider().setUsers(null);
                        ((ProviderBundle) object).getMetadata().setTerms(null);
                    } else if (object instanceof RichService) {
                        ((RichService) object).getService().setMainContact(null);
                        ((RichService) object).getService().setSecurityContactEmail(null);
                        ((RichService) object).getMetadata().setTerms(null);
                    }
                }
            } else if (Paging.class.isAssignableFrom(t.getClass())) {
                for (Object object : ((Paging) t).getResults()) {
                    if (object instanceof Service) {
                        ((Service) object).setMainContact(null);
                        ((Service) object).setSecurityContactEmail(null);
                    } else if (object instanceof Provider) {
                        ((Provider) object).setMainContact(null);
                        ((Provider) object).setUsers(null);
                    } else if (object instanceof InfraService) {
                        ((InfraService) object).getService().setMainContact(null);
                        ((InfraService) object).getService().setSecurityContactEmail(null);
                        ((InfraService) object).getMetadata().setTerms(null);
                    } else if (object instanceof ProviderBundle) {
                        ((ProviderBundle) object).getProvider().setMainContact(null);
                        ((ProviderBundle) object).getProvider().setUsers(null);
                        ((ProviderBundle) object).getMetadata().setTerms(null);
                    } else if (object instanceof RichService) {
                        ((RichService) object).getService().setMainContact(null);
                        ((RichService) object).getService().setSecurityContactEmail(null);
                        ((RichService) object).getMetadata().setTerms(null);
                    }
                }
            }
        }

        return t;
    }
}
