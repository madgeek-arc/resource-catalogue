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
package gr.uoa.di.madgik.resourcecatalogue.integration;

import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.controllers.CatalogueInfoController;
import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.OnboardingPolicy;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.OnboardingPolicyService;
import gr.uoa.di.madgik.resourcecatalogue.service.AdapterService;
import gr.uoa.di.madgik.resourcecatalogue.service.CatalogueService;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createAdapterBundle;
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createCatalogueBundle;
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createInteroperabilityRecordBundle;
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createOrganisationBundle;
import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createServiceBundle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

/**
 * Runs the real onboarding workflow of an Organisation-owned Service under each onboarding policy.
 */
class OnboardingPolicyIntegrationTest extends BaseIntegrationTest {

    private static final String NO_TEMPLATE = "no template status";

    @Autowired
    private OrganisationService organisationService;
    @Autowired
    private ServiceService serviceService;
    @Autowired
    private CatalogueInfoController catalogueInfoController;
    @Autowired
    private CatalogueService catalogueService;
    @Autowired
    private AdapterService adapterService;
    @Autowired
    private InteroperabilityRecordService guidelineService;
    @Autowired
    private SecurityService securityService;
    @MockitoSpyBean
    private OnboardingPolicyService policyService;

    private Authentication admin() {
        return securityService.getAdminAccess();
    }

    private void servicePolicy(OnboardingPolicy policy) {
        doReturn(policy).when(policyService).getPolicy("service");
    }

    private OrganisationBundle approvedOrganisation() {
        OrganisationBundle organisation = organisationService.add(createOrganisationBundle(), admin());
        return organisationService.verify(organisation.getId(), "approved", true, admin());
    }

    private ServiceBundle newService(OrganisationBundle organisation) {
        ServiceBundle bundle = createServiceBundle();
        bundle.getService().put("resourceOwner", organisation.getId());
        return serviceService.add(bundle, admin());
    }

    private String templateStatus(OrganisationBundle organisation) {
        return organisationService.get(organisation.getId()).getTemplateStatus();
    }

    @Test
    void noneApprovesImmediatelyAndNeverTouchesTemplateStatus() throws InterruptedException {
        servicePolicy(OnboardingPolicy.REVIEW_NONE);
        OrganisationBundle organisation = approvedOrganisation();

        ServiceBundle first = newService(organisation);
        ServiceBundle second = newService(organisation);
        Thread.sleep(1500); // let the async aspects run

        assertEquals("approved", serviceService.get(first.getId()).getStatus());
        assertEquals("approved", serviceService.get(second.getId()).getStatus());
        assertEquals(NO_TEMPLATE, templateStatus(organisation));
    }

    @Test
    void allKeepsEveryResourcePendingAndNeverTouchesTemplateStatus() throws InterruptedException {
        servicePolicy(OnboardingPolicy.REVIEW_ALL);
        OrganisationBundle organisation = approvedOrganisation();

        ServiceBundle first = newService(organisation);
        ServiceBundle second = newService(organisation);
        Thread.sleep(1500); // let the async aspects run

        assertEquals("pending", serviceService.get(first.getId()).getStatus());
        assertEquals("pending", serviceService.get(second.getId()).getStatus());
        assertEquals(NO_TEMPLATE, templateStatus(organisation));

        serviceService.verify(first.getId(), "approved", true, admin());
        assertEquals("approved", serviceService.get(first.getId()).getStatus());
        assertEquals(NO_TEMPLATE, templateStatus(organisation));
    }

    @Test
    void templateFirstGatesLaterResourcesOnTheTemplate() throws InterruptedException {
        servicePolicy(OnboardingPolicy.REVIEW_FIRST);
        OrganisationBundle organisation = approvedOrganisation();

        ServiceBundle template = newService(organisation);
        Thread.sleep(1500); // let the async aspects run
        assertEquals("pending", serviceService.get(template.getId()).getStatus());
        assertEquals("pending template", templateStatus(organisation));

        ResourceException blocked = assertThrows(ResourceException.class, () -> newService(organisation));
        assertEquals(HttpStatus.FORBIDDEN, blocked.getStatus());
        assertTrue(blocked.getMessage().contains("template under review"), blocked.getMessage());

        serviceService.verify(template.getId(), "approved", true, admin());
        assertEquals("approved template", templateStatus(organisation));

        ServiceBundle later = newService(organisation);
        assertEquals("approved", serviceService.get(later.getId()).getStatus());
    }

