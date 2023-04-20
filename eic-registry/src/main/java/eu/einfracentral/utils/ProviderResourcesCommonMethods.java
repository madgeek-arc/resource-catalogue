package eu.einfracentral.utils;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class ProviderResourcesCommonMethods {

    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;

    public ProviderResourcesCommonMethods(CatalogueService<CatalogueBundle, Authentication> catalogueService) {
        this.catalogueService = catalogueService;
    }

    public void checkCatalogueIdConsistency(Object o, String catalogueId) {
        catalogueService.existsOrElseThrow(catalogueId);
        if (o != null) {
            if (o instanceof ProviderBundle){
                if (((ProviderBundle) o).getPayload().getCatalogueId() == null || ((ProviderBundle) o).getPayload().getCatalogueId().equals("")) {
                    throw new ValidationException("Provider's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((ProviderBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Provider's 'catalogueId' don't match");
                    }
                }
            }
            if (o instanceof DatasourceBundle){
                if (((DatasourceBundle) o).getPayload().getCatalogueId() == null || ((DatasourceBundle) o).getPayload().getCatalogueId().equals("")) {
                    throw new ValidationException("Datasource's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((DatasourceBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Datasource's 'catalogueId' don't match");
                    }
                }
            }
            if (o instanceof ServiceBundle){
                if (((ServiceBundle) o).getPayload().getCatalogueId() == null || ((ServiceBundle) o).getPayload().getCatalogueId().equals("")) {
                    throw new ValidationException("Service's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((ServiceBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Service's 'catalogueId' don't match");
                    }
                }
            }
            if (o instanceof TrainingResourceBundle){
                if (((TrainingResourceBundle) o).getPayload().getCatalogueId() == null || ((TrainingResourceBundle) o).getPayload().getCatalogueId().equals("")) {
                    throw new ValidationException("Training Resource's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((TrainingResourceBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Training Resource's 'catalogueId' don't match");
                    }
                }
            }
            if (o instanceof InteroperabilityRecordBundle){
                if (((InteroperabilityRecordBundle) o).getPayload().getCatalogueId() == null || ((InteroperabilityRecordBundle) o).getPayload().getCatalogueId().equals("")) {
                    throw new ValidationException("Interoperability Record's 'catalogueId' cannot be null or empty");
                } else {
                    if (!((InteroperabilityRecordBundle) o).getPayload().getCatalogueId().equals(catalogueId)) {
                        throw new ValidationException("Parameter 'catalogueId' and Interoperability Record's 'catalogueId' don't match");
                    }
                }
            }
        }
    }
}