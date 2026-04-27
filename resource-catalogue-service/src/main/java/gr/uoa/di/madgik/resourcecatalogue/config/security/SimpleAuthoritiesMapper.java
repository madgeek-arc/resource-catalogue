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

import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.service.AuthoritiesMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Fallback AuthoritiesMapper used when OrganisationService is unavailable (e.g. the lot1 build).
 * Grants ROLE_ADMIN and ROLE_EPOT from catalogue properties; all authenticated users receive ROLE_USER.
 */
@Component
public class SimpleAuthoritiesMapper implements AuthoritiesMapper {

    private static final Logger logger = LoggerFactory.getLogger(SimpleAuthoritiesMapper.class);

    private final CatalogueProperties catalogueProperties;

    public SimpleAuthoritiesMapper(CatalogueProperties catalogueProperties) {
        this.catalogueProperties = catalogueProperties;
        if (catalogueProperties.getAdmins().isEmpty()) {
            throw new IllegalStateException("No Admins Provided");
        }
    }

    @PostConstruct
    void init() {
        logger.info("SimpleAuthoritiesMapper active — provider role lookup is not available in this build");
    }

    @Override
    public boolean isAdmin(String email) {
        return catalogueProperties.getAdmins().contains(email.toLowerCase());
    }

    @Override
    public boolean isEPOT(String email) {
        return catalogueProperties.getOnboardingTeam() != null
                && catalogueProperties.getOnboardingTeam().contains(email.toLowerCase());
    }

    @Override
    public Set<GrantedAuthority> getAuthorities(String email) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        String lower = email.toLowerCase();
        if (catalogueProperties.getAdmins().contains(lower)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        if (catalogueProperties.getOnboardingTeam() != null
                && catalogueProperties.getOnboardingTeam().contains(lower)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_EPOT"));
        }
        return authorities;
    }

    @Override
    public void updateAuthorities() {
        // no-op: authorities are derived directly from CatalogueProperties
    }
}