    private AdapterBundle newAdapter(OrganisationBundle organisation, ServiceBundle linkedService) {
        AdapterBundle bundle = createAdapterBundle();
        bundle.setCatalogueId(null);
        bundle.getAdapter().put("resourceOwner", organisation.getId());
        bundle.getAdapter().remove("catalogueId");
        bundle.getAdapter().put("linkedResource", java.util.Map.of("resource_type", "service",
                "id", linkedService.getId()));
        return adapterService.add(bundle, admin());
    }

    private InteroperabilityRecordBundle newGuideline(OrganisationBundle organisation) {
        InteroperabilityRecordBundle bundle = createInteroperabilityRecordBundle();
        bundle.setCatalogueId(null);
        bundle.getInteroperabilityRecord().put("resourceOwner", organisation.getId());
        bundle.getInteroperabilityRecord().remove("catalogueId");
        return guidelineService.add(bundle, admin());
    }

    @Test
    void adapterPolicies() throws InterruptedException {
        servicePolicy(OnboardingPolicy.REVIEW_NONE);
        OrganisationBundle organisation = approvedOrganisation();
        ServiceBundle linked = newService(organisation);

        doReturn(OnboardingPolicy.REVIEW_NONE).when(policyService).getPolicy("adapter");
        assertEquals("approved", adapterService.get(newAdapter(organisation, linked).getId()).getStatus());

        doReturn(OnboardingPolicy.REVIEW_ALL).when(policyService).getPolicy("adapter");
        assertEquals("pending", adapterService.get(newAdapter(organisation, linked).getId()).getStatus());
        assertEquals(NO_TEMPLATE, templateStatus(organisation));

        doReturn(OnboardingPolicy.REVIEW_FIRST).when(policyService).getPolicy("adapter");
        AdapterBundle template = newAdapter(organisation, linked);
        Thread.sleep(1500);
        assertEquals("pending", adapterService.get(template.getId()).getStatus());
        assertEquals("pending template", templateStatus(organisation));
        assertThrows(ResourceException.class, () -> newAdapter(organisation, linked));

        adapterService.verify(template.getId(), "approved", true, admin());
        assertEquals("approved template", templateStatus(organisation));
        assertEquals("approved", adapterService.get(newAdapter(organisation, linked).getId()).getStatus());
    }

    @Test
    void guidelinePolicies() throws InterruptedException {
        OrganisationBundle organisation = approvedOrganisation();

        doReturn(OnboardingPolicy.REVIEW_NONE).when(policyService).getPolicy("interoperability_record");
        assertEquals("approved", guidelineService.get(newGuideline(organisation).getId()).getStatus());

        doReturn(OnboardingPolicy.REVIEW_ALL).when(policyService).getPolicy("interoperability_record");
        assertEquals("pending", guidelineService.get(newGuideline(organisation).getId()).getStatus());
        assertEquals(NO_TEMPLATE, templateStatus(organisation));

        doReturn(OnboardingPolicy.REVIEW_FIRST).when(policyService).getPolicy("interoperability_record");
        InteroperabilityRecordBundle template = newGuideline(organisation);
        Thread.sleep(1500);
        assertEquals("pending", guidelineService.get(template.getId()).getStatus());
        assertEquals("pending template", templateStatus(organisation));
        assertThrows(ResourceException.class, () -> newGuideline(organisation));

        guidelineService.verify(template.getId(), "approved", true, admin());
        assertEquals("approved template", templateStatus(organisation));
        assertEquals("approved", guidelineService.get(newGuideline(organisation).getId()).getStatus());
    }

