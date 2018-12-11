package eu.einfracentral.config.security;

import com.nimbusds.jwt.JWT;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.User;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.FacetFilter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mitre.openid.connect.client.OIDCAuthoritiesMapper;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    private ProviderService<Provider, Authentication> providerService;
    private SecurityService securityService;

    @Autowired
    public EICAuthoritiesMapper(@Value("${eic.admins}") String admins, ProviderService<Provider, Authentication> manager,
                                SecurityService securityService) {
        this.providerService = manager;
        this.securityService = securityService;
        if (admins == null) {
            throw new RuntimeException("No Admins Provided");
        }
        userRolesMap = new HashMap<>();

        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        try {
            List<Provider> providers = providerService.getAll(ff, securityService.getAdminAccess()).getResults();
            if (providers != null) {
                userRolesMap = providers
                        .stream()
                        .distinct()
                        .flatMap((Function<Provider, Stream<String>>) provider -> provider.getUsers()
                                .stream()
                                .filter(Objects::nonNull)
                                .map(u -> {
                                    if (u.getId() != null) {
                                        return u.getId();
                                    }
                                    return u.getEmail();
                                }))
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors
                                .toMap(Function.identity(), a -> new SimpleGrantedAuthority("ROLE_PROVIDER")));
            }
        } catch (Exception e) {
            logger.warn("There are no Provider entries in DB");
        }

        userRolesMap.putAll(Arrays.stream(admins.replaceAll(" ", "").split(","))
                .collect(Collectors.toMap(
                        Function.identity(),
                        a -> new SimpleGrantedAuthority("ROLE_ADMIN"))
                ));
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(JWT idToken, UserInfo userInfo) {
        Set<GrantedAuthority> out = new HashSet<>();
        SimpleGrantedAuthority authority;
        out.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (userRolesMap.get(userInfo.getSub()) != null) {
            if (userRolesMap.get(userInfo.getEmail()) != null) { // if there is also an email entry then user must be admin
                authority = userRolesMap.get(userInfo.getEmail());
            } else {
                authority = userRolesMap.get(userInfo.getSub());
            }
        } else {
            authority = userRolesMap.get(userInfo.getEmail());
        }
        if (authority != null) {
            logger.info(String.format("User %s with email %s mapped as %s", userInfo.getSub(), userInfo.getEmail(), authority.getAuthority()));
            out.add(authority);
        }
        return out;
    }

    @JmsListener(destination = "eicRoleMapper")
    public void receiveMessage(Provider provider) {
        logger.info("mapping new providers");
        if (userRolesMap == null) {
            userRolesMap = new HashMap<>();
        }
        for (User user : provider.getUsers()) {
            if (user.getEmail() != null) {
                userRolesMap.putIfAbsent(user.getEmail(), new SimpleGrantedAuthority("ROLE_PROVIDER"));
            } else {
                userRolesMap.putIfAbsent(user.getId(), new SimpleGrantedAuthority("ROLE_PROVIDER"));
            }
        }
    }
}
