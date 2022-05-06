package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.exception.ResourceException;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface PendingResourceService<T extends Bundle> extends ResourceService<T, Authentication> {

    /**
     * Updates the resource and sets a new id.
     *
     * @param t
     * @param auth
     * @return
     */
    @Override
    T update(T t, Authentication auth);

    /**
     * Transforms the resource to pending.
     *
     * @param t
     * @param auth
     */
    T transformToPending(T t, Authentication auth);

    /**
     * Transforms the resource with the specified id to pending.
     *
     * @param id
     * @param auth
     */
    T transformToPending(String id, Authentication auth);

    /**
     * Transforms the resource to active.
     *
     * @param t
     * @param auth
     */
    T transformToActive(T t, Authentication auth);

    /**
     * Transforms the resource with the specified id to active.
     *
     * @param id
     * @param auth
     */
    T transformToActive(String id, Authentication auth);

    /**
     * Create a Rich Object from a Pending Resource
     *
     * @param id
     * @param auth
     * @return
     */
    Object getPendingRich(String id, Authentication auth);

    /**
     * Get the id using the originalId of the resource.
     *
     * @param originalId
     * @return
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
     * @return
     */
    default Map<String, String> getIdOriginalIdMap() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        return this.getAll(ff, null)
                .getResults()
                .stream()
                .collect(Collectors.toMap(Bundle::getId, r -> r.getMetadata().getOriginalId()));
    }

    List<T> getMy(Authentication authentication);

    boolean hasAdminAcceptedTerms(String providerId, Authentication authentication);

    void adminAcceptedTerms(String providerId, Authentication authentication);

    /**
     * Get the service resource.
     *
     * @param serviceId
     * @param version
     * @return Resource
     */
    Resource getPendingResource(String serviceId, String version);

    /**
     * Get the provider resource.
     *
     * @param providerId
     * @return Resource
     */
    Resource getPendingResource(String providerId);

}
