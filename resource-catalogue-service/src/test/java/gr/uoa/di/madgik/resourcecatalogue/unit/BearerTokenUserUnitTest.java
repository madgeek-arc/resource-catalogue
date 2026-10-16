package gr.uoa.di.madgik.resourcecatalogue.unit;

import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.service.AuthTokenService;
import gr.uoa.di.madgik.resourcecatalogue.service.OIDCSecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Authentication produced by the opaque-token resource server ({@link BearerTokenAuthentication})
 * must be convertible to a {@link User}, otherwise {@code @PreAuthorize} checks that reach
 * {@code OIDCSecurityService.isOrganisationAdmin} fail with a NullPointerException.
 */
class BearerTokenUserUnitTest {

    private static BearerTokenAuthentication bearerAuth(Map<String, Object> attributes, String... roles) {
        Set<GrantedAuthority> authorities = Set.of(
                java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
        DefaultOAuth2AuthenticatedPrincipal principal =
                new DefaultOAuth2AuthenticatedPrincipal((String) attributes.get("sub"), attributes, authorities);
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token",
                Instant.now(), Instant.now().plusSeconds(3600));
        return new BearerTokenAuthentication(principal, token, authorities);
    }

    private static Map<String, Object> attributes(String email) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("sub", "442a2ac5@myaccessid.org");
        attributes.put("given_name", "Jane");
        attributes.put("family_name", "Doe");
        if (email != null) {
            attributes.put("email", email);
        }
        return attributes;
    }

    // region User.of
    @Test
    void userOf_bearerTokenAuthentication_readsClaimsFromTokenAttributes() {
        User user = User.of(bearerAuth(attributes("jane.doe@example.org"), "ROLE_USER"));

        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo("442a2ac5@myaccessid.org");
        assertThat(user.getEmail()).isEqualTo("jane.doe@example.org");
        assertThat(user.getName()).isEqualTo("Jane");
        assertThat(user.getSurname()).isEqualTo("Doe");
    }

    @Test
    void userOf_unknownAuthenticatedType_throwsInformativeException() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("someone", "pw", "ROLE_USER");
        auth.setAuthenticated(true);

        assertThatThrownBy(() -> User.of(auth))
                .isInstanceOf(InsufficientAuthenticationException.class)
                .hasMessageContaining("TestingAuthenticationToken");
    }

    @Test
    void userInfoOf_bearerTokenAuthentication_includesSortedRoles() {
        UserInfo info = UserInfo.of(bearerAuth(attributes("Jane.Doe@Example.org"), "ROLE_USER", "ROLE_PROVIDER"));

        assertThat(info.email()).isEqualTo("jane.doe@example.org");
        assertThat(info.roles()).containsExactly("ROLE_PROVIDER", "ROLE_USER");
    }
    // endregion

    // region OIDCSecurityService
    private static OIDCSecurityService securityServiceWithAdmins(String providerId, String... adminEmails) {
        OrganisationService organisationService = mock(OrganisationService.class);
        OrganisationBundle bundle = mock(OrganisationBundle.class);
        LinkedHashMap<String, Object> organisation = new LinkedHashMap<>();
        organisation.put("users", java.util.Arrays.stream(adminEmails)
                .map(email -> Map.<String, Object>of("email", email))
                .toList());
        when(bundle.getOrganisation()).thenReturn(organisation);
        when(organisationService.get(providerId)).thenReturn(bundle);
        return new OIDCSecurityService(null, organisationService, null, null, null, null, null, null, null, null,
                new CatalogueProperties());
    }

    @Test
    void isOrganisationAdmin_bearerTokenOfListedUser_isTrue() {
        OIDCSecurityService service = securityServiceWithAdmins("prov1", "jane.doe@example.org");

        assertThat(service.isOrganisationAdmin(
                bearerAuth(attributes("Jane.Doe@example.org"), "ROLE_PROVIDER", "ROLE_USER"), "prov1")).isTrue();
    }

    @Test
    void isOrganisationAdmin_bearerTokenOfUnlistedUser_isFalseNotException() {
        OIDCSecurityService service = securityServiceWithAdmins("prov1", "someone.else@example.org");

        assertThat(service.isOrganisationAdmin(
                bearerAuth(attributes("jane.doe@example.org"), "ROLE_PROVIDER", "ROLE_USER"), "prov1")).isFalse();
    }

    @Test
    void isOrganisationAdmin_unrecognisedAuthenticationType_isFalseNotException() {
        OIDCSecurityService service = securityServiceWithAdmins("prov1", "jane.doe@example.org");
        TestingAuthenticationToken auth = new TestingAuthenticationToken("someone", "pw", "ROLE_USER");
        auth.setAuthenticated(true);

        assertThat(service.isOrganisationAdmin(auth, "prov1")).isFalse();
    }

    @Test
    void isOrganisationAdmin_bearerTokenWithoutEmail_isFalseNotException() {
        OIDCSecurityService service = securityServiceWithAdmins("prov1", "jane.doe@example.org");

        assertThat(service.isOrganisationAdmin(
                bearerAuth(attributes(null), "ROLE_PROVIDER", "ROLE_USER"), "prov1")).isFalse();
    }

    @Test
    void isOrganisationAdmin_bearerTokenWithBlankEmailAndOrgUserWithBlankEmail_isFalse() {
        OIDCSecurityService service = securityServiceWithAdmins("prov1", "");

        assertThat(service.isOrganisationAdmin(
                bearerAuth(attributes(""), "ROLE_PROVIDER", "ROLE_USER"), "prov1")).isFalse();
    }

    @Test
    void authTokenService_bearerTokenAuthentication_returnsCallerToken() {
        AuthTokenService tokenService = new AuthTokenService(mock(OAuth2AuthorizedClientManager.class));

        assertThat(tokenService.getAccessToken(bearerAuth(attributes("jane.doe@example.org"), "ROLE_USER")))
                .isEqualTo("token");
    }

    @Test
    void authTokenService_unsupportedAuthentication_throws() {
        AuthTokenService tokenService = new AuthTokenService(mock(OAuth2AuthorizedClientManager.class));

        assertThatThrownBy(() -> tokenService.getAccessToken(new TestingAuthenticationToken("x", "y")))
                .isInstanceOf(InsufficientAuthenticationException.class);
    }

    @Test
    void isOrganisationAdmin_anonymous_isFalse() {
        OIDCSecurityService service = securityServiceWithAdmins("prov1", "jane.doe@example.org");
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        assertThat(service.isOrganisationAdmin(anonymous, "prov1")).isFalse();
    }
    // endregion
}
