package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.Paging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    // TODO: this method works only with ProviderBundle, Provider, Service and InfraService
    //       make it work for Lists and Paging of these classes as well.
    @Override
    public T beforeBodyWrite(T t, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            if (t instanceof ProviderBundle
                    && !this.securityService.userIsProviderAdmin(auth, ((ProviderBundle) t).getId())) {
                ((ProviderBundle) t).getProvider().setMainContact(null);
                ((ProviderBundle) t).getProvider().setUsers(null);
            } else if (t instanceof Provider
                    && !this.securityService.userIsProviderAdmin(auth, ((Provider) t).getId())) {
                ((Provider) t).setMainContact(null);
                ((Provider) t).setUsers(null);
            } else if (t instanceof InfraService
                    && !this.securityService.userIsServiceProviderAdmin(auth, ((InfraService) t).getService())) {
                ((InfraService) t).getService().setMainContact(null);
                ((InfraService) t).getService().setSecurityContactEmail(null);
            } else if (t instanceof Service
                    && !this.securityService.userIsServiceProviderAdmin(auth, ((Service) t))) {
                ((Service) t).setMainContact(null);
                ((Service) t).setSecurityContactEmail(null);
            } else if (Collection.class.isAssignableFrom(t.getClass())) {
                // TODO
            } else if (Paging.class.isAssignableFrom(t.getClass())) {
                // TODO
            }
        }

        return t;
    }
}
