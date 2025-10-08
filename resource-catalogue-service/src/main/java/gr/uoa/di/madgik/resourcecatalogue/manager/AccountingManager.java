/*
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

package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.resourcecatalogue.config.AccountingProperties;
import gr.uoa.di.madgik.resourcecatalogue.service.AccountingService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;

@Service("accountingManager")
public class AccountingManager implements AccountingService {

    private final AccountingProperties accountingProperties;
    private final WebClient webClient;

    private String accessToken;
    private Instant expiryTime;

    public AccountingManager(AccountingProperties accountingProperties,
                             WebClient.Builder webClientBuilder) {
        this.accountingProperties = accountingProperties;
        this.webClient = webClientBuilder.build();
    }

    public synchronized String getAccessToken() {
        if (accessToken == null || Instant.now().isAfter(expiryTime)) {
            refreshToken();
        }
        return accessToken;
    }

    private void refreshToken() {
        Map<String, Object> response = webClient.post()
                .uri(accountingProperties.getTokenEndpoint())
                .headers(headers -> headers.setBasicAuth(accountingProperties.getClientId(),
                        accountingProperties.getClientSecret()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("scope", "openid email entitlements"))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        assert response != null;
        this.accessToken = (String) response.get("access_token");
        Integer expiresIn = (Integer) response.get("expires_in");
        this.expiryTime = Instant.now().plusSeconds(expiresIn - 60); // refresh 1 min earlier
    }
}
