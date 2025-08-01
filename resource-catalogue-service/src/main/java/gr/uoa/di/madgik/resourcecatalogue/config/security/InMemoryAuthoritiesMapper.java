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

package gr.uoa.di.madgik.resourcecatalogue.config.security;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.config.dynamicproperties.PropertyChangeEvent;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.service.AuthoritiesMapper;
import gr.uoa.di.madgik.resourcecatalogue.service.CatalogueService;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class InMemoryAuthoritiesMapper implements AuthoritiesMapper {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryAuthoritiesMapper.class);
    private Set<String> providerUsers = new HashSet<>();
    private Set<String> catalogueUsers = new HashSet<>();
    private final Map<String, Set<SimpleGrantedAuthority>> adminsAndEpot = new HashMap<>();
    private final int maxQuantity;

    private final ProviderService providerService;

    private final CatalogueService catalogueService;
    private final SecurityService securityService;
    private final CatalogueProperties catalogueProperties;


    private final ReentrantLock lock = new ReentrantLock();

    public InMemoryAuthoritiesMapper(@Value("${elastic.index.max_result_window:10000}") int maxQuantity,
                                     CatalogueProperties catalogueProperties,
                                     ProviderService manager,
                                     CatalogueService catalogueService,
                                     SecurityService securityService) {
        this.catalogueProperties = catalogueProperties;
        this.providerService = manager;
        this.catalogueService = catalogueService;
        this.securityService = securityService;
        this.maxQuantity = maxQuantity;
        if (catalogueProperties.getAdmins().isEmpty()) {
            throw new ServiceException("No Admins Provided");
        }
    }

    @PostConstruct
    void createAdminsOnStartup() {
        mergeRoles(adminsAndEpot, catalogueProperties.getOnboardingTeam()
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toMap(
                        Function.identity(),
                        a -> new SimpleGrantedAuthority("ROLE_EPOT"))
                ));

        mergeRoles(adminsAndEpot, catalogueProperties.getAdmins()
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toMap(
                        Function.identity(),
                        a -> new SimpleGrantedAuthority("ROLE_ADMIN"))
                ));
        updateAuthorities();
    }

    @Override
    public boolean isAdmin(String email) {
        if (!adminsAndEpot.containsKey(email)) {
            return false;
        } else {
            return adminsAndEpot.get(email)
                    .stream()
                    .anyMatch(simpleGrantedAuthority -> simpleGrantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        }
    }

    @Override
    public boolean isEPOT(String email) {
        if (!adminsAndEpot.containsKey(email)) {
            return false;
        } else {
            return adminsAndEpot.get(email).stream()
                    .anyMatch(simpleGrantedAuthority -> simpleGrantedAuthority.getAuthority().equals("ROLE_EPOT"));
        }
    }

    @Override
    public void updateAuthorities() {
        long time = System.nanoTime();
        FacetFilter ff = new FacetFilter();
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);

        List<ProviderBundle> providers = new ArrayList<>();
        try {
            providers.addAll(providerService.getAll(ff, securityService.getAdminAccess()).getResults());
        } catch (Exception e) {
            logger.warn("There are no Provider entries in DB");
        }

        List<CatalogueBundle> catalogues = new ArrayList<>();
        ff.getFilter().remove("published");
        try {
            catalogues.addAll(catalogueService.getAll(ff, securityService.getAdminAccess()).getResults());
        } catch (Exception e) {
            logger.warn("There are no Catalogue entries in DB");
        }

        lock.lock();
        providerUsers = getProviderUserEmails(providers);
        catalogueUsers = getCatalogueUserEmails(catalogues);
        lock.unlock();
        logger.debug("Update Authorities took {} ms", (System.nanoTime() - time) / 1000000);
    }

    @Override
    public Set<GrantedAuthority> getAuthorities(String email) {
        long time = System.nanoTime();
        updateAuthorities();

        Set<GrantedAuthority> authorities = new HashSet<>();

        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        try {
            if (!lock.tryLock(10, TimeUnit.SECONDS)) {
                throw new InsufficientAuthenticationException("Could not authorize user. Try again...");
            }
            if (providerUsers.contains(email.toLowerCase())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_PROVIDER"));
            }
            if (catalogueUsers.contains(email.toLowerCase())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_CATALOGUE_ADMIN"));
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        if (adminsAndEpot.containsKey(email.toLowerCase())) {
            authorities.addAll(adminsAndEpot.get(email.toLowerCase()));
        }
        logger.debug("Get Authorities took {} ms", (System.nanoTime() - time) / 1000000);
        return authorities;
    }

    private Set<String> getProviderUserEmails(List<ProviderBundle> providerBundles) {
        return providerBundles
                .stream()
                .flatMap(p -> (p.getProvider().getUsers() != null ? p.getProvider().getUsers() : new ArrayList<User>())
                        .stream()
                        .filter(Objects::nonNull)
                        .map(User::getEmail)
                        .filter(Objects::nonNull)
                        .map(String::toLowerCase))
                .filter(u -> u != null && !Objects.equals("", u))
                .collect(Collectors.toSet());
    }

    private Set<String> getCatalogueUserEmails(List<CatalogueBundle> catalogueBundles) {
        return catalogueBundles
                .stream()
                .flatMap(p -> (p.getCatalogue().getUsers() != null ? p.getCatalogue().getUsers() : new ArrayList<User>())
                        .stream()
                        .filter(Objects::nonNull)
                        .map(User::getEmail)
                        .map(String::toLowerCase))
                .collect(Collectors.toSet());
    }

    private void mergeRoles(Map<String, Set<SimpleGrantedAuthority>> roles, Map<String, SimpleGrantedAuthority> newRoles) {
        for (Map.Entry<String, SimpleGrantedAuthority> role : newRoles.entrySet()) {
            roles.putIfAbsent(role.getKey(), new HashSet<>());
            roles.get(role.getKey()).add(role.getValue());
        }
    }

    @EventListener
    public void onPropertyChange(PropertyChangeEvent event) {
        if ("catalogue.admins".equals(event.getPropertyName())
                || "catalogue.onboarding-team".equals(event.getPropertyName())) {
            updateAdminsAndEpot();
        }
    }

    private void updateAdminsAndEpot() {
        adminsAndEpot.clear();
        for (String admin : catalogueProperties.getAdmins()) {
            adminsAndEpot.put(admin, Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }
        for (String epot : catalogueProperties.getOnboardingTeam()) {
            adminsAndEpot.put(epot, Set.of(new SimpleGrantedAuthority("ROLE_EPOT")));
        }
    }
}
