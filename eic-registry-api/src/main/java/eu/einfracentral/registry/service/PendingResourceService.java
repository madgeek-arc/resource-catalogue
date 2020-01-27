package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.exception.ResourceException;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

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
     * @param id
     */
    void transformToPending(String id);

    /**
     * Transforms the resource to active.
     *
     * @param id
     */
    void transformToActive(String id);

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
            throw new ResourceException("Id is not unique", HttpStatus.CONFLICT);
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
}
