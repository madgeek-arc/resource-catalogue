package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import org.springframework.security.core.Authentication;

import java.util.List;

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
     * Return a List of Catalogue a User has access in
     *
     * @param authentication Authentication
     * @return {@link List}&lt;{@link CatalogueBundle}&gt;
     */
    List<CatalogueBundle> getMyCatalogues(Authentication authentication);
}
