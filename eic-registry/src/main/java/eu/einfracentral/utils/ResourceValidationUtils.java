package eu.einfracentral.utils;

import eu.einfracentral.domain.ResourceBundle;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ResourceBundleService;

public class ResourceValidationUtils {

    public static <T extends ResourceBundle<?>> void checkIfResourceBundleActiveAndApprovedAndNotPublic(String resourceId, String catalogueId, ResourceBundleService<T> resourceBundleService) {
        T resourceBundle;
        // check if Resource exists
        try {
            resourceBundle = resourceBundleService.get(resourceId, catalogueId);
            // check if Service is Public
            if (resourceBundle.getMetadata().isPublished()) {
                throw new ValidationException("Please provide a Resource ID with no catalogue prefix.");
            }
        } catch (ResourceNotFoundException e) {
            throw new ValidationException(String.format("There is no Resource with id '%s' in the '%s' Catalogue", resourceId, catalogueId));
        }
        // check if Service is Active + Approved
        if (!resourceBundle.isActive() || !resourceBundle.getStatus().equals("approved resource")) {
            throw new ValidationException(String.format("Resource with ID [%s] is not Approved and/or Active", resourceId));
        }
    }

    private ResourceValidationUtils() {
    }
}
