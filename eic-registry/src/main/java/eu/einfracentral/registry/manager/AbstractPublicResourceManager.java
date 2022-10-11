package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Identifiable;
import eu.einfracentral.domain.ResourceBundle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractPublicResourceManager <T extends Identifiable> extends ResourceManager<T> {

    public AbstractPublicResourceManager(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    protected List<String> appendCatalogueId(List<String> items, String catalogueId) {
        Set<String> transformed = new HashSet<>();
        for (String item : items){
            if (!item.contains(catalogueId)){
                item = catalogueId + "." + item;
            }
            transformed.add(item);
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
        if (resourceBundle.getPayload().getResourceProviders() != null && !resourceBundle.getPayload().getResourceProviders().isEmpty()){
            resourceBundle.getPayload().setResourceProviders(
                    appendCatalogueId(
                            resourceBundle.getPayload().getResourceProviders(),
                            resourceBundle.getPayload().getCatalogueId()));
        }

        // Related Resources
        if (resourceBundle.getPayload().getRelatedResources() != null && !resourceBundle.getPayload().getRelatedResources().isEmpty()){
            resourceBundle.getPayload().setRelatedResources(
                    appendCatalogueId(
                            resourceBundle.getPayload().getRelatedResources(),
                            resourceBundle.getPayload().getCatalogueId()));
        }

        // Required Resources
        if (resourceBundle.getPayload().getRequiredResources() != null && !resourceBundle.getPayload().getRequiredResources().isEmpty()){
            resourceBundle.getPayload().setRequiredResources(
                    appendCatalogueId(
                            resourceBundle.getPayload().getRequiredResources(),
                            resourceBundle.getPayload().getCatalogueId()));
        }
    }
}
