/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.domain.deprecated.Adapter;
import gr.uoa.di.madgik.resourcecatalogue.service.AuthoritiesMapper;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public SecureResponseAdvice(SecurityService securityService, AuthoritiesMapper authoritiesMapper,
                                @Value("${catalogue.email-properties.registration-emails.to:registration@catalogue.eu}") String epotEmail) {
        this.securityService = securityService;
        this.authoritiesMapper = authoritiesMapper;
        this.epotEmail = epotEmail;
    }

    private static final Logger logger = LoggerFactory.getLogger(SecureResponseAdvice.class);

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    //TODO: test
    @Override
    public T beforeBodyWrite(T t, MethodParameter methodParameter,
                             MediaType mediaType,
                             Class<? extends HttpMessageConverter<?>> aClass,
                             ServerHttpRequest serverHttpRequest,
                             ServerHttpResponse serverHttpResponse) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (t != null && !securityService.hasPortalAdminRole(auth)) {
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

    //TODO: enable for LinkedHasMap too
    protected void modifyContent(T t, Authentication auth) {
        //FIXME
//        if (t instanceof CatalogueBundle) {
//            modifyCatalogueBundle(t, auth);
//        }
        if (t instanceof OrganisationBundle) {
            modifyProviderBundle(t, auth);
        } else if (t instanceof Adapter) {
            modifyAdapterBundle(t, auth);
        } else if (t instanceof ServiceBundle) {
            modifyServiceBundle(t, auth);
        } else if (t instanceof DatasourceBundle) {
            modifyDatasourceBundle(t, auth);
        } else if (t instanceof TrainingResourceBundle) {
            modifyTrainingResourceBundle(t, auth);
        } else if (t instanceof DeployableApplicationBundle) {
            modifyDeployableApplicationBundle(t, auth);
        } else if (t instanceof InteroperabilityRecordBundle) {
            modifyInteroperabilityRecordBundle(t, auth);
        } else if (t instanceof LoggingInfo) {
            modifyLoggingInfo(t);
        }
    }

    //FIXME
//    @SuppressWarnings("unchecked")
//    private void modifyCatalogueBundle(T bundle, Authentication auth) {
//        modifyLoggingInfoList((T) ((CatalogueBundle) bundle).getLoggingInfo());
//        modifyLoggingInfo((T) ((CatalogueBundle) bundle).getLatestAuditInfo());
//        modifyLoggingInfo((T) ((CatalogueBundle) bundle).getLatestUpdateInfo());
//        modifyLoggingInfo((T) ((CatalogueBundle) bundle).getLatestOnboardingInfo());
//
//        if (!this.securityService.hasAdminAccess(auth, ((CatalogueBundle) bundle).getId())) {
//            ((CatalogueBundle) bundle).getCatalogue().setMainContact(null);
//            ((CatalogueBundle) bundle).getCatalogue().setUsers(null);
//            ((CatalogueBundle) bundle).getMetadata().setTerms(null);
//        }
//    }

    @SuppressWarnings("unchecked")
    private void modifyProviderBundle(T bundle, Authentication auth) {
        modifyLoggingInfoList((T) ((OrganisationBundle) bundle).getLoggingInfo());
        modifyLoggingInfo((T) ((OrganisationBundle) bundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((OrganisationBundle) bundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((OrganisationBundle) bundle).getLatestOnboardingInfo());

        if (!this.securityService.hasAdminAccess(auth, ((OrganisationBundle) bundle).getId())) {
            ((OrganisationBundle) bundle).getOrganisation().put("email", null);
            ((OrganisationBundle) bundle).getOrganisation().put("users", null);
            ((OrganisationBundle) bundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyAdapterBundle(T bundle, Authentication auth) {
        modifyLoggingInfoList((T) ((OrganisationBundle) bundle).getLoggingInfo());
        modifyLoggingInfo((T) ((OrganisationBundle) bundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((OrganisationBundle) bundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((OrganisationBundle) bundle).getLatestOnboardingInfo());

        if (!this.securityService.hasAdminAccess(auth, ((OrganisationBundle) bundle).getId())) {
            ((OrganisationBundle) bundle).getOrganisation().put("email", null);
            ((OrganisationBundle) bundle).getOrganisation().put("users", null);
            ((OrganisationBundle) bundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyServiceBundle(T serviceBundle, Authentication auth) {
        modifyLoggingInfoList((T) ((ServiceBundle) serviceBundle).getLoggingInfo());
        modifyLoggingInfo((T) ((ServiceBundle) serviceBundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((ServiceBundle) serviceBundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((ServiceBundle) serviceBundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceAdmin(auth, ((ServiceBundle) serviceBundle).getId())) {
            ((ServiceBundle) serviceBundle).getService().put("email", null);
            ((ServiceBundle) serviceBundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyDatasourceBundle(T datasourceBundle, Authentication auth) {
        modifyLoggingInfoList((T) ((DatasourceBundle) datasourceBundle).getLoggingInfo());
        modifyLoggingInfo((T) ((DatasourceBundle) datasourceBundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((DatasourceBundle) datasourceBundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((DatasourceBundle) datasourceBundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceAdmin(auth, ((DatasourceBundle) datasourceBundle).getId())) {
            ((DatasourceBundle) datasourceBundle).getDatasource().put("email", null);
            ((DatasourceBundle) datasourceBundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyTrainingResourceBundle(T trainingResourceBundle, Authentication auth) {
        modifyLoggingInfoList((T) ((TrainingResourceBundle) trainingResourceBundle).getLoggingInfo());
        modifyLoggingInfo((T) ((TrainingResourceBundle) trainingResourceBundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((TrainingResourceBundle) trainingResourceBundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((TrainingResourceBundle) trainingResourceBundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceAdmin(auth, ((TrainingResourceBundle) trainingResourceBundle).getId())) {
            ((TrainingResourceBundle) trainingResourceBundle).getTrainingResource().put("email", null);
            ((TrainingResourceBundle) trainingResourceBundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyDeployableApplicationBundle(T deployableApplicationBundle, Authentication auth) {
        modifyLoggingInfoList((T) ((DeployableApplicationBundle) deployableApplicationBundle).getLoggingInfo());
        modifyLoggingInfo((T) ((DeployableApplicationBundle) deployableApplicationBundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((DeployableApplicationBundle) deployableApplicationBundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((DeployableApplicationBundle) deployableApplicationBundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceAdmin(auth, ((DeployableApplicationBundle) deployableApplicationBundle).getId())) {
            ((DeployableApplicationBundle) deployableApplicationBundle).getDeployableApplication().put("email", null);
            ((DeployableApplicationBundle) deployableApplicationBundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyInteroperabilityRecordBundle(T interoperabilityRecordBundle, Authentication auth) {
        modifyLoggingInfoList((T) ((InteroperabilityRecordBundle) interoperabilityRecordBundle).getLoggingInfo());
        modifyLoggingInfo((T) ((InteroperabilityRecordBundle) interoperabilityRecordBundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((InteroperabilityRecordBundle) interoperabilityRecordBundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((InteroperabilityRecordBundle) interoperabilityRecordBundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceAdmin(auth, ((InteroperabilityRecordBundle) interoperabilityRecordBundle).getId())) {
            ((InteroperabilityRecordBundle) interoperabilityRecordBundle).getInteroperabilityRecord().put("email", null);
            ((InteroperabilityRecordBundle) interoperabilityRecordBundle).getMetadata().setTerms(null);
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
