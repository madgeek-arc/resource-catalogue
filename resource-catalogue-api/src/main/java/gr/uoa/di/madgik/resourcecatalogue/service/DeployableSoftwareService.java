package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.DeployableSoftwareBundle;

public interface DeployableSoftwareService extends ResourceCatalogueGenericService<DeployableSoftwareBundle>,
        EOSCResourceService<DeployableSoftwareBundle>, TemplateOnboardingService, DraftService<DeployableSoftwareBundle> {
}
