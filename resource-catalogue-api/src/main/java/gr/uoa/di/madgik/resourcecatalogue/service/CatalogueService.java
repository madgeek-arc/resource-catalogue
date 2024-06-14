package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface CatalogueService extends ResourceService<CatalogueBundle> {

    /**
     * Return a Catalogue given its ID
     *
     * @param id   Catalogue ID
     * @param auth Authentication
     * @return {@link CatalogueBundle}
     */
    CatalogueBundle get(String id, Authentication auth);

    /**
     * Check the existence of a Catalogue
     *
     * @param id Catalogue ID
     */
    void existsOrElseThrow(String id);

    /**
     * Add a new Catalogue
     *
     * @param catalogue      Catalogue to be added
     * @param authentication Authentication
     * @return {@link CatalogueBundle}
     */
    @Override
    CatalogueBundle add(CatalogueBundle catalogue, Authentication authentication);

    /**
     * Update an existing Catalogue
     *
     * @param catalogue Catalogue to be updated
     * @param comment   Optional comment
     * @param auth      Authentication
     * @return {@link CatalogueBundle}
     */
    CatalogueBundle update(CatalogueBundle catalogue, String comment, Authentication auth);

    /**
     * Return a List of Catalogue a User has access in
     *
     * @param authentication Authentication
     * @return {@link List}&lt;{@link CatalogueBundle}&gt;
     */
    List<CatalogueBundle> getMyCatalogues(Authentication authentication);

    /**
     * Verify (approve/reject) a Catalogue during its Onboarding process
     *
     * @param id     Catalogue ID
     * @param status The Onboarding Status of the Catalogue
     * @param active boolean value marking a Catalogue as Active or Inactive
     * @param auth   Authentication
     * @return {@link CatalogueBundle}
     */
    CatalogueBundle verifyCatalogue(String id, String status, Boolean active, Authentication auth);

    /**
     * Activate/Deactivate a Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param active      boolean value marking a Catalogue as Active or Inactive
     * @param auth        Authentication
     * @return {@link CatalogueBundle}
     */
    CatalogueBundle publish(String catalogueId, Boolean active, Authentication auth);

    /**
     * Has an Authenticated User accepted the EOSC Portal Terms & Conditions
     *
     * @param catalogueId    Catalogue ID
     * @param authentication Authentication
     * @return <code>True</code> if Authenticated User has accepted Terms; <code>False</code> otherwise.
     */
    boolean hasAdminAcceptedTerms(String catalogueId, Authentication authentication);

    /**
     * Update a Catalogue's list of Users that has accepted the Terms & Conditions
     *
     * @param catalogueId    Catalogue ID
     * @param authentication Authentication
     */
    void adminAcceptedTerms(String catalogueId, Authentication authentication);

    /**
     * Suspend the Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param suspend     boolean value marking a Catalogue as Suspended or Unsuspended
     * @param auth        Authentication
     * @return {@link CatalogueBundle}
     */
    CatalogueBundle suspend(String catalogueId, boolean suspend, Authentication auth);

    /**
     * Audit the Catalogue
     *
     * @param id         Catalogue ID
     * @param actionType Validate or Invalidate action
     * @param auth       Authentication
     * @return {@link CatalogueBundle}
     */
    CatalogueBundle auditCatalogue(String id, String comment, LoggingInfo.ActionType actionType, Authentication auth);
}
