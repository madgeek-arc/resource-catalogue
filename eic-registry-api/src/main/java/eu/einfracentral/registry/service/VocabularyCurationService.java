package eu.einfracentral.registry.service;

import eu.einfracentral.domain.VocabularyCuration;
import org.springframework.security.core.Authentication;

public interface VocabularyCurationService <T, U extends Authentication> extends ResourceService<VocabularyCuration, Authentication>{
}
