package eu.einfracentral.service;

public interface ElasticValidatorService {
    /**
     * Validates if all Resources of a specific Resource Type of the DB are registered on Elastic.
     *
     * @param resourceType Resource Type
     */
    void validate(String resourceType);
}
