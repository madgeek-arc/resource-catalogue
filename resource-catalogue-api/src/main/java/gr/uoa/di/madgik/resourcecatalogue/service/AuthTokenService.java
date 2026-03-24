package gr.uoa.di.madgik.resourcecatalogue.service;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;

@Service
@Profile("!no-auth")
public class AuthTokenService {

    private final OAuth2AuthorizedClientManager manager;

    public AuthTokenService(OAuth2AuthorizedClientManager manager) {
        this.manager = manager;
    }

    public String getAccessToken(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauth) {
            OAuth2AuthorizeRequest authRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId(oauth.getAuthorizedClientRegistrationId())
                    .principal(oauth)
                    .build();
            OAuth2AuthorizedClient client = manager.authorize(authRequest);
            if (client != null) {
                OAuth2AccessToken accessToken = client.getAccessToken();
                return accessToken.getTokenValue();
            }
        }
        throw new InsufficientAuthenticationException("Insufficient authentication");

    }
}
