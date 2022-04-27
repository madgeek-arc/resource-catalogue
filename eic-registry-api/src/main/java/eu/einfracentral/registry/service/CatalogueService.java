package eu.einfracentral.registry.service;

import eu.einfracentral.domain.CatalogueBundle;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.ServiceProviderDomain;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface CatalogueService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    //SECTION: CATALOGUE
    T get(String id, U auth);

    @Override
    T add(T catalogue, Authentication authentication);

    /**
     * @param catalogue
     * @param comment
     * @param auth
     * @return
     */
    CatalogueBundle update(CatalogueBundle catalogue, String comment, Authentication auth);

    List<T> getMyCatalogues(U authentication);

    List<T> getInactive();

    void validateScientificDomains(List<ServiceProviderDomain> scientificDomains);

    T verifyCatalogue(String id, String status, Boolean active, U auth);

    CatalogueBundle publish(String catalogueId, Boolean active, Authentication auth);

    boolean hasAdminAcceptedTerms(String catalogueId, U authentication);

    void adminAcceptedTerms(String catalogueId, U authentication);

    /**
     * @param ff
     * @return
     */
    List<Map<String, Object>> createQueryForCatalogueFilters(FacetFilter ff, String orderDirection, String orderField);

    /**
     * @param catalogueBundle
     * @param catalogueBundlePaging
     * @param quantity
     * @param from
     * @return
     */
    Paging<CatalogueBundle> createCorrectQuantityFacets(List<CatalogueBundle> catalogueBundle, Paging<CatalogueBundle> catalogueBundlePaging, int quantity, int from);

}
