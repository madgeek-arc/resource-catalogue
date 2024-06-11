package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.VocabularyCuration;
import org.springframework.security.core.Authentication;

public interface VocabularyCurationService extends ResourceService<VocabularyCuration> {

    /**
     * Add a new VocabularyCuration from UI
     *
     * @param resourceId     Resource ID
     * @param providerId     Provider ID
     * @param resourceType   Resource Type
     * @param entryValueName Suggested Vocabulary name
     * @param vocabulary     Vocabulary Type
     * @param parent         Vocabulary's parent (if exists)
     * @param auth           Authentication
     * @return {@link VocabularyCuration}
     */
    VocabularyCuration addFront(String resourceId, String providerId, String resourceType, String entryValueName,
                                String vocabulary, String parent, Authentication auth);

    /**
     * Add a new VocabularyCuration
     *
     * @param vocabularyCuration VocabularyCuration
     * @param resourceType       Resource Type
     * @param auth               Authentication
     * @return {@link VocabularyCuration}
     */
    VocabularyCuration add(VocabularyCuration vocabularyCuration, String resourceType, Authentication auth);

    /**
     * Get a Browsing of all VocabularyCurations requests
     *
     * @param ff   FacetFilter
     * @param auth Authentication
     * @return {@link Browsing}&lt;{@link VocabularyCuration}&gt;
     */
    Browsing<VocabularyCuration> getAllVocabularyCurationRequests(FacetFilter ff, Authentication auth);

    /**
     * Approve or Reject a VocabularyCuration request
     *
     * @param vocabularyCuration VocabularyCuration
     * @param approved           boolean value defining if a VocabularyCuration has been approved
     * @param rejectionReason    Reason for the Rejection (if rejected)
     * @param authentication     Authentication
     */
    void approveOrRejectVocabularyCuration(VocabularyCuration vocabularyCuration, boolean approved,
                                           String rejectionReason, Authentication authentication);
}
