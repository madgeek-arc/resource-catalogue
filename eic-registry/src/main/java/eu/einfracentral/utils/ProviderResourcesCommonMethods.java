package eu.einfracentral.utils;

import eu.einfracentral.domain.CatalogueBundle;
import eu.einfracentral.domain.InteroperabilityRecordBundle;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class ProviderResourcesCommonMethods{

    private static final Logger logger = LogManager.getLogger(ProviderResourcesCommonMethods.class);
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;

    public ProviderResourcesCommonMethods(CatalogueService<CatalogueBundle, Authentication> catalogueService) {
        this.catalogueService = catalogueService;
    }

    public void checkCatalogueIdConsistency(Object o, String catalogueId) {
        catalogueService.existsOrElseThrow(catalogueId);
        if (o != null) {
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