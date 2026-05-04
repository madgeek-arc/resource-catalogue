package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("publicCatalogueManager")
public class PublicCatalogueService extends AbstractPublicResourceManager<CatalogueBundle> {

    private final OrganisationService organisationService;

    public PublicCatalogueService(GenericResourceService genericResourceService,
                                  JmsService jmsService,
                                  PidIssuer pidIssuer,
                                  FacetLabelService facetLabelService,
                                  OrganisationService organisationService) {
        super(genericResourceService, jmsService, pidIssuer, facetLabelService);
        this.organisationService = organisationService;
    }

    @Override
    protected String getResourceTypeName() {
        return "catalogue";
    }

    @Override
    public void updateIdsToPublic(CatalogueBundle catalogue) {
        // Resource Owner
        OrganisationBundle provider = organisationService.get(
                (String) catalogue.getCatalogue().get("resourceOwner"),
                catalogue.getCatalogueId()
        );
        catalogue.getCatalogue().put("resourceOwner", provider.getIdentifiers().getPid());

        // Service Providers
        Object providersObj = catalogue.getCatalogue().get("serviceProviders");
        if (providersObj instanceof List<?> providersList && !providersList.isEmpty()) {
            List<String> updatedServiceProviders = new ArrayList<>();
            for (Object providerObj : providersList) {
                if (providerObj instanceof String providerId) {
                    OrganisationBundle bundle = organisationService.get(providerId, catalogue.getCatalogueId());
                    updatedServiceProviders.add(bundle.getIdentifiers().getPid());
                }
            }
            catalogue.getCatalogue().put("serviceProviders", updatedServiceProviders);
        }
    }
}
