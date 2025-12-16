/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProviderResourcesCommonMethods {

    private static final Logger logger = LoggerFactory.getLogger(ProviderResourcesCommonMethods.class);

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    private final CatalogueService catalogueService;
    private final ProviderService providerService;
    private final DatasourceService datasourceService;
    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;
    private final VocabularyService vocabularyService;
    private final IdCreator idCreator;

    public ProviderResourcesCommonMethods(@Lazy CatalogueService catalogueService,
                                          @Lazy ProviderService providerService,
                                          @Lazy DatasourceService datasourceService,
                                          @Lazy ResourceInteroperabilityRecordService
                                                  resourceInteroperabilityRecordService,
                                          @Lazy VocabularyService vocabularyService,
                                          @Lazy IdCreator idCreator) {
        this.catalogueService = catalogueService;
        this.providerService = providerService;
        this.datasourceService = datasourceService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.vocabularyService = vocabularyService;
        this.idCreator = idCreator;
    }

    public void checkCatalogueIdConsistency(Object o, String catalogueId) {
        if (!catalogueService.exists(catalogueId)) {
            throw new ResourceNotFoundException(catalogueId, "Catalogue");
        }
        if (o != null) {
            if (o instanceof ProviderBundle) {
                if (((ProviderBundle) o).getPayload().getCatalogueId() == null || ((ProviderBundle) o).getPayload().getCatalogueId().isEmpty()) {
                    throw new ValidationException("Provider's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((ProviderBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Provider's 'catalogueId' don't match");
                    }
                }
                if (!catalogueService.get(catalogueId).getStatus().equals("approved")) {
                    throw new ResourceException(String.format("The Catalogue '%s' is not yet approved", catalogueId),
                            HttpStatus.CONFLICT);
                }
            }
            if (o instanceof ServiceBundle) {
                if (((ServiceBundle) o).getPayload().getCatalogueId() == null || ((ServiceBundle) o).getPayload().getCatalogueId().isEmpty()) {
                    throw new ValidationException("Service's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((ServiceBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Service's 'catalogueId' don't match");
                    }
                }
            }
            if (o instanceof TrainingResourceBundle) {
                if (((TrainingResourceBundle) o).getPayload().getCatalogueId() == null ||
                        ((TrainingResourceBundle) o).getPayload().getCatalogueId().isEmpty()) {
                    throw new ValidationException("Training Resource's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((TrainingResourceBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Training Resource's 'catalogueId' don't match");
                    }
                }
            }
            if (o instanceof DeployableServiceBundle) {
                if (((DeployableServiceBundle) o).getPayload().getCatalogueId() == null ||
                        ((DeployableServiceBundle) o).getPayload().getCatalogueId().isEmpty()) {
                    throw new ValidationException("Deployable Service's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((DeployableServiceBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Deployable Service's 'catalogueId' don't match");
                    }
                }
            }
            if (o instanceof InteroperabilityRecordBundle) {
                if (((InteroperabilityRecordBundle) o).getPayload().getCatalogueId() == null ||
                        ((InteroperabilityRecordBundle) o).getPayload().getCatalogueId().isEmpty()) {
                    throw new ValidationException("Interoperability Record's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((InteroperabilityRecordBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Interoperability Record's 'catalogueId' don't match");
                    }
                }
            }
        }
    }

    //TODO: move to Resource Catalogue specific validation file
    public void suspensionValidation(Bundle<?> bundle, String catalogueId, String providerId, boolean suspend, Authentication auth) {
        if (bundle.getMetadata().isPublished()) {
            throw new ResourceException("You cannot directly suspend a Public resource", HttpStatus.FORBIDDEN);
        }

        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId, auth);
        if (bundle instanceof ProviderBundle) {
            if (catalogueBundle.isSuspended() && !suspend) {
                throw new ResourceException("You cannot unsuspend a Provider when its Catalogue is suspended",
                        HttpStatus.CONFLICT);
            }
        } else {
            if (providerId != null && !providerId.isEmpty()) {
                ProviderBundle providerBundle = providerService.get(providerId, auth);
                if ((catalogueBundle.isSuspended() || providerBundle.isSuspended()) && !suspend) {
                    throw new ResourceException("You cannot unsuspend a Resource when its Provider and/or Catalogue are suspended",
                            HttpStatus.CONFLICT);
                }
            }
        }
    }


    public List<LoggingInfo> returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(Bundle<?> bundle, Authentication auth) {
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        if (bundle.getLoggingInfo() != null && !bundle.getLoggingInfo().isEmpty()) {
            loggingInfoList = bundle.getLoggingInfo();
        } else {
            loggingInfoList.add(createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.REGISTERED.getKey()));
        }
        return loggingInfoList;
    }

    public LoggingInfo createLoggingInfo(Authentication auth, String type, String actionType) {
        return createLoggingInfo(auth, type, actionType, null);
    }

    public LoggingInfo createLoggingInfo(Authentication auth, String type, String actionType, String comment) {
        return LoggingInfo.createLoggingInfoEntry(UserInfo.of(auth), type, actionType, comment);
    }

    public void createIdentifiers(Bundle<?> bundle, String resourceType, boolean external) {
        Identifiers identifiers = new Identifiers();
        if (external) {
            identifiers.setOriginalId(bundle.getId());
            identifiers.setPid(idCreator.generate(resourceType));
        } else {
            identifiers.setOriginalId(bundle.getId());
            identifiers.setPid(bundle.getId());
        }
        bundle.setIdentifiers(identifiers);
    }

    public void blockResourceDeletion(String status, boolean isPublished) {
        if (status.equals(vocabularyService.get("pending").getId())) {
            throw new ResourceException("You cannot delete a Template that is under review", HttpStatus.FORBIDDEN);
        }
        if (isPublished) {
            throw new ResourceException("You cannot directly delete a Public Resource", HttpStatus.FORBIDDEN);
        }
    }

    public void deleteResourceRelatedServiceSubprofiles(String serviceId, String catalogueId) {
        DatasourceBundle datasourceBundle = datasourceService.get(serviceId, catalogueId);
        if (datasourceBundle != null) {
            try {
                logger.info("Deleting Datasource of Service with id: '{}'", serviceId);
                datasourceService.delete(datasourceBundle);
            } catch (ResourceNotFoundException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void deleteResourceInteroperabilityRecords(String resourceId, String resourceType) {
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.getWithResourceId(resourceId);
        if (resourceInteroperabilityRecordBundle != null) {
            try {
                logger.info("Deleting ResourceInteroperabilityRecord of {} with id: '{}'", resourceType, resourceId);
                resourceInteroperabilityRecordService.delete(resourceInteroperabilityRecordBundle);
            } catch (ResourceNotFoundException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public LoggingInfo setLatestLoggingInfo(List<LoggingInfo> loggingInfoList, String loggingInfoType) {
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
        for (LoggingInfo loggingInfo : loggingInfoList) {
            if (loggingInfo.getType().equals(loggingInfoType)) {
                return loggingInfo;
            }
        }
        return null;
    }

//    public String determineAuditState(List<LoggingInfo> loggingInfoList) {
//        List<LoggingInfo> sorted = new ArrayList<>(loggingInfoList);
//        sorted.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
//        boolean hasBeenAudited = false;
//        boolean hasBeenUpdatedAfterAudit = false;
//        String auditActionType = "";
//        int auditIndex = -1;
//        for (LoggingInfo loggingInfo : sorted) {
//            auditIndex++;
//            if (loggingInfo.getType().equals(LoggingInfo.Types.AUDIT.getKey())) {
//                hasBeenAudited = true;
//                auditActionType = loggingInfo.getActionType();
//                break;
//            }
//        }
//        // update after audit
//        if (hasBeenAudited) {
//            for (int i = 0; i < auditIndex; i++) {
//                if (sorted.get(i).getType().equals(LoggingInfo.Types.UPDATE.getKey())) {
//                    hasBeenUpdatedAfterAudit = true;
//                    break;
//                }
//            }
//        }
//
//        String auditState;
//        if (!hasBeenAudited) {
//            auditState = Auditable.NOT_AUDITED;
//        } else if (!hasBeenUpdatedAfterAudit) {
//            auditState = auditActionType.equals(LoggingInfo.ActionType.INVALID.getKey()) ?
//                    Auditable.INVALID_AND_NOT_UPDATED :
//                    Auditable.VALID;
//        } else {
//            auditState = auditActionType.equals(LoggingInfo.ActionType.INVALID.getKey()) ?
//                    Auditable.INVALID_AND_UPDATED :
//                    Auditable.VALID;
//        }
//
//        return auditState;
//    }

    public void addAuthenticatedUser(Object object, Authentication auth) {
        User authUser = User.of(auth);
        if (object instanceof Catalogue catalogue) {
            Set<User> users = catalogue.getUsers() == null ? new HashSet<>() : new HashSet<>(catalogue.getUsers());
            users.add(authUser);
            catalogue.setUsers(new ArrayList<>(users));
        } else if (object instanceof Provider provider) {
            Set<User> users = provider.getUsers() == null ? new HashSet<>() : new HashSet<>(provider.getUsers());
            users.add(authUser);
            provider.setUsers(new ArrayList<>(users));
        } else if (object instanceof Adapter adapter) {
            Set<User> users = adapter.getAdmins() == null ? new HashSet<>() : new HashSet<>(adapter.getAdmins());
            users.add(authUser);
            adapter.setAdmins(new ArrayList<>(users));
        }
    }
}