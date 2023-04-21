package eu.einfracentral.registry.service;

import eu.einfracentral.domain.InteroperabilityRecordBundle;
import eu.einfracentral.domain.LoggingInfo;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;

public interface InteroperabilityRecordService<T> extends ResourceService<T, Authentication> {

    InteroperabilityRecordBundle add(InteroperabilityRecordBundle interoperabilityRecordBundle, String catalogueId, Authentication auth);
    InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, String catalogueId, Authentication auth);
    InteroperabilityRecordBundle get(String id, String catalogueId);
    InteroperabilityRecordBundle getOrElseReturnNull(String id, String catalogueId);
    InteroperabilityRecordBundle verifyResource(String id, String status, Boolean active, Authentication auth);
    InteroperabilityRecordBundle publish(String id, Boolean active, Authentication auth);
    boolean validateInteroperabilityRecord(InteroperabilityRecordBundle interoperabilityRecordBundle);
    Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId);
    InteroperabilityRecordBundle createPublicInteroperabilityRecord(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth);
    InteroperabilityRecordBundle getCatalogueInteroperabilityRecord(String catalogueId, String interoperabilityRecordId, Authentication auth);
    Paging<InteroperabilityRecordBundle> getInteroperabilityRecordBundles(String catalogueId, String providerId, Authentication auth);
    FacetFilter createFacetFilterForFetchingInteroperabilityRecords(MultiValueMap<String, Object> allRequestParams, String catalogueId, String providerId);
    void updateFacetFilterConsideringTheAuthorization(FacetFilter filter, Authentication auth);
}
