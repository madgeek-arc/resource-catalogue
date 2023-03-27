package eu.einfracentral.registry.service;

import eu.einfracentral.domain.InteroperabilityRecordBundle;
import eu.einfracentral.domain.LoggingInfo;
import eu.einfracentral.domain.TrainingResourceBundle;
import eu.openminted.registry.core.domain.Paging;
import org.springframework.security.core.Authentication;

public interface InteroperabilityRecordService<T> extends ResourceService<T, Authentication> {

    InteroperabilityRecordBundle getOrElseReturnNull(String id, String catalogueId);
    InteroperabilityRecordBundle verifyResource(String id, String status, Boolean active, Authentication auth);
    InteroperabilityRecordBundle publish(String id, Boolean active, Authentication auth);
    boolean validateInteroperabilityRecord(InteroperabilityRecordBundle interoperabilityRecordBundle);
    Paging<LoggingInfo> getLoggingInfoHistory(String id);
    T createPublicInteroperabilityRecord(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication auth);
}
