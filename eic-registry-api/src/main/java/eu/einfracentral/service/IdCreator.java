package eu.einfracentral.service;

import eu.einfracentral.domain.*;

import java.security.NoSuchAlgorithmException;

public interface IdCreator {

    /**
     * Creates id for {@link Provider}
     *
     * @param provider Provider
     * @return {@link String}
     */
    String createProviderId(Provider provider);

    /**
     * Creates id for {@link ServiceBundle}
     *
     * @param serviceBundle Service
     * @return {@link String}
     */
    String createServiceId(ServiceBundle serviceBundle);

    /**
     * Creates id for {@link TrainingResourceBundle}
     *
     * @param trainingResourceBundle TrainingResource
     * @return {@link String}
     */
    String createTrainingResourceId(TrainingResourceBundle trainingResourceBundle) throws NoSuchAlgorithmException;

    /**
     * Creates id for {@link Catalogue}
     *
     * @param catalogue Catalogue
     * @return {@link String}
     */
    String createCatalogueId(Catalogue catalogue);

    /**
     * Creates id for {@link InteroperabilityRecord}
     *
     * @param interoperabilityRecord InteroperabilityRecord
     * @return {@link String}
     */
    String createInteroperabilityRecordId(InteroperabilityRecord interoperabilityRecord) throws NoSuchAlgorithmException;

    /**
     * Strip accents, replace special characters and transform a string to lowercase
     *
     * @param string String
     * @return {@link String}
     */
    String sanitizeString(String string);
}
