package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.AuthoritiesMapper;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
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

@Profile("beyond")
@ControllerAdvice
public class SecureResponseAdvice<T> implements ResponseBodyAdvice<T> {

    private final SecurityService securityService;
    private final AuthoritiesMapper authoritiesMapper;

    private final String epotEmail;

    @Autowired
    public SecureResponseAdvice(SecurityService securityService, AuthoritiesMapper authoritiesMapper,
                                @Value("${catalogue.email-properties.registration-emails.to:registration@catalogue.eu}") String epotEmail) {
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
        if (t instanceof CatalogueBundle) {
            modifyCatalogueBundle(t, auth);
        } else if (t instanceof Catalogue) {
            modifyCatalogue(t, auth);
        } else if (t instanceof ProviderBundle) {
            modifyProviderBundle(t, auth);
        } else if (t instanceof Provider) {
            modifyProvider(t, auth);
        } else if (t instanceof ServiceBundle) {
            modifyServiceBundle(t, auth);
        } else if (t instanceof Service) {
            modifyService(t, auth);
        } else if (t instanceof TrainingResourceBundle) {
            modifyTrainingResourceBundle(t, auth);
        } else if (t instanceof TrainingResource) {
            modifyTrainingResource(t, auth);
        } else if (t instanceof InteroperabilityRecordBundle) {
            modifyInteroperabilityRecordBundle(t, auth);
        } else if (t instanceof LoggingInfo) {
            modifyLoggingInfo(t);
        }
    }

    private void modifyService(T service, Authentication auth) {
        if (!this.securityService.isResourceProviderAdmin(auth, ((Service) service).getId(), ((Service) service).getCatalogueId())) {
            ((Service) service).setMainContact(null);
            ((Service) service).setSecurityContactEmail(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyServiceBundle(T serviceBundle, Authentication auth) {
        modifyLoggingInfoList((T) ((ServiceBundle) serviceBundle).getLoggingInfo());
        modifyLoggingInfo((T) ((ServiceBundle) serviceBundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((ServiceBundle) serviceBundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((ServiceBundle) serviceBundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceProviderAdmin(auth, ((ServiceBundle) serviceBundle).getId(), ((ServiceBundle) serviceBundle).getService().getCatalogueId())) {
            ((ServiceBundle) serviceBundle).getService().setMainContact(null);
            ((ServiceBundle) serviceBundle).getService().setSecurityContactEmail(null);
            ((ServiceBundle) serviceBundle).getMetadata().setTerms(null);
        }
    }

    private void modifyTrainingResource(T trainingResource, Authentication auth) {
        if (!this.securityService.isResourceProviderAdmin(auth, ((TrainingResource) trainingResource).getId(), ((TrainingResource) trainingResource).getCatalogueId())) {
            ((TrainingResource) trainingResource).setContact(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyTrainingResourceBundle(T trainingResourceBundle, Authentication auth) {
        modifyLoggingInfoList((T) ((TrainingResourceBundle) trainingResourceBundle).getLoggingInfo());
        modifyLoggingInfo((T) ((TrainingResourceBundle) trainingResourceBundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((TrainingResourceBundle) trainingResourceBundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((TrainingResourceBundle) trainingResourceBundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceProviderAdmin(auth, ((TrainingResourceBundle) trainingResourceBundle).getId(), ((TrainingResourceBundle) trainingResourceBundle).getTrainingResource().getCatalogueId())) {
            ((TrainingResourceBundle) trainingResourceBundle).getTrainingResource().setContact(null);
            ((TrainingResourceBundle) trainingResourceBundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyInteroperabilityRecordBundle(T interoperabilityRecordBundle, Authentication auth) {
        modifyLoggingInfoList((T) ((InteroperabilityRecordBundle) interoperabilityRecordBundle).getLoggingInfo());
        modifyLoggingInfo((T) ((InteroperabilityRecordBundle) interoperabilityRecordBundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((InteroperabilityRecordBundle) interoperabilityRecordBundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((InteroperabilityRecordBundle) interoperabilityRecordBundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceProviderAdmin(auth, ((InteroperabilityRecordBundle) interoperabilityRecordBundle).getId(), ((InteroperabilityRecordBundle) interoperabilityRecordBundle).getInteroperabilityRecord().getCatalogueId())) {
            ((InteroperabilityRecordBundle) interoperabilityRecordBundle).getMetadata().setTerms(null);
        }
    }

    private void modifyProvider(T provider, Authentication auth) {
        if (!this.securityService.isProviderAdmin(auth, ((Provider) provider).getId(), ((Provider) provider).getCatalogueId())) {
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

        if (!this.securityService.isProviderAdmin(auth, ((ProviderBundle) bundle).getId(), ((Bundle<Provider>) bundle).getPayload().getCatalogueId())) {
            ((ProviderBundle) bundle).getProvider().setMainContact(null);
            ((ProviderBundle) bundle).getProvider().setUsers(null);
            ((ProviderBundle) bundle).getMetadata().setTerms(null);
        }
    }

    private void modifyCatalogue(T catalogue, Authentication auth) {
        if (!this.securityService.isCatalogueAdmin(auth, ((Catalogue) catalogue).getId(), true)) {
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

        if (!this.securityService.isCatalogueAdmin(auth, ((CatalogueBundle) bundle).getId(), true)) {
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
