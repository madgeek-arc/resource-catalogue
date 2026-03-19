package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.DeployableApplicationBundle;

public interface DeployableApplicationService extends ResourceCatalogueGenericService<DeployableApplicationBundle>,
        EOSCResourceService<DeployableApplicationBundle>, TemplateOnboardingService, DraftService<DeployableApplicationBundle> {
}
