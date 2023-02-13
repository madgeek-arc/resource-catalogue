package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Identifiable;
import eu.einfracentral.domain.ResourceBundle;
import eu.einfracentral.domain.TrainingResourceBundle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractPublicResourceManager<T extends Identifiable> extends ResourceManager<T> {

    public AbstractPublicResourceManager(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    protected List<String> appendCatalogueId(List<String> items, String catalogueId) {
        Set<String> transformed = new HashSet<>();
        if (items != null) {
            for (String item : items) {
                if (!item.contains(catalogueId)) {
                    item = catalogueId + "." + item;
                }
                transformed.add(item);
            }
        }
        return new ArrayList<>(transformed);
    }

    protected void updateResourceIdsToPublic(ResourceBundle<?> resourceBundle) {
        // Resource Organisation
        resourceBundle.getPayload().setResourceOrganisation(
                String.format("%s.%s",
                        resourceBundle.getPayload().getCatalogueId(),
                        resourceBundle.getPayload().getResourceOrganisation()));

        // Resource Providers
        resourceBundle.getPayload().setResourceProviders(
                appendCatalogueId(
                        resourceBundle.getPayload().getResourceProviders(),
                        resourceBundle.getPayload().getCatalogueId()));

        // Related Resources
        resourceBundle.getPayload().setRelatedResources(
                appendCatalogueId(
                        resourceBundle.getPayload().getRelatedResources(),
                        resourceBundle.getPayload().getCatalogueId()));

        // Required Resources
        resourceBundle.getPayload().setRequiredResources(
                appendCatalogueId(
                        resourceBundle.getPayload().getRequiredResources(),
                        resourceBundle.getPayload().getCatalogueId()));
    }

    protected void updateTrainingResourceIdsToPublic(TrainingResourceBundle trainingResourceBundle) {
        // Resource Organisation
        trainingResourceBundle.getTrainingResource().setResourceOrganisation(
                String.format("%s.%s",
                        trainingResourceBundle.getTrainingResource().getCatalogueId(),
                        trainingResourceBundle.getTrainingResource().getResourceOrganisation()));

        // Resource Providers
        trainingResourceBundle.getTrainingResource().setResourceProviders(
                appendCatalogueId(
                        trainingResourceBundle.getTrainingResource().getResourceProviders(),
                        trainingResourceBundle.getTrainingResource().getCatalogueId()));

        // EOSC Related Services
        trainingResourceBundle.getTrainingResource().setEoscRelatedServices(
                appendCatalogueId(
                        trainingResourceBundle.getTrainingResource().getEoscRelatedServices(),
                        trainingResourceBundle.getTrainingResource().getCatalogueId()));
    }
}
