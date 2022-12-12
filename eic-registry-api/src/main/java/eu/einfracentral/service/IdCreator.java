package eu.einfracentral.service;

import eu.einfracentral.domain.*;

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
     * @param resource
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
     * Creates id for {@link InteroperabilityRecord}
     *
     * @param interoperabilityRecord
     * @return
     */
    String createInteroperabilityRecordId(InteroperabilityRecord interoperabilityRecord) throws NoSuchAlgorithmException;

    /**
     * *
     * @param providerName
     * @return
     */
    String reformatId(String providerName);
}
