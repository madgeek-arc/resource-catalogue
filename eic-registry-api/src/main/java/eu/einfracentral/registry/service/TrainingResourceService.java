package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.LoggingInfo;
import eu.einfracentral.domain.TrainingResource;
import eu.einfracentral.domain.TrainingResourceBundle;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.SearchService;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TrainingResourceService<T> extends ResourceService<T, Authentication> {

    /**
     * Add a new TrainingResource on the EOSC Catalogue
     *
     * @param resource Training Resource
     * @param auth     Authentication
     * @return {@link T}
     */
    T addResource(T resource, Authentication auth);

    /**
     * Add a new TrainingResource on an external Catalogue, providing the Catalogue's ID
     *
     * @param resource    Training Resource
     * @param catalogueId Catalogue ID
     * @param auth        Authentication
     * @return {@link T}
     */
    T addResource(T resource, String catalogueId, Authentication auth);

    /**
     * Update a Training Resource of the EOSC Catalogue.
     *
     * @param resource Training Resource
     * @param comment  Comment
     * @param auth     Authentication
     * @return {@link T}
     * @throws ResourceNotFoundException The Resource was not found
     */
    T updateResource(T resource, String comment, Authentication auth) throws ResourceNotFoundException;

    /**
     * Update a Training Resource of an external Catalogue, providing its Catalogue ID
     *
     * @param resource    Training Resource
     * @param catalogueId Catalogue ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link T}
     * @throws ResourceNotFoundException The Resource was not found
     */
    T updateResource(T resource, String catalogueId, String comment, Authentication auth)
            throws ResourceNotFoundException;

    /**
     * Get a Training Resource of a specific Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param resourceId  Training Resource ID
     * @param auth        Authentication
     * @return {@link T}
     */
    T getCatalogueResource(String catalogueId, String resourceId, Authentication auth);

    /**
     * Returns the Training Resource with the specified ID
     *
     * @param id          Training Resource ID
     * @param catalogueId Catalogue ID
     * @return {@link T}
     */
    T get(String id, String catalogueId);

    /**
     * Get TrainingResource Bundles by a specific field.
     *
     * @param field Field of Training Resource
     * @param auth  Authentication
     * @return {@link Map}&lt;{@link String},{@link List}&lt;{@link T}&gt;&gt;
     * @throws NoSuchFieldException The field does not exist
     */
    Map<String, List<T>> getBy(String field, Authentication auth) throws NoSuchFieldException;

    /**
     * Get TrainingResources with the specified ids.
     *
     * @param authentication Authentication
     * @param ids            Training Resource IDs
     * @return {@link List}&lt;{@link TrainingResource}&gt;
     */
    List<TrainingResource> getByIds(Authentication authentication, String... ids);


    /**
     * Check if the TrainingResource exists.
     *
     * @param ids Training Resource IDs
     * @return True/False
     */
    boolean exists(SearchService.KeyValue... ids);

    /**
     * Validates the given TrainingResource Bundle.
     *
     * @param trainingResourceBundle TrainingResource Bundle
     * @return True/False
     */
    boolean validateTrainingResource(TrainingResourceBundle trainingResourceBundle);

    /**
     * Get Training Resource.
     *
     * @param id          TrainingResource ID
     * @param catalogueId Catalogue ID
     * @return {@link Resource}
     */
    Resource getResource(String id, String catalogueId);

    /**
     * Sets a TrainingResource as active/inactive.
     *
     * @param resourceId TrainingResource ID
     * @param active     True/False
     * @param auth       Authentication
     * @return {@link T}
     */
    T publish(String resourceId, Boolean active, Authentication auth);

    /**
     * Return children vocabularies from parent vocabularies
     *
     * @param type   Vocabulary's type
     * @param parent Vocabulary's parent
     * @param rec
     * @return {@link List}&lt;{@link String}&gt;
     */
    List<String> getChildrenFromParent(String type, String parent, List<Map<String, Object>> rec);

    /**
     * Gets a Browsing of all TrainingResources for admins
     *
     * @param filter FacetFilter
     * @param auth   Authentication
     * @return {@link Browsing}&lt;{@link T}&gt;
     */
    Browsing<T> getAllForAdmin(FacetFilter filter, Authentication auth);

    /**
     * Audit a Training Resource
     *
     * @param resourceId  TrainingResource ID
     * @param catalogueId Catalogue ID
     * @param comment     Comment
     * @param actionType  Audit's action type
     * @param auth        Authentication
     * @return {@link T}
     */
    T auditResource(String resourceId, String catalogueId, String comment, LoggingInfo.ActionType actionType,
                    Authentication auth);

    /**
     * Get a paging of random Training Resources
     *
     * @param ff               FacetFilter
     * @param auditingInterval Auditing Interval (in months)
     * @param auth             Authentication
     * @return {@link Paging}&lt;{@link T}&gt;
     */
    Paging<T> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth);

    /**
     * Get a list of TrainingResource Bundles of a specific Provider of the EOSC Catalogue
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getResourceBundles(String providerId, Authentication auth);

    /**
     * Get a paging of TrainingResource Bundles of a specific Provider of an external Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param providerId  Provider ID
     * @param auth        Authentication
     * @return {@link Paging}&lt;{@link T}&gt;
     */
    Paging<T> getResourceBundles(String catalogueId, String providerId, Authentication auth);

    /**
     * Get a list of TrainingResources of a specific Provider of the EOSC Catalogue
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link List}&lt;{@link TrainingResource}&gt;
     */
    List<? extends TrainingResource> getResources(String providerId, Authentication auth);

    /**
     * Get an EOSC Provider's TrainingResource Template, if exists, else return null
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link TrainingResourceBundle}
     */
    TrainingResourceBundle getResourceTemplate(String providerId, Authentication auth);

    /**
     * Get all inactive TrainingResources of a specific Provider, providing its ID
     *
     * @param providerId Provider ID
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getInactiveResources(String providerId);

    /**
     * Send email notifications to all Providers with outdated TrainingResources
     *
     * @param resourceId Training Resource ID
     * @param auth       Authentication
     */
    void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth);

    /**
     * Get the history of the specific TrainingResource of the specific Catalogue ID
     *
     * @param id          TrainingResource ID
     * @param catalogueId Catalogue ID
     * @return {@link Paging}&lt;{@link LoggingInfo}&gt;
     */
    Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId);

    /**
     * Verify the TrainingResource providing its ID
     *
     * @param id     TrainingResource ID
     * @param status TrainingResource's status (approved/rejected)
     * @param active True/False
     * @param auth   Authentication
     * @return {@link T}
     */
    T verifyResource(String id, String status, Boolean active, Authentication auth);

    /**
     * Change the Provider of the specific TrainingResource
     *
     * @param resourceId  TrainingResource ID
     * @param newProvider New Provider ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link T}
     */
    T changeProvider(String resourceId, String newProvider, String comment, Authentication auth);

    /**
     * Get a paging with all TrainingResource Bundles belonging to a specific audit state
     *
     * @param ff         FacetFilter
     * @param auditState Audit State
     * @return {@link Paging}&lt;{@link Bundle}&lt;?&gt;&gt;
     */
    Paging<Bundle<?>> getAllForAdminWithAuditStates(FacetFilter ff, Set<String> auditState);

    /**
     * Get a specific TrainingResource of the EOSC Catalogue, given its ID, or return null
     *
     * @param id TrainingResource ID
     * @return {@link TrainingResourceBundle}
     */
    TrainingResourceBundle getOrElseReturnNull(String id);

    /**
     * Get a specific TrainingResource of an external Catalogue, given its ID, or return null
     *
     * @param id          Training Resource ID
     * @param catalogueId Catalogue ID
     * @return {@link TrainingResourceBundle}
     */
    TrainingResourceBundle getOrElseReturnNull(String id, String catalogueId);

    /**
     * Create a Public TrainingResource
     *
     * @param resource TrainingResource
     * @param auth     Authentication
     * @return {@link T}
     */
    T createPublicResource(T resource, Authentication auth);

    /**
     * Suspend the TrainingResource given its ID
     *
     * @param trainingResourceId TrainingResource ID
     * @param catalogueId        Catalogue ID
     * @param suspend            True/False
     * @param auth               Authentication
     * @return {@link TrainingResourceBundle}
     */
    TrainingResourceBundle suspend(String trainingResourceId, String catalogueId, boolean suspend, Authentication auth);

    /**
     * Publish TrainingResource's related resources
     *
     * @param id          TrainingResource ID
     * @param catalogueId Catalogue ID
     * @param active      True/False
     * @param auth        Authentication
     */
    void publishTrainingResourceRelatedResources(String id, String catalogueId, Boolean active,
                                                 Authentication auth);
}
