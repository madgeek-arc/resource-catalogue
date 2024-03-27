package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;

import java.util.Set;

public interface InteroperabilityRecordService<T> extends ResourceService<T, Authentication> {

    /**
     * Add a new Interoperability Record on an existing Catalogue, providing the Catalogue's ID
     *
     * @param interoperabilityRecordBundle Interoperability Record
     * @param catalogueId                  Catalogue ID
     * @param auth                         Authentication
     * @return {@link InteroperabilityRecordBundle}
     */
    InteroperabilityRecordBundle add(InteroperabilityRecordBundle interoperabilityRecordBundle, String catalogueId,
                                     Authentication auth);

    /**
     * Update an Interoperability Record of an existing Catalogue, providing its Catalogue ID
     *
     * @param interoperabilityRecordBundle Interoperability Record
     * @param catalogueId                  Catalogue ID
     * @param auth                         Authentication
     * @return {@link InteroperabilityRecordBundle}
     */
    InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, String catalogueId,
                                        Authentication auth);

    /**
     * Returns the Interoperability Record with the specified ID
     *
     * @param id          Interoperability Record ID
     * @param catalogueId Catalogue ID
     * @return {@link InteroperabilityRecordBundle}
     */
    InteroperabilityRecordBundle get(String id, String catalogueId);

    /**
     * Get a specific Interoperability Record of an external Catalogue, given its ID, or return null
     *
     * @param id          Interoperability Record ID
     * @param catalogueId Catalogue ID
     * @return {@link InteroperabilityRecordBundle}
     */
    InteroperabilityRecordBundle getOrElseReturnNull(String id, String catalogueId);

    /**
     * Verify the Interoperability Record providing its ID
     *
     * @param id     Interoperability Record ID
     * @param status Interoperability Record's status (approved/rejected)
     * @param active True/False
     * @param auth   Authentication
     * @return {@link InteroperabilityRecordBundle}
     */
    InteroperabilityRecordBundle verifyResource(String id, String status, Boolean active, Authentication auth);

    /**
     * Sets an Interoperability Record as active/inactive.
     *
     * @param id     Interoperability Record ID
     * @param active True/False
     * @param auth   Authentication
     * @return {@link InteroperabilityRecordBundle}
     */
    InteroperabilityRecordBundle publish(String id, Boolean active, Authentication auth);

    /**
     * Validates the given Interoperability Record Bundle.
     *
     * @param interoperabilityRecordBundle Interoperability Record Bundle
     * @return True/False
     */
    boolean validateInteroperabilityRecord(InteroperabilityRecordBundle interoperabilityRecordBundle);

    /**
     * Get the history of the specific Interoperability Record of the specific Catalogue ID
     *
     * @param id          Interoperability Record ID
     * @param catalogueId Catalogue ID
     * @return {@link Paging}&lt;{@link LoggingInfo}&gt;
     */
    Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId);

    /**
     * Create a Public Interoperability Record
     *
     * @param interoperabilityRecordBundle Interoperability Record
     * @param auth                         Authentication
     * @return {@link InteroperabilityRecordBundle}
     */
    InteroperabilityRecordBundle createPublicInteroperabilityRecord(
            InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth);

    /**
     * Get an Interoperability Record of a specific Catalogue
     *
     * @param catalogueId              Catalogue ID
     * @param interoperabilityRecordId Interoperability Record ID
     * @param auth                     Authentication
     * @return {@link T}
     */
    InteroperabilityRecordBundle getCatalogueInteroperabilityRecord(String catalogueId, String interoperabilityRecordId,
                                                                    Authentication auth);

    /**
     * Get a paging of Interoperability Record Bundles of a specific Provider of an existing Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param providerId  Provider ID
     * @param auth        Authentication
     * @return {@link Paging}&lt;{@link InteroperabilityRecordBundle}&gt;
     */
    Paging<InteroperabilityRecordBundle> getInteroperabilityRecordBundles(String catalogueId, String providerId,
                                                                          Authentication auth);

    /**
     * Create a FacetFilter for fetching Interoperability Records
     *
     * @param allRequestParams {@link MultiValueMap} of all the Requested Parameters given
     * @param catalogueId      Catalogue ID
     * @param providerId       Provider ID
     * @return {@link FacetFilter}
     */
    FacetFilter createFacetFilterForFetchingInteroperabilityRecords(MultiValueMap<String, Object> allRequestParams,
                                                                    String catalogueId, String providerId);

    /**
     * Updates the FacetFilter considering the authorization rights
     *
     * @param filter FacetFilter
     * @param auth   Authorization
     */
    void updateFacetFilterConsideringTheAuthorization(FacetFilter filter, Authentication auth);

    /**
     * Audit an Interoperability Record
     *
     * @param resourceId  Interoperability Record ID
     * @param catalogueId Catalogue ID
     * @param comment     Comment
     * @param actionType  Audit's action type
     * @param auth        Authentication
     * @return {@link T}
     */
    T auditResource(String resourceId, String catalogueId, String comment, LoggingInfo.ActionType actionType,
                    Authentication auth);

    /**
     * Suspend the Interoperability Record given its ID
     *
     * @param interoperabilityRecordId Interoperability Record ID
     * @param catalogueId              Catalogue ID
     * @param suspend                  True/False
     * @param auth                     Authentication
     * @return {@link InteroperabilityRecordBundle}
     */
    InteroperabilityRecordBundle suspend(String interoperabilityRecordId, String catalogueId, boolean suspend,
                                         Authentication auth);

    /**
     * Get a paging with all Interoperability Record Bundles belonging to a specific audit state
     *
     * @param ff         FacetFilter
     * @param auditState Audit State
     * @return {@link Paging}&lt;{@link Bundle}&lt;?&gt;&gt;
     */
    Paging<Bundle<?>> getAllForAdminWithAuditStates(FacetFilter ff, Set<String> auditState);
}
