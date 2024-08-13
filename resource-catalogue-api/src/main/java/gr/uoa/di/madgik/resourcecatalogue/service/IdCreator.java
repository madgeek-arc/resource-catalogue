package gr.uoa.di.madgik.resourcecatalogue.service;

public interface IdCreator {

    /**
     * Generate ID for all user-generated resources
     *
     * @param resourceType resourceType
     * @return {@link String}
     */
    String generate(String resourceType);

    /**
     * Strip accents, replace special characters and transform a string to lowercase
     *
     * @param string String
     * @return {@link String}
     */
    String sanitizeString(String string);
}
