package gr.uoa.di.madgik.resourcecatalogue.unit;

import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.OIDCSecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProviderCanAddResourcesUnitTest {

    private static final String ORGANISATION_ID = "21.T15999/org";
    private static final String ADMIN_EMAIL = "admin@example.org";
    private static final String OTHER_EMAIL = "other@example.org";

    private final OrganisationService organisationService = mock(OrganisationService.class);
    private OIDCSecurityService securityService;
    private OrganisationBundle organisation;

    @BeforeEach
    void setUp() {
        securityService = new OIDCSecurityService(null, organisationService, null, null, null, null, null, null, null,
                null, new CatalogueProperties());

        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", ORGANISATION_ID);
        payload.put("users", List.of(Map.of("id", "admin", "email", ADMIN_EMAIL, "name", "Ann", "surname", "Admin")));
        organisation = new OrganisationBundle();
        organisation.setOrganisation(payload);
        organisation.setActive(true);
        organisation.setStatus("approved");

        when(organisationService.get(ORGANISATION_ID, null)).thenReturn(organisation);
        when(organisationService.get(ORGANISATION_ID)).thenReturn(organisation);
    }

    @ParameterizedTest
    @ValueSource(strings = {"no template status", "rejected template", "pending template", "approved template"})
    void unauthenticatedCallerIsRefused(String templateStatus) {
        organisation.setTemplateStatus(templateStatus);

        assertThat(canAddResources(null)).isFalse();
        assertThat(canAddResources(anonymous())).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"no template status", "rejected template", "pending template", "approved template"})
    void userOutsideTheOrganisationIsRefused(String templateStatus) {
        organisation.setTemplateStatus(templateStatus);

        assertThat(canAddResources(user(OTHER_EMAIL))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"no template status", "rejected template", "pending template", "approved template"})
    void organisationAdminIsAllowed(String templateStatus) {
        organisation.setTemplateStatus(templateStatus);

        assertThat(canAddResources(user(ADMIN_EMAIL))).isTrue();
    }

    @Test
    void organisationAdminIsRefusedWhenOrganisationIsInactive() {
        organisation.setTemplateStatus("no template status");
        organisation.setActive(false);

        assertThat(canAddResources(user(ADMIN_EMAIL))).isFalse();
    }

    @Test
    void organisationAdminIsRefusedWhenOrganisationIsNotApproved() {
        organisation.setTemplateStatus("no template status");
        organisation.setStatus("pending");

        assertThat(canAddResources(user(ADMIN_EMAIL))).isFalse();
    }

    private boolean canAddResources(Authentication auth) {
        LinkedHashMap<String, Object> resource = new LinkedHashMap<>();
        resource.put("resourceOwner", ORGANISATION_ID);
        return securityService.providerCanAddResources(auth, resource, null);
    }

    private static Authentication anonymous() {
        return new AnonymousAuthenticationToken("key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    }

    private static Authentication user(String email) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("sub", email);
        attributes.put("given_name", "Jane");
        attributes.put("family_name", "Doe");
        attributes.put("email", email);
        Set<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        DefaultOAuth2AuthenticatedPrincipal principal =
                new DefaultOAuth2AuthenticatedPrincipal(email, attributes, Set.copyOf(authorities));
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token",
                Instant.now(), Instant.now().plusSeconds(3600));
        return new BearerTokenAuthentication(principal, token, Set.copyOf(authorities));
    }
}
