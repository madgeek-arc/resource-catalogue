package gr.uoa.di.madgik.resourcecatalogue.unit;

import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.integration.BaseIntegrationTest;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createLoggingInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProviderResourcesCommonMethodUnitTest extends BaseIntegrationTest {

    @Autowired
    private ProviderResourcesCommonMethods commonMethods;
    @Autowired
    private SecurityService securityService;

    @Test
    public void testNullLoggingInfo_CreatesNewLoggingInfo() {
        // Arrange
        ProviderBundle provider = mock(ProviderBundle.class);
        Authentication auth = securityService.getAdminAccess();

        // Simulate null logging info
        when(provider.getLoggingInfo()).thenReturn(null);

        // Act
        List<LoggingInfo> result = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(provider, auth);

        // Assert
        assertNotNull(result, "The result should not be null");
        assertEquals(1, result.size(), "The result should contain one logging info entry");
        LoggingInfo loggingInfo = result.getFirst();
        assertEquals(LoggingInfo.Types.ONBOARD.getKey(), loggingInfo.getType());
        assertEquals(LoggingInfo.ActionType.REGISTERED.getKey(), loggingInfo.getActionType());
    }

    @Test
    public void testNonEmptyLoggingInfo_ReturnsExistingList() {
        // Arrange
        List<LoggingInfo> existingLoggingInfo = List.of(createLoggingInfo(LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey()));
        ProviderBundle provider = mock(ProviderBundle.class);
        Authentication auth = securityService.getAdminAccess();

        when(provider.getLoggingInfo()).thenReturn(existingLoggingInfo);

        // Act
        List<LoggingInfo> result = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(provider, auth);

        // Assert
        assertEquals(existingLoggingInfo, result,
                "When loggingInfo is non-empty, the method should return the existing list");
    }

    @Test
    public void testEmptyLoggingInfo_CreatesNewLoggingInfo() {
        // Arrange
        ProviderBundle provider = mock(ProviderBundle.class);
        Authentication auth = securityService.getAdminAccess();

        // Simulate empty logging info
        when(provider.getLoggingInfo()).thenReturn(Collections.emptyList());

        // Act
        List<LoggingInfo> result = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(provider, auth);

        // Assert
        assertNotNull(result, "The result should not be null");
        assertEquals(1, result.size(), "The result should contain one logging info entry");
        LoggingInfo loggingInfo = result.getFirst();
        assertEquals(LoggingInfo.Types.ONBOARD.getKey(), loggingInfo.getType());
        assertEquals(LoggingInfo.ActionType.REGISTERED.getKey(), loggingInfo.getActionType());
    }

    //TODO: implement this
//    @Test
//    public void testNullAuthentication_CreatesDefaultLoggingInfo() {
//        // Arrange
//        ProviderBundle provider = mock(ProviderBundle.class);
//        when(provider.getLoggingInfo()).thenReturn(null);
//
//        // Pass null as the authentication object
//        Authentication auth = null;
//
//        // Act
//        List<LoggingInfo> result = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(provider, auth);
//
//        // Assert
//        assertNotNull(result, "The result should not be null");
//        assertEquals(1, result.size(), "The result should contain one logging info entry");
//        LoggingInfo loggingInfo = result.getFirst();
//        assertEquals(LoggingInfo.Types.ONBOARD.getKey(), loggingInfo.getType());
//        assertEquals(LoggingInfo.ActionType.REGISTERED.getKey(), loggingInfo.getActionType());
//    }
}
