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

package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.service.DraftResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public abstract class DraftableResourceManager<T extends Bundle<?>> extends ResourceManager<T> implements ResourceService<T>, DraftResourceService<T> {

    private static final Logger logger = LoggerFactory.getLogger(DraftableResourceManager.class);

    @Autowired
    protected IdCreator idCreator;
    @Autowired
    protected ProviderResourcesCommonMethods commonMethods;

    public DraftableResourceManager(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    @Override
    public T transformToNonDraft(T t, Authentication auth) {
        logger.trace("Attempting to transform the Draft Provider with id '{}' to Active", t.getId());
        this.validate(t);
        if (this.exists(t.getId())) {
            throw new ResourceAlreadyExistsException(String.format("Provider with id = '%s' already exists!", t.getId()));
        }

        // update loggingInfo
        updateLoggingInfo(t, auth);

        return t;
    }

    private void updateLoggingInfo(T t, Authentication auth) {
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(t, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);
        t.setLoggingInfo(loggingInfoList);

        // latestOnboardInfo
        t.setLatestOnboardingInfo(loggingInfo);

        t.setMetadata(Metadata.updateMetadata(t.getMetadata(), AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));
    }
}
