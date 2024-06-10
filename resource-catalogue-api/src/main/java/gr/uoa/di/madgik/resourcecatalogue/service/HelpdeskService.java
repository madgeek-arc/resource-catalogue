package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.HelpdeskBundle;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface HelpdeskService extends ResourceService<HelpdeskBundle> {

    HelpdeskBundle add(HelpdeskBundle helpdesk, String resourceType, Authentication auth);

    /**
     * Retrieve {@link HelpdeskBundle} for a catalogue specific resource.
     *
     * @param serviceId   String Service ID
     * @param catalogueId String Catalogue ID
     * @return {@link HelpdeskBundle}
     */
    HelpdeskBundle get(String serviceId, String catalogueId);

    /**
     * Validates the given Helpdesk
     *
     * @param helpdeskBundle HelpdeskBundle
     * @param resourceType   String Resource Type
     * @return {@link HelpdeskBundle}
     */
    HelpdeskBundle validate(HelpdeskBundle helpdeskBundle, String resourceType);

    /**
     * Creates a Public version of the specific Helpdesk
     *
     * @param helpdeskBundle HelpdeskBundle
     * @param auth           Authentication
     * @return {@link HelpdeskBundle}
     */
    HelpdeskBundle createPublicResource(HelpdeskBundle helpdeskBundle, Authentication auth);

    /**
     * Updates a Helpdesk Bundle
     *
     * @param helpdeskBundle HelpdeskBundle
     * @param auth           Authentication
     */
    void updateBundle(HelpdeskBundle helpdeskBundle, Authentication auth);

    /**
     * Add a list of HelpdeskBundles on the Resource Catalogue
     *
     * @param helpdeskList List of HelpdeskBundles
     * @param auth         Authentication
     */
    void addBulk(List<HelpdeskBundle> helpdeskList, Authentication auth);
}
