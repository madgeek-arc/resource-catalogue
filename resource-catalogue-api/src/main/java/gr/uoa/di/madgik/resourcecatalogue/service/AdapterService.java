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

package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import org.springframework.security.core.Authentication;

public interface AdapterService extends ResourceCatalogueGenericService<AdapterBundle>,
        EOSCResourceService<AdapterBundle>,
        TemplateOnboardingService, DraftService<AdapterBundle> {

    /**
     * Moves an Adapter to a different owner Organisation. The target Organisation must exist in the
     * Adapter's catalogue and be both approved and active. The change is propagated to the public
     * Adapter and its PID record, and shifts administrative access to the new Organisation's admins.
     *
     * @param id         the Adapter id
     * @param newOwnerId the id of the Organisation to move the Adapter to
     * @param comment    optional free-text comment stored on the update logging-info entry
     * @param auth       the authentication of the caller
     * @return the updated Adapter bundle
     */
    AdapterBundle changeResourceOwner(String id, String newOwnerId, String comment, Authentication auth);

    /**
     * Stores the result of an SQA assessment on the Adapter. Only the {@code sqa} field is written, onto the
     * stored Adapter, so edits made while the assessment ran are kept. This is not a user update: it adds no
     * logging-info entry and does not start a new assessment.
     *
     * @param id                 the Adapter id
     * @param catalogueId        the Adapter's catalogue id
     * @param assessedRepository the repository that was assessed
     * @param sqaUrl             the URL of the assessment report
     * @param sqaBadge           the badge vocabulary id, or {@code null} if the assessment earned no badge
     *                           (any previous badge is then removed)
     * @return the updated bundle, or {@code null} if the Adapter's repository is no longer the assessed one
     */
    AdapterBundle updateSqa(String id, String catalogueId, String assessedRepository, String sqaUrl, String sqaBadge);
}
