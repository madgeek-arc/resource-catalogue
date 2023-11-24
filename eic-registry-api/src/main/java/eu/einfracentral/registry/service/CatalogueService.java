package eu.einfracentral.registry.service;

import eu.einfracentral.domain.CatalogueBundle;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface CatalogueService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    /**
     * Return a Catalogue given its ID
     *
     * @param id The ID of the Catalogue
     * @param auth Authentication
     * @return {@link T}
     */
    T get(String id, U auth);

    /**
     * Check the existence of a Catalogue
     *
     * @param id The ID of the Catalogue
     */
    void existsOrElseThrow(String id);

    /**
     * Add a new Catalogue
     *
     * @param catalogue Catalogue to be added
     * @param authentication Authentication
     * @return {@link T}
     */
    @Override
    T add(T catalogue, Authentication authentication);

    /**
     * Update an existing Catalogue
     *
     * @param catalogue Catalogue to be updated
     * @param comment Optional comment
     * @param auth Authentication
     * @return {@link CatalogueBundle}
     */
    CatalogueBundle update(CatalogueBundle catalogue, String comment, Authentication auth);

    /**
     * Return a List of Catalogue a User has access in
     *
     * @param authentication Authentication
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getMyCatalogues(U authentication);

    /**
     * Verify (approve/reject) a Catalogue during its Onboarding process
     *
     * @param id The ID of the Catalogue
     * @param status The Onboarding Status of the Catalogue
     * @param active boolean value marking a Catalogue as Active or Inactive
     * @param auth Authentication
     * @return {@link T}
     */
    T verifyCatalogue(String id, String status, Boolean active, U auth);

    /**
     * Activate/Deactivate a Catalogue
     *
     * @param catalogueId The ID of the Catalogue
     * @param active boolean value marking a Catalogue as Active or Inactive
     * @param auth Authentication
     * @return {@link CatalogueBundle}
     */
    CatalogueBundle publish(String catalogueId, Boolean active, Authentication auth);

    /**
     * Has an Authenticated User accepted the EOSC Portal Terms & Conditions
     *
     * @param catalogueId The ID of the Catalogue
     * @param authentication Authentication
     * @return <code>True</code> if Authenticated User has accepted Terms; <code>False</code> otherwise.
     */
    boolean hasAdminAcceptedTerms(String catalogueId, U authentication);

    /**
     * Update a Catalogue's list of Users that has accepted the Terms & Conditions
     *
     * @param catalogueId The ID of the Catalogue
     * @param authentication Authentication
     */
    void adminAcceptedTerms(String catalogueId, U authentication);

    /**
     * Create query for fetching Catalogues from DB
     *
     * @param ff FacetFilter
     * @param orderDirection Ascending/Descending
     * @param orderField The field in which the order takes place
     * @return {@link List}&lt;{@link Map}&lt;{@link String},{@link Object}&gt;
     */
    List<Map<String, Object>> createQueryForCatalogueFilters(FacetFilter ff, String orderDirection, String orderField);

    /**
     * Create correct quantity facets for fetching Catalogues
     *
     * @param catalogueBundle List of Catalogue Bundles
     * @param catalogueBundlePaging Paging of Catalogue Bundles
     * @param quantity - FacetFilter's quantity field
     * @param from FacetFilter's from field
     * @return {@link Paging}&lt;{@link CatalogueBundle}&gt;
     */
    Paging<CatalogueBundle> createCorrectQuantityFacets(List<CatalogueBundle> catalogueBundle, Paging<CatalogueBundle> catalogueBundlePaging, int quantity, int from);

    /**
     * Suspend the Catalogue
     *
     * @param catalogueId The ID of the Catalogue
     * @param suspend boolean value marking a Catalogue as Suspended or Unsuspended
     * @param auth Authentication
     * @return {@link CatalogueBundle}
     */
    CatalogueBundle suspend(String catalogueId, boolean suspend, Authentication auth);
}
