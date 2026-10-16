/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.config.security;

import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

@Profile("!no-auth")
@Service
public class UserInfoService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RestClient restClient;


    public UserInfoService(ClientRegistrationRepository clientRegistrationRepository, RestClient restClient) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.restClient = restClient;
    }

    /**
     * Get a UserInfo response for a user.
     *
     * @param registrationId the id of the client registration
     * @param accessToken    the access token of the user
     * @return the response from the UserInfo endpoint
     * @throws HttpClientErrorException.Unauthorized if the access token is invalid
     * @throws HttpClientErrorException.Forbidden    if the access token has insufficient scope
     * @throws RestClientResponseException           if another HTTP error occurs.
     * @throws IllegalStateException                 if the client registration is not found or a UserInfo
     *                                               endpoint is not configured for the client registration
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfo(String registrationId, String accessToken) {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId);

        if (clientRegistration == null) {
            throw new IllegalStateException("Client registration " + registrationId + " not found");
        }

        String userInfoEndpointUri = clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri();

        if (userInfoEndpointUri == null) {
            throw new IllegalStateException("User Info URI is not available");
        }

        return restClient
                .get()
                .uri(userInfoEndpointUri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(LinkedHashMap.class);
    }
}