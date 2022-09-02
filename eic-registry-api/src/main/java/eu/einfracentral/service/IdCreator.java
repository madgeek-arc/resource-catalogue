package eu.einfracentral.service;

import eu.einfracentral.domain.Catalogue;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ResourceBundle;

public interface IdCreator {

    /**
     * Creates id for {@link Provider}
     *
     * @param provider
     * @return
     */
    String createProviderId(Provider provider);

    /**
     * Creates id for {@link ResourceBundle}
     *
     * @param resource
     * @return
     */
    String createResourceId(ResourceBundle<?> resource);

    /**
     * Creates id for {@link Catalogue}
     *
     * @param catalogue
     * @return
     */
    String createCatalogueId(Catalogue catalogue);

    /**
     * *
     * @param providerName
     * @return
     */
    String reformatId(String providerName);
}
