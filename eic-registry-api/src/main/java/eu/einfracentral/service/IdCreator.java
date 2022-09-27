package eu.einfracentral.service;

import eu.einfracentral.domain.Catalogue;
import eu.einfracentral.domain.DatasourceBundle;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ResourceBundle;

import java.security.NoSuchAlgorithmException;

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
    String createServiceId(ResourceBundle<?> resource);

    /**
     * Creates id for {@link ResourceBundle}
     *
     * @param datasourceBundle
     * @return
     */
    String createDatasourceId(ResourceBundle<?> resource) throws NoSuchAlgorithmException;

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
