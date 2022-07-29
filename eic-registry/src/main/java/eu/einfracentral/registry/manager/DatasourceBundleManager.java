package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.CatalogueService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.validators.FieldValidator;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Random;

@org.springframework.stereotype.Service
public class DatasourceBundleManager extends AbstractResourceBundleManager<DatasourceBundle> implements ResourceBundleService<DatasourceBundle> {

    private static final Logger logger = LogManager.getLogger(DatasourceBundleManager.class);

    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final Random randomNumberGenerator;
    private final FieldValidator fieldValidator;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;

    public DatasourceBundleManager(ProviderService<ProviderBundle, Authentication> providerService,
                                   Random randomNumberGenerator, IdCreator idCreator,
                                   @Lazy FieldValidator fieldValidator,
                                   @Lazy SecurityService securityService,
                                   @Lazy RegistrationMailService registrationMailService,
                                   @Lazy VocabularyService vocabularyService,
                                   CatalogueService<CatalogueBundle, Authentication> catalogueService) {
        super(DatasourceBundle.class);
        this.providerService = providerService; // for providers
        this.randomNumberGenerator = randomNumberGenerator;
        this.idCreator = idCreator;
        this.fieldValidator = fieldValidator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.catalogueService = catalogueService;
    }

    @Override
    public DatasourceBundle addResource(DatasourceBundle service, Authentication auth) {
        return null;
    }

    @Override
    public DatasourceBundle addResource(DatasourceBundle service, String catalogueId, Authentication auth) {
        return null;
    }

    @Override
    public DatasourceBundle updateResource(DatasourceBundle service, String comment, Authentication auth) throws ResourceNotFoundException {
        return null;
    }

    @Override
    public DatasourceBundle updateResource(DatasourceBundle service, String catalogueId, String comment, Authentication auth) throws ResourceNotFoundException {
        return null;
    }

    @Override
    public DatasourceBundle getCatalogueService(String catalogueId, String serviceId, Authentication auth) {
        return null;
    }

    @Override
    public Paging<DatasourceBundle> getInactiveServices() {
        return null;
    }

    @Override
    public boolean validate(DatasourceBundle service) {
        return false;
    }

    @Override
    public List<Service> createFeaturedServices() {
        return null;
    }

    @Override
    public DatasourceBundle publish(String serviceId, String version, boolean active, Authentication auth) {
        return null;
    }

    @Override
    public DatasourceBundle auditResource(String serviceId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        return null;
    }

    @Override
    public Paging<DatasourceBundle> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth) {
        return null;
    }

    @Override
    public List<DatasourceBundle> getInfraServices(String providerId, Authentication auth) {
        return null;
    }

    @Override
    public Paging<DatasourceBundle> getInfraServices(String catalogueId, String providerId, Authentication auth) {
        return null;
    }

    @Override
    public List<Service> getServices(String providerId, Authentication auth) {
        return null;
    }

    @Override
    public List<Service> getActiveServices(String providerId) {
        return null;
    }

    @Override
    public DatasourceBundle getServiceTemplate(String providerId, Authentication auth) {
        return null;
    }

    @Override
    public Service getFeaturedService(String providerId) {
        return null;
    }

    @Override
    public List<DatasourceBundle> getInactiveServices(String providerId) {
        return null;
    }

    @Override
    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth) {

    }

    @Override
    public Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId) {
        return null;
    }

    @Override
    public DatasourceBundle verifyResource(String id, String status, Boolean active, Authentication auth) {
        return null;
    }

    @Override
    public DatasourceBundle changeProvider(String resourceId, String newProvider, String comment, Authentication auth) {
        return null;
    }
}
