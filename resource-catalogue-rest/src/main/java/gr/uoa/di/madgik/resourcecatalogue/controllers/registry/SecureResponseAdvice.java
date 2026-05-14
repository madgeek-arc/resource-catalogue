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

import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.AuthoritiesMapper;
import gr.uoa.di.madgik.resourcecatalogue.service.NodeResolver;
import gr.uoa.di.madgik.resourcecatalogue.service.NodeResolver.Node;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Profile("beyond")
@ControllerAdvice
public class SecureResponseAdvice<T> implements ResponseBodyAdvice<T> {

    private final SecurityService securityService;
    private final AuthoritiesMapper authoritiesMapper;
    private final NodeResolver nodeResolver;

    private final String epotEmail;
    private final String nodePid;

    public SecureResponseAdvice(SecurityService securityService, AuthoritiesMapper authoritiesMapper,
                                @Value("${catalogue.email-properties.registration-emails.to:registration@catalogue.eu}") String epotEmail,
                                @Value("${node.pid}") String nodePid,
                                NodeResolver nodeResolver) {
        this.securityService = securityService;
        this.authoritiesMapper = authoritiesMapper;
        this.epotEmail = epotEmail;
        this.nodeResolver = nodeResolver;
        this.nodePid = nodePid;
    }

    private static final Logger logger = LoggerFactory.getLogger(SecureResponseAdvice.class);

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    @Override
    public T beforeBodyWrite(T t, MethodParameter methodParameter,
                             MediaType mediaType,
                             Class<? extends HttpMessageConverter<?>> aClass,
                             ServerHttpRequest serverHttpRequest,
                             ServerHttpResponse serverHttpResponse) {
        if (t != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // TODO: remove when implemented correctly
            fixNodeFacets(t, resolveNodeName());

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
        return null;
    }

    private String resolveNodeName() {
        Node node = nodeResolver.fetchNodes().stream().filter(f -> f.pid().equals(nodePid)).findFirst().orElseThrow();
        return node.name();
    }

    private void fixNodeFacets(T t, String nodeLabel) {
        if (Paging.class.isAssignableFrom(t.getClass())) {
            Facet nodeFacet = ((Paging<?>) t).getFacets().stream().filter(f -> f.getField().equals("node")).findFirst().orElse(null);
            if (nodeFacet != null) {
                for (gr.uoa.di.madgik.registry.domain.Value value : nodeFacet.getValues()) {
                    if (!nodeLabel.equalsIgnoreCase(value.getLabel())) {
                        value.setLabel("%s (%s)".formatted(nodeLabel, value.getLabel()));
                    }
                }
            }
        }
    }

    //TODO: enable for LinkedHasMap too
    protected void modifyContent(T t, Authentication auth) {
        if (t instanceof OrganisationBundle) {
            modifyOrganisationBundle(t, auth);
        } else if (t instanceof AdapterBundle) {
            modifyAdapterBundle(t, auth);
        } else if (t instanceof ServiceBundle) {
            modifyServiceBundle(t, auth);
        } else if (t instanceof CatalogueBundle) {
            modifyCatalogueBundle(t, auth);
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

    @SuppressWarnings("unchecked")
    private void modifyOrganisationBundle(T bundle, Authentication auth) {
        modifyLoggingInfoList((T) ((OrganisationBundle) bundle).getLoggingInfo());
        modifyLoggingInfo((T) ((OrganisationBundle) bundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((OrganisationBundle) bundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((OrganisationBundle) bundle).getLatestOnboardingInfo());

        if (!this.securityService.hasAdminAccess(auth, ((OrganisationBundle) bundle).getId())) {
            LinkedHashMap<String, Object> org = ((OrganisationBundle) bundle).getOrganisation();
            nullifyMainContactEmails(org);
            org.put("users", null);
            ((OrganisationBundle) bundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyAdapterBundle(T bundle, Authentication auth) {
        modifyLoggingInfoList((T) ((AdapterBundle) bundle).getLoggingInfo());
        modifyLoggingInfo((T) ((AdapterBundle) bundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((AdapterBundle) bundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((AdapterBundle) bundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceAdmin(auth, ((AdapterBundle) bundle).getId())) {
            nullifyCreatorEmails(((AdapterBundle) bundle).getAdapter());
            ((AdapterBundle) bundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyServiceBundle(T bundle, Authentication auth) {
        modifyLoggingInfoList((T) ((ServiceBundle) bundle).getLoggingInfo());
        modifyLoggingInfo((T) ((ServiceBundle) bundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((ServiceBundle) bundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((ServiceBundle) bundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceAdmin(auth, ((ServiceBundle) bundle).getId())) {
            nullifyMainContactEmails(((ServiceBundle) bundle).getService());
            ((ServiceBundle) bundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyCatalogueBundle(T bundle, Authentication auth) {
        modifyLoggingInfoList((T) ((CatalogueBundle) bundle).getLoggingInfo());
        modifyLoggingInfo((T) ((CatalogueBundle) bundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((CatalogueBundle) bundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((CatalogueBundle) bundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceAdmin(auth, ((CatalogueBundle) bundle).getId())) {
            nullifyMainContactEmails(((CatalogueBundle) bundle).getCatalogue());
            ((CatalogueBundle) bundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyDatasourceBundle(T bundle, Authentication auth) {
        modifyLoggingInfoList((T) ((DatasourceBundle) bundle).getLoggingInfo());
        modifyLoggingInfo((T) ((DatasourceBundle) bundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((DatasourceBundle) bundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((DatasourceBundle) bundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceAdmin(auth, ((DatasourceBundle) bundle).getId())) {
            nullifyMainContactEmails(((DatasourceBundle) bundle).getDatasource());
            ((DatasourceBundle) bundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyTrainingResourceBundle(T bundle, Authentication auth) {
        modifyLoggingInfoList((T) ((TrainingResourceBundle) bundle).getLoggingInfo());
        modifyLoggingInfo((T) ((TrainingResourceBundle) bundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((TrainingResourceBundle) bundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((TrainingResourceBundle) bundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceAdmin(auth, ((TrainingResourceBundle) bundle).getId())) {
            nullifyCreatorEmails(((TrainingResourceBundle) bundle).getTrainingResource());
            ((TrainingResourceBundle) bundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyDeployableApplicationBundle(T bundle, Authentication auth) {
        modifyLoggingInfoList((T) ((DeployableApplicationBundle) bundle).getLoggingInfo());
        modifyLoggingInfo((T) ((DeployableApplicationBundle) bundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((DeployableApplicationBundle) bundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((DeployableApplicationBundle) bundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceAdmin(auth, ((DeployableApplicationBundle) bundle).getId())) {
            nullifyCreatorEmails(((DeployableApplicationBundle) bundle).getDeployableApplication());
            ((DeployableApplicationBundle) bundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyInteroperabilityRecordBundle(T bundle, Authentication auth) {
        modifyLoggingInfoList((T) ((InteroperabilityRecordBundle) bundle).getLoggingInfo());
        modifyLoggingInfo((T) ((InteroperabilityRecordBundle) bundle).getLatestAuditInfo());
        modifyLoggingInfo((T) ((InteroperabilityRecordBundle) bundle).getLatestUpdateInfo());
        modifyLoggingInfo((T) ((InteroperabilityRecordBundle) bundle).getLatestOnboardingInfo());

        if (!this.securityService.isResourceAdmin(auth, ((InteroperabilityRecordBundle) bundle).getId())) {
            nullifyCreatorEmails(((InteroperabilityRecordBundle) bundle).getInteroperabilityRecord());
            ((InteroperabilityRecordBundle) bundle).getMetadata().setTerms(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void nullifyMainContactEmails(LinkedHashMap<String, Object> resource) {
        Map<String, Object> mainContact = (Map<String, Object>) resource.get("mainContact");
        if (mainContact != null) {
            mainContact.put("email", null);
        }
    }

    @SuppressWarnings("unchecked")
    private void nullifyCreatorEmails(LinkedHashMap<String, Object> resource) {
        List<Map<String, Object>> creators = (List<Map<String, Object>>) resource.get("creators");
        if (creators != null) {
            for (Map<String, Object> creator : creators) {
                creator.put("email", null);
            }
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
