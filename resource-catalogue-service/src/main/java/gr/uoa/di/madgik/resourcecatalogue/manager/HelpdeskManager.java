/**
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

package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.HelpdeskBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.ResourceValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;

import java.util.List;

@org.springframework.stereotype.Service("helpdeskManager")
public class HelpdeskManager extends ResourceCatalogueManager<HelpdeskBundle> implements HelpdeskService {

    private static final Logger logger = LoggerFactory.getLogger(HelpdeskManager.class);
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final PublicHelpdeskService publicHelpdeskManager;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final IdCreator idCreator;

    public HelpdeskManager(ServiceBundleService<ServiceBundle> serviceBundleService,
                           TrainingResourceService trainingResourceService,
                           PublicHelpdeskService publicHelpdeskManager,
                           @Lazy SecurityService securityService,
                           @Lazy RegistrationMailService registrationMailService,
                           ProviderResourcesCommonMethods commonMethods,
                           IdCreator idCreator) {
        super(HelpdeskBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.publicHelpdeskManager = publicHelpdeskManager;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.commonMethods = commonMethods;
        this.idCreator = idCreator;
    }

    @Override
    public String getResourceTypeName() {
        return "helpdesk";
    }

    @Override
    public HelpdeskBundle validate(HelpdeskBundle helpdeskBundle, String resourceType) {
        String resourceId = helpdeskBundle.getHelpdesk().getServiceId();
        String catalogueId = helpdeskBundle.getCatalogueId();

        HelpdeskBundle existingHelpdesk = get(resourceId, catalogueId);
        if (existingHelpdesk != null) {
            throw new ValidationException(String.format("Resource [%s] of the Catalogue [%s] has already a Helpdesk " +
                    "registered, with id: [%s]", resourceId, catalogueId, existingHelpdesk.getId()));
        }

        // check if Resource exists and if User belongs to Resource's Provider Admins
        if (resourceType.equals("service")) {
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, serviceBundleService, resourceType);
        } else if (resourceType.equals("training_resource")) {
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, trainingResourceService, resourceType);
        } else {
            throw new ValidationException("Field resourceType should be either 'service' or 'training_resource'");
        }
        return super.validate(helpdeskBundle);
    }

    @Override
    public HelpdeskBundle add(HelpdeskBundle helpdesk, String resourceType, Authentication auth) {
        validate(helpdesk, resourceType);

        helpdesk.setId(idCreator.generate(getResourceTypeName()));
        logger.trace("Attempting to add a new Helpdesk: {}", helpdesk);

        helpdesk.setMetadata(Metadata.createMetadata(AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));
        commonMethods.createIdentifiers(helpdesk, getResourceTypeName(), false);
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(helpdesk, auth);
        helpdesk.setLoggingInfo(loggingInfoList);
        helpdesk.setActive(true);
        // latestOnboardingInfo
        helpdesk.setLatestOnboardingInfo(loggingInfoList.getFirst());

        super.add(helpdesk, null);
        logger.info("Added Helpdesk with id '{}'", helpdesk.getId());

        registrationMailService.sendEmailsForHelpdeskExtensionToPortalAdmins(helpdesk, "post");

        return helpdesk;
    }

    @Override
    public HelpdeskBundle get(String serviceId, String catalogueId) {
        Resource res = where(false, new SearchService.KeyValue("service_id", serviceId), new SearchService.KeyValue("catalogue_id", catalogueId));
        return res != null ? deserialize(res) : null;
    }

    @Override
    public HelpdeskBundle update(HelpdeskBundle helpdeskBundle, Authentication auth) {
        logger.trace("Attempting to update the Helpdesk with id '{}'", helpdeskBundle.getId());

        HelpdeskBundle ret = ObjectUtils.clone(helpdeskBundle);
        Resource existingResource = whereID(ret.getId(), true);
        HelpdeskBundle existingHelpdesk = deserialize(existingResource);
        // check if there are actual changes in the Helpdesk
        if (ret.getHelpdesk().equals(existingHelpdesk.getHelpdesk())) {
            return ret;
        }

        validate(ret);
        ret.setMetadata(Metadata.updateMetadata(ret.getMetadata(), AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));
        ret.setIdentifiers(existingHelpdesk.getIdentifiers());
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(ret, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());
        loggingInfoList.add(loggingInfo);
        ret.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        ret.setActive(existingHelpdesk.isActive());
        existingResource.setPayload(serialize(ret));
        existingResource.setResourceType(getResourceType());

        // block user from updating serviceId
        if (!ret.getHelpdesk().getServiceId().equals(existingHelpdesk.getHelpdesk().getServiceId()) && !securityService.hasRole(auth, "ROLE_ADMIN")) {
            throw new ValidationException("You cannot change the Service Id with which this Helpdesk is related");
        }

        resourceService.updateResource(existingResource);
        logger.info("Updated Helpdesk with id '{}'", ret.getId());

        registrationMailService.sendEmailsForHelpdeskExtensionToPortalAdmins(ret, "put");

        return ret;
    }

    public void updateBundle(HelpdeskBundle helpdeskBundle, Authentication auth) {
        logger.trace("Attempting to update the Helpdesk: {}", helpdeskBundle);

        Resource existing = getResource(helpdeskBundle.getId());
        if (existing == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not update Helpdesk with id '%s' because it does not exist",
                            helpdeskBundle.getId()));
        }

        existing.setPayload(serialize(helpdeskBundle));
        existing.setResourceType(getResourceType());

        resourceService.updateResource(existing);
    }

    @Override
    public void delete(HelpdeskBundle helpdesk) {
        super.delete(helpdesk);
        logger.info("Deleted Helpdesk with id '{}' of the Catalogue '{}'",
                helpdesk.getHelpdesk().getId(), helpdesk.getCatalogueId());
    }

    public HelpdeskBundle createPublicResource(HelpdeskBundle helpdeskBundle, Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Helpdesk from Helpdesk '{}' of the '{}' Catalogue",
                AuthenticationInfo.getFullName(auth),
                AuthenticationInfo.getEmail(auth).toLowerCase(),
                helpdeskBundle.getId(), helpdeskBundle.getCatalogueId());
        publicHelpdeskManager.add(helpdeskBundle, auth);
        return helpdeskBundle;
    }
}
