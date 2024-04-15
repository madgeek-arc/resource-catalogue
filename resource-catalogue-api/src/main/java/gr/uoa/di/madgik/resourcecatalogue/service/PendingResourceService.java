package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface PendingResourceService<T extends Bundle> extends ResourceService<T, Authentication> {

    /**
     * Transforms the resource to pending.
     *
     * @param t    resource
     * @param auth Authentication
     * @return {@link T}
     */
    T transformToPending(T t, Authentication auth);

    /**
     * Transforms the resource with the specified id to pending.
     *
     * @param id   resource ID
     * @param auth Authentication
     * @return {@link T}
     */
    T transformToPending(String id, Authentication auth);

    /**
     * Transforms the resource to active.
     *
     * @param t    resource
     * @param auth Authentication
     * @return {@link T}
     */
    T transformToActive(T t, Authentication auth);

    /**
     * Transforms the resource with the specified id to active.
     *
     * @param id   resource ID
     * @param auth Authentication
     * @return {@link T}
     */
    T transformToActive(String id, Authentication auth);

    /**
     * Get the id using the originalId of the resource.
     *
     * @param originalId Original resource ID
     * @return {@link String}
     */
    default String getId(String originalId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("originalId", originalId);
        Browsing<T> resources = this.getAll(ff, null);
        if (resources.getTotal() > 1) {
            throw new ResourceException("Id '" + resources.getResults().get(0).getId()
                    + "' is not unique", HttpStatus.CONFLICT);
        } else if (resources.getTotal() == 0) {
            throw new ResourceException("Id not found", HttpStatus.NOT_FOUND);
        } else {
            return resources.getResults().get(0).getId();
        }
    }

    /**
     * Get a mapping of resource ids with original ids.
     *
     * @return {@link Map}&lt;{@link String},{@link String}&gt;
     */
    default Map<String, String> getIdOriginalIdMap() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        return this.getAll(ff, null)
                .getResults()
                .stream()
                .collect(Collectors.toMap(Bundle::getId, r -> r.getIdentifiers().getOriginalId()));
    }

    /**
     * Get a List of all Pending resources for a specific authenticated User
     *
     * @param authentication Authentication
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getMy(Authentication authentication);

    /**
     * Returns True if a User has accepted the Terms & Conditions, else returns False
     *
     * @param providerId     Provider ID
     * @param authentication Authentication
     * @return True/False
     */
    boolean hasAdminAcceptedTerms(String providerId, Authentication authentication);

    /**
     * Updates the list of Provider user emails that have accepted the Terms & Conditions
     *
     * @param providerId     Provider ID
     * @param authentication Authentication
     */
    void adminAcceptedTerms(String providerId, Authentication authentication);
}