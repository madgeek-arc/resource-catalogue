package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import org.springframework.security.core.Authentication;

public interface TemplateOnboardingService {

    /**
     * Get an EOSC Provider's Service Template, if exists, else return null
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link Bundle}
     */
    Bundle getTemplate(String providerId, Authentication auth);
}
