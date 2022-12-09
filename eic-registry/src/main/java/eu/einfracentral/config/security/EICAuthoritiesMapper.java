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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
public class EICAuthoritiesMapper implements OIDCAuthoritiesMapper, AuthoritiesMapper {

    private static final Logger logger = LogManager.getLogger(EICAuthoritiesMapper.class);
    private Map<String, Set<SimpleGrantedAuthority>> userRolesMap;
    private final Map<String, Set<SimpleGrantedAuthority>> adminsAndEpots = new HashMap<>();
    private final String admins;
    private final String epotAdmins;
    private final int maxQuantity;

    private final ProviderService<ProviderBundle, Authentication> providerService;

    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;
    private final PendingResourceService<ProviderBundle> pendingProviderService;
    private final SecurityService securityService;

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
        mapAuthorities(adminsAndEpots);
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(JWT idToken, UserInfo userInfo) {
        Set<GrantedAuthority> out = new HashSet<>();
        if (idToken == null || userInfo == null) {
            throw new UnauthorizedUserException("token is not valid or it has expired");
        }

        Set<SimpleGrantedAuthority> authorities;
        out.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (userRolesMap.get(userInfo.getEmail()) != null){
            logger.info(String.format("Roles by email: [%s]", userRolesMap.get(userInfo.getEmail()).stream().map(Objects::toString).collect(Collectors.joining(","))));
        }
        if (userRolesMap.get(userInfo.getSub()) != null) {
            logger.info(String.format("Roles by sub: [%s]", userRolesMap.get(userInfo.getSub()).stream().map(Objects::toString).collect(Collectors.joining(","))));
        }
        if (userRolesMap.get(userInfo.getSub()) != null) {
            if (userRolesMap.get(userInfo.getEmail()) != null) { // if there is also an email entry then user must be admin
                authorities = userRolesMap.get(userInfo.getEmail().toLowerCase());
            } else {
                authorities = userRolesMap.get(userInfo.getSub());
            }
        } else {
            authorities = userRolesMap.get(userInfo.getEmail().toLowerCase());
        }
        if (authorities != null) {
            out.addAll(authorities);
        }

        String authoritiesString = out.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        logger.info("User '{}' with email '{}' mapped as '{}'", userInfo.getSub(), userInfo.getEmail(), authoritiesString);
        return out;
    }

    @Override
    public boolean isAdmin(String email) {
        if (!userRolesMap.containsKey(email)) {
            return false;
        } else {
            return userRolesMap.get(email)
                    .stream()
                    .anyMatch(simpleGrantedAuthority -> simpleGrantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        }
    }

    @Override
    public boolean isEPOT(String email) {
        if (!userRolesMap.containsKey(email)) {
            return false;
        } else {
            return userRolesMap.get(email).stream()
                    .anyMatch(simpleGrantedAuthority -> simpleGrantedAuthority.getAuthority().equals("ROLE_EPOT"));
        }
    }

    @Override
    public void updateAuthorities() {
        logger.info("Updating authorities map");
        mapAuthorities(adminsAndEpots);
    }

    private void mapAuthorities(Map<String, Set<SimpleGrantedAuthority>> adminsAndEpots) {
        logger.info("Entered");
        long time = System.nanoTime();
        userRolesMap = new HashMap<>(adminsAndEpots);
        FacetFilter ff = new FacetFilter();
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        try {
            List<ProviderBundle> providers = providerService.getAll(ff, securityService.getAdminAccess()).getResults();
//            providers.addAll(pendingProviderService.getAll(ff, null).getResults()); // TODO: try/catch or something
            List<User> users = getProviderUsers(providers);
            mergeRoles(userRolesMap, createUserRoles(users, "ROLE_PROVIDER"));

        } catch (Exception e) {
            logger.warn("There are no Provider entries in DB");
        }

        try {
            List<CatalogueBundle> catalogueAdmins = catalogueService.getAll(ff, securityService.getAdminAccess()).getResults();
            List<User> users = getCatalogueUsers(catalogueAdmins);
            mergeRoles(userRolesMap, createUserRoles(users, "ROLE_CATALOGUE_ADMIN"));
        } catch (Exception e) {
            logger.warn("There are no Catalogue entries in DB");
        }
        logger.info("Map Authorities took {} ms", (System.nanoTime() - time) / 1000);
    }

    private List<User> getProviderUsers(List<ProviderBundle> providers) {
        return providers
                .stream()
                .distinct()
                .map(providerBundle -> {
                    if (providerBundle.getProvider() != null && providerBundle.getProvider().getUsers() != null) {
                        return providerBundle.getProvider();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .map(Provider::getUsers)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<User> getCatalogueUsers(List<CatalogueBundle> providers) {
        return providers
                .stream()
                .distinct()
                .map(providerBundle -> {
                    if (providerBundle.getCatalogue() != null && providerBundle.getCatalogue().getUsers() != null) {
                        return providerBundle.getCatalogue();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .map(Catalogue::getUsers)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private Map<String, SimpleGrantedAuthority> createUserRoles(List<User> users, String role) {
        return users
                .stream()
                .map(user -> {
                    if (user.getId() != null && !"".equals(user.getId())) {
                        return user.getId();
                    }
                    return user.getEmail().toLowerCase();
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors
                        .toMap(Function.identity(), a -> new SimpleGrantedAuthority(role)));
    }

//    private void mergeRoles(Map<String, Set<SimpleGrantedAuthority>> roles, Map<String, SimpleGrantedAuthority> newRoles) {
//        for (Map.Entry<String, SimpleGrantedAuthority> role : newRoles.entrySet()) {
//            roles.putIfAbsent(role.getKey(), new HashSet<>());
//            roles.get(role.getKey()).add(role.getValue());
////            if (!userRolesMap.containsKey(role.getKey())){
////            }
////            if (userRolesMap.containsKey(role.getKey()) && !userRolesMap.get(role.getKey()).contains(role.getValue())) {
////                userRolesMap.get(role.getKey()).add(role.getValue());
////            }
//        }
//    }

    private void mergeRoles(Map<String, Set<SimpleGrantedAuthority>> roles, Map<String, SimpleGrantedAuthority> newRoles) {
        for (Map.Entry<String, SimpleGrantedAuthority> role : newRoles.entrySet()) {
            roles.putIfAbsent(role.getKey(), new HashSet<>());
            roles.get(role.getKey()).add(role.getValue());
        }
    }
}
