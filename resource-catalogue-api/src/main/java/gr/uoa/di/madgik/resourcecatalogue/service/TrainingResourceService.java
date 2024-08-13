package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResource;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface TrainingResourceService extends ResourceService<TrainingResourceBundle>, BundleOperations<TrainingResourceBundle> {

    /**
     * Add a new Training Resource on an external Catalogue, providing the Catalogue's ID
     *
     * @param resource    Training Resource
     * @param catalogueId Catalogue ID
     * @param auth        Authentication
     * @return {@link   TrainingResourceBundle}
     */
    TrainingResourceBundle add(TrainingResourceBundle resource, String catalogueId, Authentication auth);

    /**
     * Update a Training Resource of the EOSC Catalogue.
     *
     * @param resource Training Resource
     * @param comment  Comment
     * @param auth     Authentication
     * @return {@link   TrainingResourceBundle}
     * @throws ResourceNotFoundException The Resource was not found
     */
    TrainingResourceBundle update(TrainingResourceBundle resource, String comment, Authentication auth) throws ResourceNotFoundException;

    /**
     * Update a Training Resource of an external Catalogue, providing its Catalogue ID
     *
     * @param resource    Training Resource
     * @param catalogueId Catalogue ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link   TrainingResourceBundle}
     * @throws ResourceNotFoundException The Resource was not found
     */
    TrainingResourceBundle update(TrainingResourceBundle resource, String catalogueId, String comment, Authentication auth)
            throws ResourceNotFoundException;

    /**
     * Get a Training Resource of a specific Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param resourceId  Training Resource ID
     * @param auth        Authentication
     * @return {@link   TrainingResourceBundle}
     */
    TrainingResourceBundle getCatalogueResource(String catalogueId, String resourceId, Authentication auth);

    /**
     * Returns the Training Resource with the specified ID
     *
     * @param id          Training Resource ID
     * @param catalogueId Catalogue ID
     * @return {@link   TrainingResourceBundle}
     */
    TrainingResourceBundle get(String id, String catalogueId);

    /**
     * Get Training Resource Bundles by a specific field.
     *
     * @param field Field of Training Resource
     * @param auth  Authentication
     * @return {@link Map}&lt;{@link String},{@link List}&lt;{@link   TrainingResourceBundle}&gt;&gt;
     * @throws NoSuchFieldException The field does not exist
     */
    Map<String, List<TrainingResourceBundle>> getBy(String field, Authentication auth) throws NoSuchFieldException;

    /**
     * Get Training Resources with the specified ids.
     *
     * @param authentication Authentication
     * @param ids            Training Resource IDs
     * @return {@link List}&lt;{@link TrainingResource}&gt;
     */
    List<TrainingResource> getByIds(Authentication authentication, String... ids);

    /**
     * Validates the given Training Resource Bundle.
     *
     * @param trainingResourceBundle Training Resource Bundle
     * @return True/False
     */
    boolean validateTrainingResource(TrainingResourceBundle trainingResourceBundle);

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
     * Gets a Browsing of all Training Resources for admins
     *
     * @param filter FacetFilter
     * @param auth   Authentication
     * @return {@link Browsing}&lt;{@link   TrainingResourceBundle}&gt;
     */
    Browsing<TrainingResourceBundle> getAllForAdmin(FacetFilter filter, Authentication auth);

    /**
     * Get a paging of random Training Resources
     *
     * @param ff               FacetFilter
     * @param auditingInterval Auditing Interval (in months)
     * @param auth             Authentication
     * @return {@link Paging}&lt;{@link   TrainingResourceBundle}&gt;
     */
    Paging<TrainingResourceBundle> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth);

    /**
     * Get a list of Training Resource Bundles of a specific Provider of the EOSC Catalogue
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link List}&lt;{@link   TrainingResourceBundle}&gt;
     */
    List<TrainingResourceBundle> getResourceBundles(String providerId, Authentication auth);

    /**
     * Get a paging of Training Resource Bundles of a specific Provider of an external Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param providerId  Provider ID
     * @param auth        Authentication
     * @return {@link Paging}&lt;{@link   TrainingResourceBundle}&gt;
     */
    Paging<TrainingResourceBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth);

    /**
     * Get a list of Training Resources of a specific Provider of the EOSC Catalogue
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link List}&lt;{@link TrainingResource}&gt;
     */
    List<? extends TrainingResource> getResources(String providerId, Authentication auth);

    /**
     * Get an EOSC Provider's Training Resource Template, if exists, else return null
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link TrainingResourceBundle}
     */
    TrainingResourceBundle getResourceTemplate(String providerId, Authentication auth);

    /**
     * Get all inactive Training Resources of a specific Provider, providing its ID
     *
     * @param providerId Provider ID
     * @return {@link List}&lt;{@link   TrainingResourceBundle}&gt;
     */
    List<TrainingResourceBundle> getInactiveResources(String providerId);

    /**
     * Send email notifications to all Providers with outdated Training Resources
     *
     * @param resourceId Training Resource ID
     * @param auth       Authentication
     */
    void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth);

    /**
     * Get the history of the specific Training Resource of the specific Catalogue ID
     *
     * @param id          Training Resource ID
     * @param catalogueId Catalogue ID
     * @return {@link Paging}&lt;{@link LoggingInfo}&gt;
     */
    Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId);

    /**
     * Change the Provider of the specific Training Resource
     *
     * @param resourceId  Training Resource ID
     * @param newProvider New Provider ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link   TrainingResourceBundle}
     */
    TrainingResourceBundle changeProvider(String resourceId, String newProvider, String comment, Authentication auth);

    /**
     * Get a specific Training Resource of the EOSC Catalogue, given its ID, or return null
     *
     * @param id Training Resource ID
     * @return {@link TrainingResourceBundle}
     */
    TrainingResourceBundle getOrElseReturnNull(String id);

    /**
     * Get a specific Training Resource of an external Catalogue, given its ID, or return null
     *
     * @param id          Training Resource ID
     * @param catalogueId Catalogue ID
     * @return {@link TrainingResourceBundle}
     */
    TrainingResourceBundle getOrElseReturnNull(String id, String catalogueId);

    /**
     * Create a Public Training Resource
     *
     * @param resource Training Resource
     * @param auth     Authentication
     * @return {@link   TrainingResourceBundle}
     */
    TrainingResourceBundle createPublicResource(TrainingResourceBundle resource, Authentication auth);

    /**
     * Publish Training Resource's related resources
     *
     * @param id          Training Resource ID
     * @param catalogueId Catalogue ID
     * @param active      True/False
     * @param auth        Authentication
     */
    void publishTrainingResourceRelatedResources(String id, String catalogueId, Boolean active,
                                                 Authentication auth);
}
