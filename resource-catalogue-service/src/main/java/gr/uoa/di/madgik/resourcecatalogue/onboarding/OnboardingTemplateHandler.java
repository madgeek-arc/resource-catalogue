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
package gr.uoa.di.madgik.resourcecatalogue.onboarding;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.TemplateOnboardingService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Applies the onboarding policy of a resource type to the Organisation's {@code templateStatus}.
 * <p>
 * {@code templateStatus} is a single, shared per-Organisation field, written only for resource types whose
 * policy is {@link OnboardingPolicy#REVIEW_FIRST}. Under {@code review-all} and {@code review-none} it is never
 * started; it is only settled when a template left pending by a policy change is verified.
 */
@Component
public class OnboardingTemplateHandler {

    private final OnboardingPolicyService policyService;
    private final OrganisationService organisationService;
    private final VocabularyService vocabularyService;
    // looked up lazily: the managers depend on this handler
    private final ObjectProvider<List<TemplateOnboardingService>> templateServices;

    public OnboardingTemplateHandler(OnboardingPolicyService policyService,
                                     OrganisationService organisationService,
                                     VocabularyService vocabularyService,
                                     ObjectProvider<List<TemplateOnboardingService>> templateServices) {
        this.policyService = policyService;
        this.organisationService = organisationService;
        this.vocabularyService = vocabularyService;
        this.templateServices = templateServices;
    }

    public boolean isReviewFirst(String resourceType) {
        return policyService.isReviewFirst(resourceType);
    }

    /**
     * Mirrors a resource's new onboarding status to the Organisation's {@code templateStatus}
     * ({@code pending template}, {@code approved template}, {@code rejected template}). A rejection only counts
     * when the template is the one pending, so rejecting a later resource leaves an approved template alone.
     * <p>
     * For a type that is not {@code review-first} this only settles a template left {@code pending} by a policy
     * change, see {@link #settleOrphanedTemplate}.
     */
    public void syncTemplateStatus(String resourceType, Bundle resource, String status, Authentication auth) {
        if (!isReviewFirst(resourceType)) {
            settleOrphanedTemplate(resource, status, auth);
            return;
        }
        OrganisationBundle provider = organisationService.get(resourceOwner(resource), resource.getCatalogueId());
        switch (status) {
            case "pending":
                provider.setTemplateStatus("pending template");
                break;
            case "approved":
                provider.setTemplateStatus("approved template");
                break;
            case "rejected":
                // only the template can be rejected; rejecting a later resource must not reopen the gate
                if ("pending template".equals(provider.getTemplateStatus())) {
                    provider.setTemplateStatus("rejected template");
                }
                break;
            default:
                break;
        }
        organisationService.update(provider, "system update", auth);
    }

    /**
     * Called when a resource is updated. A {@code rejected} resource goes back to {@code pending}:
     * <ul>
     *   <li>{@code review-first}: only if the Organisation's template was rejected, which also moves the
     *   Organisation to {@code pending template}.</li>
     *   <li>{@code review-all}: always, {@code templateStatus} is not involved.</li>
     *   <li>{@code review-none}: never, resources are not subject to approval.</li>
     * </ul>
     */
    public void resetRejectedOnboarding(String resourceType, Bundle resource, Authentication auth) {
        OnboardingPolicy policy = policyService.getPolicy(resourceType);
        if (policy == OnboardingPolicy.REVIEW_NONE
                || !vocabularyService.get("rejected").getId().equals(resource.getStatus())) {
            return;
        }
        if (policy == OnboardingPolicy.REVIEW_ALL) {
            resource.setStatus(vocabularyService.get("pending").getId());
            resource.setActive(false);
            return;
        }
        OrganisationBundle provider = organisationService.get(resourceOwner(resource), resource.getCatalogueId());
        if (provider.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
            resource.setStatus(vocabularyService.get("pending").getId());
            resource.setActive(false);
            provider.setTemplateStatus(vocabularyService.get("pending template").getId());
            organisationService.update(provider, "system update", auth);
        }
    }

    /**
     * A type that used to be {@code review-first} can leave its Organisation in {@code pending template}: its
     * pending template is verified later, but no longer by a {@code review-first} type, which would block every
     * type that still is {@code review-first} for good. The verification then settles the template, provided no
     * pending template of a current {@code review-first} type is left, which this verification could not be.
     */
    private void settleOrphanedTemplate(Bundle resource, String status, Authentication auth) {
        if (!"approved".equals(status) && !"rejected".equals(status)) {
            return;
        }
        OrganisationBundle provider = organisationService.get(resourceOwner(resource), resource.getCatalogueId());
        if (!"pending template".equals(provider.getTemplateStatus()) || hasPendingTemplate(provider, auth)) {
            return;
        }
        provider.setTemplateStatus("approved".equals(status) ? "approved template" : "rejected template");
        organisationService.update(provider, "system update", auth);
    }

    private boolean hasPendingTemplate(OrganisationBundle provider, Authentication auth) {
        List<TemplateOnboardingService> services = templateServices.getIfAvailable(List::of);
        return services.stream().anyMatch(service -> service.getTemplate(provider.getId(), auth) != null);
    }

    private static String resourceOwner(Bundle resource) {
        return (String) resource.getPayload().get("resourceOwner");
    }
}
