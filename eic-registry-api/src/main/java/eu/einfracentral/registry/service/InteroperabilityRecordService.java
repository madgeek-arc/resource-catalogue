package eu.einfracentral.registry.service;

import eu.einfracentral.domain.InteroperabilityRecordBundle;
import org.springframework.security.core.Authentication;

public interface InteroperabilityRecordService<T> extends ResourceService<T, Authentication> {

    InteroperabilityRecordBundle getOrElseReturnNull(String id, String catalogueId);
}
