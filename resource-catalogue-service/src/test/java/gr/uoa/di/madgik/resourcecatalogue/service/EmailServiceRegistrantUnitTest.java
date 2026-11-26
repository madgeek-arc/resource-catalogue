package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.NodeProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * The applicant named in onboarding mails is the registrant of the resource itself, falling back to the
 * registrant of its Organisation.
 */
class EmailServiceRegistrantUnitTest {

    private EmailService emailService;
    private OrganisationBundle organisation;
    private ServiceBundle service;

    @BeforeEach
    void setUp() {
        CatalogueProperties properties = mock(CatalogueProperties.class, RETURNS_DEEP_STUBS);
        emailService = new EmailService(null, null, null, null, null, null, null, null, null, null,
                properties, mock(NodeProperties.class), null);
        organisation = TestUtils.createOrganisationBundle();
        organisation.setLoggingInfo(new ArrayList<>());
        service = TestUtils.createServiceBundle();
        service.setLoggingInfo(new ArrayList<>());
    }

    private static LoggingInfo registered(String email, String fullName) {
        LoggingInfo info = new LoggingInfo();
        info.setActionType(LoggingInfo.ActionType.REGISTERED.getKey());
        info.setUserEmail(email);
        info.setUserFullName(fullName);
        return info;
    }

    @Test
    void resourceRegistrantWinsOverOrganisationRegistrant() {
        organisation.getLoggingInfo().add(registered("org@example.org", "Org Owner"));
        service.getLoggingInfo().add(registered("res@example.org", "Res Creator"));

        User user = emailService.extractRegisteredUser(service, organisation);

        assertThat(user.getEmail()).isEqualTo("res@example.org");
        assertThat(user.getName()).isEqualTo("Res");
    }

    @Test
    void fallsBackToOrganisationRegistrant() {
        organisation.getLoggingInfo().add(registered("org@example.org", "Org Owner"));

        User user = emailService.extractRegisteredUser(service, organisation);

        assertThat(user.getEmail()).isEqualTo("org@example.org");
    }

    @Test
    void fallsBackToDefaultUser() {
        User user = emailService.extractRegisteredUser(service, organisation);

        assertThat(user.getEmail()).isEqualTo("no email provided");
        assertThat(user.getName()).isEqualTo("Unknown");
    }
}
