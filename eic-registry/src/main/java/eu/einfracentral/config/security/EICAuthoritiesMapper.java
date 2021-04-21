package eu.einfracentral.config.security;

import com.nimbusds.jwt.JWT;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.registry.service.ProviderService;
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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
public class EICAuthoritiesMapper implements OIDCAuthoritiesMapper {

    private static final Logger logger = LogManager.getLogger(EICAuthoritiesMapper.class);
    private Map<String, SimpleGrantedAuthority> userRolesMap;
    private String admins;

    private ProviderService<ProviderBundle, Authentication> providerService;
    private PendingResourceService<ProviderBundle> pendingProviderService;
    private SecurityService securityService;

    @Autowired
    public EICAuthoritiesMapper(@Value("${project.admins}") String admins,
                                ProviderService<ProviderBundle, Authentication> manager,
                                PendingResourceService<ProviderBundle> pendingProviderService,
                                SecurityService securityService) {
        this.providerService = manager;
        this.pendingProviderService = pendingProviderService;
        this.securityService = securityService;
        if (admins == null) {
            throw new ServiceException("No Admins Provided");
        }
        this.admins = admins;
        mapAuthorities(admins);
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(JWT idToken, UserInfo userInfo) {
        Set<GrantedAuthority> out = new HashSet<>();
        if (idToken == null || userInfo == null) {
            throw new UnauthorizedUserException("token is not valid or it has expired");
        }

        SimpleGrantedAuthority authority;
        out.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (userRolesMap.get(userInfo.getSub()) != null) {
            if (userRolesMap.get(userInfo.getEmail()) != null) { // if there is also an email entry then user must be admin
                authority = userRolesMap.get(userInfo.getEmail().toLowerCase());
            } else {
                authority = userRolesMap.get(userInfo.getSub());
            }
        } else {
            authority = userRolesMap.get(userInfo.getEmail().toLowerCase());
        }
        if (authority != null) {
            out.add(authority);
        }

        String authoritiesString = out.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        logger.info("User '{}' with email '{}' mapped as '{}'", userInfo.getSub(), userInfo.getEmail(), authoritiesString);
        return out;
    }

    public void updateAuthorities() {
        logger.info("Updating authorities map");
        mapAuthorities(admins);
    }

    private void mapAuthorities(String admins) {
        userRolesMap = new HashMap<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        try {
            List<ProviderBundle> providers = providerService.getAll(ff, securityService.getAdminAccess()).getResults();
            providers.addAll(pendingProviderService.getAll(ff, null).getResults());
            userRolesMap = providers
                    .stream()
                    .distinct()
                    .map(providerBundle -> {
                        if (providerBundle.getProvider() != null && providerBundle.getProvider().getUsers() != null) {
                            return providerBundle.getProvider();
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .flatMap((Function<Provider, Stream<String>>) provider -> provider.getUsers()
                            .stream()
                            .filter(Objects::nonNull)
                            .map(u -> {
                                if (u.getId() != null && !"".equals(u.getId())) {
                                    return u.getId();
                                }
                                return u.getEmail().toLowerCase();
                            }))
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors
                            .toMap(Function.identity(), a -> new SimpleGrantedAuthority("ROLE_PROVIDER")));
        } catch (Exception e) {
            logger.warn("There are no Provider entries in DB");
        }

        userRolesMap.putAll(Arrays.stream(admins.replace(" ", "").split(","))
                .map(String::toLowerCase)
                .collect(Collectors.toMap(
                        Function.identity(),
                        a -> new SimpleGrantedAuthority("ROLE_ADMIN"))
                ));
    }
}
