package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.InteroperabilityRecordBundle;
import eu.einfracentral.domain.LoggingInfo;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;

import java.util.Set;

public interface InteroperabilityRecordService<T> extends ResourceService<T, Authentication> {

    /**
     *
     * @param interoperabilityRecordBundle
     * @param catalogueId
     * @param auth
     * @return
     */
    InteroperabilityRecordBundle add(InteroperabilityRecordBundle interoperabilityRecordBundle, String catalogueId,
                                     Authentication auth);

    /**
     *
     * @param interoperabilityRecordBundle
     * @param catalogueId
     * @param auth
     * @return
     */
    InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, String catalogueId,
                                        Authentication auth);

    /**
     *
     * @param id
     * @param catalogueId
     * @return
     */
    InteroperabilityRecordBundle get(String id, String catalogueId);

    /**
     *
     * @param id
     * @param catalogueId
     * @return
     */
    InteroperabilityRecordBundle getOrElseReturnNull(String id, String catalogueId);

    /**
     *
     * @param id
     * @param status
     * @param active
     * @param auth
     * @return
     */
    InteroperabilityRecordBundle verifyResource(String id, String status, Boolean active, Authentication auth);

    /**
     *
     * @param id
     * @param active
     * @param auth
     * @return
     */
    InteroperabilityRecordBundle publish(String id, Boolean active, Authentication auth);

    /**
     *
     * @param interoperabilityRecordBundle
     * @return
     */
    boolean validateInteroperabilityRecord(InteroperabilityRecordBundle interoperabilityRecordBundle);

    /**
     *
     * @param id
     * @param catalogueId
     * @return
     */
    Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId);

    /**
     *
     * @param interoperabilityRecordBundle
     * @param auth
     * @return
     */
    InteroperabilityRecordBundle createPublicInteroperabilityRecord(
            InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth);

    /**
     *
     * @param catalogueId
     * @param interoperabilityRecordId
     * @param auth
     * @return
     */
    InteroperabilityRecordBundle getCatalogueInteroperabilityRecord(String catalogueId, String interoperabilityRecordId,
                                                                    Authentication auth);

    /**
     *
     * @param catalogueId
     * @param providerId
     * @param auth
     * @return
     */
    Paging<InteroperabilityRecordBundle> getInteroperabilityRecordBundles(String catalogueId, String providerId,
                                                                          Authentication auth);

    /**
     *
     * @param allRequestParams
     * @param catalogueId
     * @param providerId
     * @return
     */
    FacetFilter createFacetFilterForFetchingInteroperabilityRecords(MultiValueMap<String, Object> allRequestParams,
                                                                    String catalogueId, String providerId);

    /**
     *
     * @param filter
     * @param auth
     */
    void updateFacetFilterConsideringTheAuthorization(FacetFilter filter, Authentication auth);

    /**
     * @param resourceId
     * @param catalogueId
     * @param comment
     * @param actionType
     * @param auth
     * @return
     */
    T auditResource(String resourceId, String catalogueId, String comment, LoggingInfo.ActionType actionType,
                    Authentication auth);

    /**
     *
     * @param interoperabilityRecordId
     * @param catalogueId
     * @param suspend
     * @param auth
     * @return
     */
    InteroperabilityRecordBundle suspend(String interoperabilityRecordId, String catalogueId, boolean suspend,
                                         Authentication auth);

    /**
     *
     * @param ff
     * @param auditState
     * @return
     */
    Paging<Bundle<?>> getAllForAdminWithAuditStates(FacetFilter ff, Set<String> auditState);

    /**
     *
     * @param catalogueId
     * @param providerId
     * @param auth
     * @return
     */
    Paging<InteroperabilityRecordBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth);
}