    private CatalogueBundle approvedCatalogue() {
        CatalogueBundle bundle = createCatalogueBundle();
        bundle.getCatalogue().put("resourceOwner", approvedOrganisation().getId());
        CatalogueBundle catalogue = catalogueService.add(bundle, admin());
        return catalogueService.verify(catalogue.getId(), "approved", true, admin());
    }

    private OrganisationBundle catalogueOrganisation(CatalogueBundle catalogue) {
        OrganisationBundle bundle = createOrganisationBundle();
        bundle.setCatalogueId(catalogue.getId());
        OrganisationBundle organisation = organisationService.add(bundle, admin());
        return organisationService.verify(organisation.getId(), "approved", true, admin());
    }

    private ServiceBundle newCatalogueService(CatalogueBundle catalogue, OrganisationBundle organisation) {
        ServiceBundle bundle = createServiceBundle();
        bundle.setCatalogueId(catalogue.getId());
        bundle.getService().put("resourceOwner", organisation.getId());
        return serviceService.add(bundle, admin());
    }

    @Test
    void catalogueOwnedResourcesAreApprovedWithTheCatalogueUnlessPolicyIsAll() throws InterruptedException {
        CatalogueBundle catalogue = approvedCatalogue();
        OrganisationBundle organisation = catalogueOrganisation(catalogue);

        servicePolicy(OnboardingPolicy.REVIEW_NONE);
        assertEquals("approved", serviceService.get(newCatalogueService(catalogue, organisation).getId(),
                catalogue.getId()).getStatus());

        // there is no template to wait for: the catalogue's approval is what counts
        servicePolicy(OnboardingPolicy.REVIEW_FIRST);
        assertEquals("approved", serviceService.get(newCatalogueService(catalogue, organisation).getId(),
                catalogue.getId()).getStatus());

        servicePolicy(OnboardingPolicy.REVIEW_ALL);
        assertEquals("pending", serviceService.get(newCatalogueService(catalogue, organisation).getId(),
                catalogue.getId()).getStatus());

        Thread.sleep(1500); // let the async aspects run
        // an Organisation of a catalogue is born with an approved template, and no policy changes that
        assertEquals("approved template", organisationService.get(organisation.getId(), catalogue.getId())
                .getTemplateStatus());
    }

    @Test
    void catalogueOwnedResourcesAreRejectedWhileTheCatalogueIsNotApproved() {
        CatalogueBundle catalogue = approvedCatalogue();
        OrganisationBundle organisation = catalogueOrganisation(catalogue);
        catalogueService.verify(catalogue.getId(), "rejected", false, admin());

        for (OnboardingPolicy policy : OnboardingPolicy.values()) {
            servicePolicy(policy);
            ResourceException blocked = assertThrows(ResourceException.class,
                    () -> newCatalogueService(catalogue, organisation));
            assertEquals(HttpStatus.FORBIDDEN, blocked.getStatus());
        }
    }

    @Test
    void switchingAwayFromReviewFirstDoesNotLeaveTheTemplatePendingForever() throws InterruptedException {
        servicePolicy(OnboardingPolicy.REVIEW_FIRST);
        OrganisationBundle organisation = approvedOrganisation();
        ServiceBundle template = newService(organisation);
        Thread.sleep(1500); // let the async aspects run
        assertEquals("pending template", templateStatus(organisation));

        // the node moves services to review-all while the template is still pending
        servicePolicy(OnboardingPolicy.REVIEW_ALL);
        serviceService.verify(template.getId(), "approved", true, admin());

        assertEquals("approved template", templateStatus(organisation));
    }

    @Test
    void configExposesTheBundledPolicies() {
        var policies = catalogueInfoController.get().getBody().onboardingPolicy();

        assertEquals("review-first", policies.get("datasource"));
        assertEquals("review-none", policies.get("adapter"));
        assertEquals("review-all", policies.get("interoperability_record"));
        assertEquals(6, policies.size());
    }
}
