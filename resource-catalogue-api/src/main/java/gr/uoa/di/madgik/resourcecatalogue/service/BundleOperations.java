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

package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import org.springframework.security.core.Authentication;

import java.util.Comparator;
import java.util.List;

public interface BundleOperations<T extends Bundle<?>> {

    /**
     * Verify (approve/reject) a resource.
     *
     * @param id     resource ID
     * @param status The Onboarding Status of the resource
     * @param active boolean value marking a resource as Active or Inactive
     * @param auth   Authentication
     * @return {@link T}
     */
    T verify(String id, String status, Boolean active, Authentication auth);

    /**
     * Activate/Deactivate a resource
     *
     * @param id     resource ID
     * @param active boolean value marking the resource as Active or Inactive
     * @param auth   Authentication
     * @return {@link T}
     */
    T publish(String id, Boolean active, Authentication auth);

    /**
     * Has an Authenticated User accepted the Terms & Conditions
     *
     * @param id             resource ID
     * @param isDraft        boolean
     * @param authentication Authentication
     * @return <code>True</code> if Authenticated User has accepted Terms; <code>False</code> otherwise.
     */
    default boolean hasAdminAcceptedTerms(String id, boolean isDraft, Authentication authentication) {
        return false;
    }

    /**
     * Update a resource's list of Users that has accepted the Terms & Conditions
     *
     * @param id             resource ID
     * @param isDraft        boolean
     * @param authentication Authentication
     */
    default void adminAcceptedTerms(String id, boolean isDraft, Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Suspend the resource
     *
     * @param id      resource ID
     * @param suspend boolean value marking a resource as Suspended or Unsuspended
     * @param auth    Authentication
     * @return {@link T}
     */
    T suspend(String id, boolean suspend, Authentication auth);

    /**
     * Audit the resource.
     *
     * @param id         resource ID
     * @param actionType Validate or Invalidate action
     * @param auth       Authentication
     * @return {@link T}
     */
    T audit(String id, String comment, LoggingInfo.ActionType actionType, Authentication auth);

    /**
     * Get the history of a resource.
     *
     * @param bundle
     * @return
     */
    default Paging<LoggingInfo> getLoggingInfoHistory(T bundle) {
        if (bundle != null && bundle.getLoggingInfo() != null) {
            List<LoggingInfo> loggingInfoList = bundle.getLoggingInfo();
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
            return new Browsing<>(loggingInfoList.size(), 0, loggingInfoList.size(), loggingInfoList, null);
        }
        return null;
    }
}
