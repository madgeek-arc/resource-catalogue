package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.validators.FieldValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;

import javax.sql.DataSource;

public class CatalogueManagerTests {

    private IdCreator idCreatorMock;
    private DataSource dataSourceMock;
    private ProviderService<ProviderBundle, Authentication> providerServiceMock;
    private ServiceBundleService<ServiceBundle> serviceBundleServiceMock;
    private TrainingResourceService<TrainingResourceBundle> trainingResourceServiceMock;
    private InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordServiceMock;
    private FieldValidator fieldValidator;
    private SecurityService securityService;
    private VocabularyService vocabularyService;
    private RegistrationMailService registrationMailService;
    private ProviderResourcesCommonMethods commonMethods;
    private CatalogueManager catalogueManager;

    @BeforeEach
    public void setUp() {
        // Arrange
        idCreatorMock = Mockito.mock(IdCreator.class);
        dataSourceMock = Mockito.mock(DataSource.class);
        providerServiceMock = Mockito.mock(ProviderService.class);
        serviceBundleServiceMock = Mockito.mock(ServiceBundleService.class);
        trainingResourceServiceMock = Mockito.mock(TrainingResourceService.class);
        interoperabilityRecordServiceMock = Mockito.mock(InteroperabilityRecordService.class);
        fieldValidator = Mockito.mock(FieldValidator.class);
        securityService = Mockito.mock(SecurityService.class);
        vocabularyService = Mockito.mock(VocabularyService.class);
        registrationMailService = Mockito.mock(RegistrationMailService.class);
        commonMethods = Mockito.mock(ProviderResourcesCommonMethods.class);

        catalogueManager = new CatalogueManager(idCreatorMock, dataSourceMock, providerServiceMock,
                serviceBundleServiceMock, trainingResourceServiceMock, interoperabilityRecordServiceMock,
                fieldValidator, securityService, vocabularyService, registrationMailService, commonMethods);
    }

    @Test
    public void testGetResourceType() {
        // Act
        String resourceType = catalogueManager.getResourceType();
        // Assert
        Assertions.assertEquals("catalogue", resourceType);
    }
}