package eu.einfracentral.utils;

import eu.einfracentral.domain.ResourceBundle;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ResourceBundleService;
import org.apache.commons.lang3.StringUtils;

public class ResourceValidationUtils {

    public static <T extends ResourceBundle<?>> void checkIfResourceBundleActiveAndApprovedAndNotPublic(String resourceId, String catalogueId,
                                                                                                        ResourceBundleService<T> resourceBundleService,
                                                                                                        String resourceType) {
        T resourceBundle;
        resourceType = StringUtils.capitalize(resourceType);
        // check if Resource exists
        try {
            resourceBundle = resourceBundleService.get(resourceId, catalogueId);
            // check if Service is Public
            if (resourceBundle.getMetadata().isPublished()) {
                throw new ValidationException(String.format("Please provide a %s ID with no catalogue prefix.", resourceType));
            }
        } catch (ResourceNotFoundException e) {
            throw new ValidationException(String.format("There is no %s with id '%s' in the '%s' Catalogue", resourceType, resourceId, catalogueId));
        }
        // check if Service is Active + Approved
        if (!resourceBundle.isActive() || !resourceBundle.getStatus().equals("approved resource")) {
            throw new ValidationException(String.format("%s with ID '%s' is not Approved and/or Active", resourceType, resourceId));
        }
    }

    private ResourceValidationUtils() {
    }
}
