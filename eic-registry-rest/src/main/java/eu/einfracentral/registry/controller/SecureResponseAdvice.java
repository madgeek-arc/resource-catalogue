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
    private static final Logger logger = LogManager.getLogger(SecureResponseAdvice.class);

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    // TODO: this method works only with ProviderBundle, Provider, Service and InfraService
    //       make it work for Lists and Paging of these classes as well.
    @Override
    public T beforeBodyWrite(T t, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (t != null && !auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
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
                if (t instanceof Service){
                    ((Service) t).setMainContact(null);
                    ((Service) t).setSecurityContactEmail(null);
                } else if (t instanceof Provider){
                    ((Provider) t).setMainContact(null);
                    ((Provider) t).setUsers(null);
                } else if (t instanceof InfraService){
                    ((InfraService) t).getService().setMainContact(null);
                    ((InfraService) t).getService().setSecurityContactEmail(null);
                } else if (t instanceof ProviderBundle){
                    ((ProviderBundle) t).getProvider().setMainContact(null);
                    ((ProviderBundle) t).getProvider().setUsers(null);
                } else if (t instanceof RichService){
                    ((RichService) t).getService().setMainContact(null);
                    ((RichService) t).getService().setSecurityContactEmail(null);
                }
            } else if (Paging.class.isAssignableFrom(t.getClass())) {
                for (Object object : ((Paging) t).getResults()){
                    if (object instanceof Service){
                        ((Service) object).setMainContact(null);
                        ((Service) object).setSecurityContactEmail(null);
                    } else if (object instanceof Provider){
                        ((Provider) object).setMainContact(null);
                        ((Provider) object).setUsers(null);
                    } else if (object instanceof InfraService){
                        ((InfraService) object).getService().setMainContact(null);
                        ((InfraService) object).getService().setSecurityContactEmail(null);
                    } else if (object instanceof ProviderBundle){
                        ((ProviderBundle) object).getProvider().setMainContact(null);
                        ((ProviderBundle) object).getProvider().setUsers(null);
                    } else if (object instanceof RichService){
                        ((RichService) object).getService().setMainContact(null);
                        ((RichService) object).getService().setSecurityContactEmail(null);
                    }
                }
            }
        }

        return t;
    }
}
