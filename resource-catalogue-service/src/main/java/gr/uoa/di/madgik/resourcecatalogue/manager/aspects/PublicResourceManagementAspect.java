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

package gr.uoa.di.madgik.resourcecatalogue.manager.aspects;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.PublicServiceService;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PublicResourceManagementAspect<T extends Bundle<?>> {

    private static final Logger logger = LoggerFactory.getLogger(PublicResourceManagementAspect.class);

    private final PublicServiceService publicServiceManager;

    public PublicResourceManagementAspect(PublicServiceService publicServiceManager) {
        this.publicServiceManager = publicServiceManager;
    }

    @Async
    @AfterReturning(pointcut = "(execution(* gr.uoa.di.madgik.resourcecatalogue.manager.AbstractServiceBundleManager.updateEOSCIFGuidelines" +
            "(String, String, java.util.List<gr.uoa.di.madgik.resourcecatalogue.domain.EOSCIFGuidelines>, org.springframework.security.core.Authentication)))",
            returning = "serviceBundle")
    public void updatePublicResourceAfterResourceExtrasUpdate(ServiceBundle serviceBundle) {
        publicServiceManager.update(serviceBundle, null);
    }

}
