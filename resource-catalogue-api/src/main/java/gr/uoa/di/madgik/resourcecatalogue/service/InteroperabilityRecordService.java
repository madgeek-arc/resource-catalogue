package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import org.springframework.security.core.Authentication;

public interface InteroperabilityRecordService extends ResourceService<InteroperabilityRecordBundle>, BundleOperations<InteroperabilityRecordBundle> {

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
     * @return {@link InteroperabilityRecordBundle}
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
}
