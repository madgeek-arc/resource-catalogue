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
     * Creates id for {@link ServiceBundle}
     *
     * @param serviceBundle
     * @return
     */
    String createServiceId(ServiceBundle serviceBundle);

    /**
     * Creates id for {@link TrainingResourceBundle}
     *
     * @param trainingResourceBundle
     * @return
     */
    String createTrainingResourceId(TrainingResourceBundle trainingResourceBundle) throws NoSuchAlgorithmException;

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
    String sanitizeString(String providerName);
}
