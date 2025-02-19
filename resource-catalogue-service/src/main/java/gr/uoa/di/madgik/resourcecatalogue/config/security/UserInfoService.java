/**
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserInfoService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final WebClient webClient;


    public UserInfoService(ClientRegistrationRepository clientRegistrationRepository,
                           WebClient.Builder webClientBuilder) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.webClient = webClientBuilder.build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfo(String registrationId, String accessToken) {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId);
        String userInfoEndpointUri = clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri();

        if (userInfoEndpointUri == null) {
            throw new IllegalStateException("User Info URI is not available");
        }

        return webClient
                .get()
                .uri(userInfoEndpointUri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(LinkedHashMap.class)
                .block();
    }
}