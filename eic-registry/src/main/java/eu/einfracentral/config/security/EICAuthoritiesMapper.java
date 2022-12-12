package eu.einfracentral.config.security;

import com.nimbusds.jwt.JWT;
import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.CatalogueService;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.AuthoritiesMapper;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.openid.connect.client.OIDCAuthoritiesMapper;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
public class EICAuthoritiesMapper implements OIDCAuthoritiesMapper, AuthoritiesMapper {

    private static final Logger logger = LogManager.getLogger(EICAuthoritiesMapper.class);
    private Set<String> providerUsers = new HashSet<>();
    private Set<String> catalogueUsers = new HashSet<>();
    private final Map<String, Set<SimpleGrantedAuthority>> adminsAndEpots = new HashMap<>();
    private final String admins;
    private final String epotAdmins;
    private final int maxQuantity;

    private final ProviderService<ProviderBundle, Authentication> providerService;

    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;
    private final PendingResourceService<ProviderBundle> pendingProviderService;
    private final SecurityService securityService;

    private final ReentrantLock lock = new ReentrantLock();

    @Autowired
    public EICAuthoritiesMapper(@Value("${project.admins}") String admins,
                                @Value("${project.admins.epot}") String epotAdmins,
                                @Value("${elastic.index.max_result_window:10000}") int maxQuantity,
                                ProviderService<ProviderBundle, Authentication> manager,
                                CatalogueService<CatalogueBundle, Authentication> catalogueService,
                                PendingResourceService<ProviderBundle> pendingProviderService,
                                SecurityService securityService) {
        this.providerService = manager;
        this.catalogueService = catalogueService;
        this.pendingProviderService = pendingProviderService;
        this.securityService = securityService;
        this.maxQuantity = maxQuantity;
        if (admins == null) {
            throw new ServiceException("No Admins Provided");
        }
        this.admins = admins;
        this.epotAdmins = epotAdmins;
    }

    @PostConstruct
    void createAdminsOnStartup() {
        mergeRoles(adminsAndEpots, Arrays.stream(epotAdmins.replace(" ", "").split(","))
                .map(String::toLowerCase)
                .collect(Collectors.toMap(
                        Function.identity(),
                        a -> new SimpleGrantedAuthority("ROLE_EPOT"))
                ));

        mergeRoles(adminsAndEpots, Arrays.stream(admins.replace(" ", "").split(","))
                .map(String::toLowerCase)
                .collect(Collectors.toMap(
                        Function.identity(),
                        a -> new SimpleGrantedAuthority("ROLE_ADMIN"))
                ));
        updateAuthorities();
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(JWT idToken, UserInfo userInfo) {
        Set<GrantedAuthority> out = new HashSet<>();
        if (idToken == null || userInfo == null) {
            throw new UnauthorizedUserException("token is not valid or it has expired");
        }

        out.add(new SimpleGrantedAuthority("ROLE_USER"));
        out.addAll(getAuthorities(userInfo.getEmail()));
        String authoritiesString = out.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        logger.info("User '{}' with email '{}' mapped as '{}'", userInfo.getSub(), userInfo.getEmail(), authoritiesString);
        return out;
    }

    @Override
    public boolean isAdmin(String email) {
        if (!adminsAndEpots.containsKey(email)) {
            return false;
        } else {
            return adminsAndEpots.get(email)
                    .stream()
                    .anyMatch(simpleGrantedAuthority -> simpleGrantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        }
    }

    @Override
    public boolean isEPOT(String email) {
        if (!adminsAndEpots.containsKey(email)) {
            return false;
        } else {
            return adminsAndEpots.get(email).stream()
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

        try {
            providers.addAll(pendingProviderService.getAll(ff, securityService.getAdminAccess()).getResults());
        } catch (Exception e) {
            logger.warn("There are no Pending Provider entries in DB");
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

    private Set<SimpleGrantedAuthority> getAuthorities(String email) {
        long time = System.nanoTime();
        updateAuthorities();

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        try {
            if (!lock.tryLock(10, TimeUnit.SECONDS)) {
                throw new UnauthorizedUserException("Could not authorize user. Try again...");
            }
            if (providerUsers.contains(email.toLowerCase())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_PROVIDER"));
            }
            if (catalogueUsers.contains(email.toLowerCase())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_CATALOGUE_ADMIN"));
            }
        } catch (InterruptedException e) {
            logger.error(e);
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        if (adminsAndEpots.containsKey(email.toLowerCase())) {
            authorities.addAll(adminsAndEpots.get(email.toLowerCase()));
        }
        logger.debug("Get Authorities took {} ms", (System.nanoTime() - time) / 1000000);
        return authorities;
    }

    private Set<String> getProviderUserEmails(List<ProviderBundle> providerBundles) {
        return providerBundles
                .stream()
                .flatMap(p -> p.getProvider().getUsers()
                        .stream()
                        .map(User::getEmail)
                        .map(String::toLowerCase))
                .filter(u -> u != null && !"".equals(u))
                .collect(Collectors.toSet());
    }

    private Set<String> getCatalogueUserEmails(List<CatalogueBundle> catalogueBundles) {
        return catalogueBundles
                .stream()
                .flatMap(p -> p.getCatalogue().getUsers()
                        .stream()
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
}
