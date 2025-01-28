package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import org.springframework.security.core.Authentication;

public interface CatalogueService extends ResourceService<CatalogueBundle>, BundleOperations<CatalogueBundle> {

    /**
     * Return a Catalogue given its ID
     *
     * @param id   Catalogue ID
     * @param auth Authentication
     * @return {@link CatalogueBundle}
     */
    CatalogueBundle get(String id, Authentication auth);

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
     * Return true if Provider User Admin has accepted registration terms
     *
     * @param providerId Provider's ID
     * @param auth       Authentication
     * @return True/False
     */
    boolean hasAdminAcceptedTerms(String providerId, Authentication auth);

    /**
     * Update the Provider's list of Users that have accepted the Provider's registration terms
     *
     * @param providerId Provider's ID
     * @param auth       Authentication
     */
    void adminAcceptedTerms(String providerId, Authentication auth);
}
