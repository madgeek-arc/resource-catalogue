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

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigUnitTest {

    @Test
    void resolveEntitlementAuthorities_readAndWriteUrnsMapToBothRoles() {
        Map<String, Object> attributes = Map.of("entitlements", List.of(
                "urn:geant:sandbox.eosc-beyond.eu:core:group:service-catalogue:role=read",
                "urn:geant:sandbox.eosc-beyond.eu:core:group:service-catalogue:role=write",
                "urn:geant:sandbox.eosc-beyond.eu:core:group:service-catalogue:role=epot",
                "urn:geant:sandbox.eosc-beyond.eu:core:group:service-catalogue:role=admin"
        ));

        Set<GrantedAuthority> authorities = SecurityConfig.resolveEntitlementAuthorities(attributes);

        assertThat(authorities).containsExactlyInAnyOrder(
                new SimpleGrantedAuthority("ROLE_READ"),
                new SimpleGrantedAuthority("ROLE_WRITE"),
                new SimpleGrantedAuthority("ROLE_EPOT"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
    }

    @Test
    void resolveEntitlementAuthorities_differentGroupYieldsNoRoles() {
        Map<String, Object> attributes = Map.of("entitlements", List.of(
                "urn:geant:sandbox.eosc-beyond.eu:core:group:some-other-service:role=write"
        ));

        Set<GrantedAuthority> authorities = SecurityConfig.resolveEntitlementAuthorities(attributes);

        assertThat(authorities).isEmpty();
    }

    @Test
    void resolveEntitlementAuthorities_unrecognizedRoleYieldsNoRoles() {
        Map<String, Object> attributes = Map.of("entitlements", List.of(
                "urn:geant:sandbox.eosc-beyond.eu:core:group:service-catalogue:role=member"
        ));

        Set<GrantedAuthority> authorities = SecurityConfig.resolveEntitlementAuthorities(attributes);

        assertThat(authorities).isEmpty();
    }

    @Test
    void resolveEntitlementAuthorities_missingEntitlementsYieldsEmptySet() {
        assertThat(SecurityConfig.resolveEntitlementAuthorities(Map.of())).isEmpty();
        assertThat(SecurityConfig.resolveEntitlementAuthorities(null)).isEmpty();
    }

    @Test
    void resolveEntitlementAuthorities_singleStringEntitlementIsSupported() {
        Map<String, Object> attributes = Map.of("entitlements",
                "urn:geant:sandbox.eosc-beyond.eu:core:group:service-catalogue:role=read");

        Set<GrantedAuthority> authorities = SecurityConfig.resolveEntitlementAuthorities(attributes);

        assertThat(authorities).containsExactly(new SimpleGrantedAuthority("ROLE_READ"));
    }
}
