package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.DeployableService;
import gr.uoa.di.madgik.resourcecatalogue.domain.DeployableServiceBundle;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface DeployableServiceService extends ResourceCatalogueService<DeployableServiceBundle>,
        BundleOperations<DeployableServiceBundle> {

    /**
     * Add a new Deployable Service on an external Catalogue, providing the Catalogue's ID
     *
     * @param resource    Deployable Service
     * @param catalogueId Catalogue ID
     * @param auth        Authentication
     * @return {@link   DeployableServiceBundle}
     */
    DeployableServiceBundle add(DeployableServiceBundle resource, String catalogueId, Authentication auth);

    /**
     * Update a Deployable Service of the EOSC Catalogue.
     *
     * @param resource Deployable Service
     * @param comment  Comment
     * @param auth     Authentication
     * @return {@link   DeployableServiceBundle}
     * @throws ResourceNotFoundException The Resource was not found
     */
    DeployableServiceBundle update(DeployableServiceBundle resource, String comment, Authentication auth);

    /**
     * Update a Deployable Service of an external Catalogue, providing its Catalogue ID
     *
     * @param resource    Deployable Service
     * @param catalogueId Catalogue ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link   DeployableServiceBundle}
     * @throws ResourceNotFoundException The Resource was not found
     */
    DeployableServiceBundle update(DeployableServiceBundle resource, String catalogueId, String comment, Authentication auth);

    /**
     * Get a Deployable Service of a specific Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param resourceId  Deployable Service ID
     * @param auth        Authentication
     * @return {@link   DeployableServiceBundle}
     */
    DeployableServiceBundle getCatalogueResource(String catalogueId, String resourceId, Authentication auth);

    /**
     * Get Deployable Service Bundles by a specific field.
     *
     * @param field Field of Deployable Service
     * @param auth  Authentication
     * @return {@link Map}&lt;{@link String},{@link List}&lt;{@link   DeployableServiceBundle}&gt;&gt;
     * @throws NoSuchFieldException The field does not exist
     */
    Map<String, List<DeployableServiceBundle>> getBy(String field, Authentication auth) throws NoSuchFieldException;

    /**
     * Get Deployable Services with the specified ids.
     *
     * @param authentication Authentication
     * @param ids            Deployable Service IDs
     * @return {@link List}&lt;{@link DeployableService}&gt;
     */
    List<DeployableService> getByIds(Authentication authentication, String... ids);

    /**
     * Get a paging of random Deployable Services
     *
     * @param ff               FacetFilter
     * @param auditingInterval Auditing Interval (in months)
     * @param auth             Authentication
     * @return {@link Paging}&lt;{@link   DeployableServiceBundle}&gt;
     */
    Paging<DeployableServiceBundle> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth);

    /**
     * Get a list of Deployable Service Bundles of a specific Provider of the EOSC Catalogue
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link List}&lt;{@link   DeployableServiceBundle}&gt;
     */
    List<DeployableServiceBundle> getResourceBundles(String providerId, Authentication auth);

    /**
     * Get a paging of Deployable Service Bundles of a specific Provider of an external Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param providerId  Provider ID
     * @param auth        Authentication
     * @return {@link Paging}&lt;{@link   DeployableServiceBundle}&gt;
     */
    Paging<DeployableServiceBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth);

    /**
     * Get an EOSC Provider's Deployable Service Template, if exists, else return null
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link DeployableServiceBundle}
     */
    DeployableServiceBundle getResourceTemplate(String providerId, Authentication auth);

    /**
     * Get all inactive Deployable Services of a specific Provider, providing its ID
     *
     * @param providerId Provider ID
     * @return {@link List}&lt;{@link   DeployableServiceBundle}&gt;
     */
    List<DeployableServiceBundle> getInactiveResources(String providerId);

    /**
     * Send email notifications to all Providers with outdated Deployable Services
     *
     * @param resourceId Deployable Service ID
     * @param auth       Authentication
     */
    void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth);

    /**
     * Change the Provider of the specific Deployable Service
     *
     * @param resourceId  Deployable Service ID
     * @param newProvider New Provider ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link   DeployableServiceBundle}
     */
    DeployableServiceBundle changeProvider(String resourceId, String newProvider, String comment, Authentication auth);

    /**
     * Get a specific Deployable Service of the EOSC Catalogue, given its ID, or return null
     *
     * @param id Deployable Service ID
     * @return {@link DeployableServiceBundle}
     */
    DeployableServiceBundle getOrElseReturnNull(String id);

    /**
     * Create a Public Deployable Service
     *
     * @param resource Deployable Service
     * @param auth     Authentication
     * @return {@link   DeployableServiceBundle}
     */
    DeployableServiceBundle createPublicResource(DeployableServiceBundle resource, Authentication auth);
}
