package eu.einfracentral.registry.service;

import eu.einfracentral.domain.VocabularyCuration;
import org.springframework.security.core.Authentication;

public interface VocabularyCurationService <T, U extends Authentication> extends ResourceService<VocabularyCuration, Authentication>{

    /**
     * @param resourceId
     * @param providerId
     * @param resourceType
     * @param entryValueName
     * @param vocabulary
     * @param parent
     * @param auth
     */
    void addFront(String resourceId, String providerId, String resourceType, String entryValueName, String vocabulary, String parent, Authentication auth);

}
