package eu.einfracentral.service;

public interface ElasticDBValidatorService {
    /**
     * Validates if all Resources of a specific Resource Type of the DB are registered on Elastic.
     *
     * @param resourceType        Resource Type
     * @param validateDBtoElastic boolean value indicating the direction of the validation process
     */
    void validate(String resourceType, boolean validateDBtoElastic);
}
