package gr.uoa.di.madgik.resourcecatalogue.integration;

import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createLoggingInfo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProviderResourcesCommonMethodIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProviderResourcesCommonMethods commonMethods;
    @Autowired
    private SecurityService securityService;

    // returnLoggingInfoListAndCreateRegistrationInfoIfEmpty()
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
    public void testNullAuthentication_ThrowsInsufficientAuthenticationException() {
        // Arrange
        ProviderBundle provider = mock(ProviderBundle.class);
        when(provider.getLoggingInfo()).thenReturn(null);

        // Act & Assert
        InsufficientAuthenticationException exception = assertThrows(
                InsufficientAuthenticationException.class,
                () -> commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(provider, null),
                "Expected InsufficientAuthenticationException to be thrown"
        );

        assertEquals("You are not authenticated, please log in.", exception.getMessage(),
                "Exception message should match the expected message");
    }

    // createLoggingInfo()
    @Test
    public void testCreateLoggingInfo_ValidInput_ReturnsCorrectLoggingInfo() {
        // Arrange
        Authentication auth = securityService.getAdminAccess();

        // Act
        LoggingInfo result = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());

        // Assert
        assertNotNull(result, "LoggingInfo should not be null");
        assertEquals(LoggingInfo.Types.UPDATE.getKey(), result.getType(), "Type should match");
        assertEquals(LoggingInfo.ActionType.UPDATED.getKey(), result.getActionType(), "ActionType should match");
        assertEquals("admin", result.getUserRole(), "User role should match");
    }

    @Test
    public void testCreateLoggingInfo_NullAuthentication_ThrowsException() {
        // Act & Assert
        InsufficientAuthenticationException exception = assertThrows(
                InsufficientAuthenticationException.class,
                () -> commonMethods.createLoggingInfo(null, LoggingInfo.Types.UPDATE.getKey(),
                        LoggingInfo.ActionType.UPDATED.getKey()),
                "Expected InsufficientAuthenticationException to be thrown"
        );

        assertEquals("You are not authenticated, please log in.", exception.getMessage(),
                "Exception message should match the expected message");
    }

    @Test
    public void testCreateLoggingInfo_NullOrEmptyOrNonexistentTypeOrActionType() {
        // Arrange
        Authentication auth = securityService.getAdminAccess();

        // Act & Assert
        // test null type
        IllegalArgumentException exception1 = assertThrows(
                IllegalArgumentException.class,
                () -> commonMethods.createLoggingInfo(auth, null, LoggingInfo.ActionType.UPDATED.getKey()),
                "Expected IllegalArgumentException to be thrown"
        );
        assertEquals("LoggingInfo Type and ActionType cannot be null", exception1.getMessage(),
                "Exception message should match the expected message");

        // test empty actionType
        IllegalArgumentException exception2 = assertThrows(
                IllegalArgumentException.class,
                () -> commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(), ""),
                "Expected IllegalArgumentException to be thrown"
        );
        assertEquals("Invalid action type: ", exception2.getMessage(),
                "Exception message should match the expected message");

        // test nonexistent actionType
        IllegalArgumentException exception3 = assertThrows(
                IllegalArgumentException.class,
                () -> commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(), "testActionType"),
                "Expected IllegalArgumentException to be thrown"
        );
        assertEquals("Invalid action type: testActionType", exception3.getMessage(),
                "Exception message should match the expected message");
    }
}
