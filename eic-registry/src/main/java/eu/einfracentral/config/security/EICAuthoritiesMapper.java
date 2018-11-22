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
                                .map(User::getEmail))
                        .distinct()
                        .collect(Collectors
                                .toMap(Function.identity(), a -> new SimpleGrantedAuthority("ROLE_PROVIDER")));
            }
        } catch (Exception e) {
            logger.warn("There are no Provider entries in DB");
        }

        userRolesMap.putAll(Arrays.stream(admins.split(","))
                .collect(Collectors.toMap(
                        Function.identity(),
                        a -> new SimpleGrantedAuthority("ROLE_ADMIN"))
                ));
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(JWT idToken, UserInfo userInfo) {
        Set<GrantedAuthority> out = new HashSet<>();
        out.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (userInfo.getEmail() != null) {
            String email = userInfo.getEmail();
            SimpleGrantedAuthority authority = userRolesMap.get(email);

            // TODO: remove this
            logger.info(String.format("AAI DEBUG:  userInfo: ", userInfo.toJson().toString()));
            logger.info(String.format("AAI DEBUG:  email: ", userInfo.getEmail()));
            logger.info(String.format("AAI DEBUG:  sub: ", userInfo.getSub()));
            logger.info(String.format("AAI DEBUG:  given name: ", userInfo.getGivenName()));
            logger.info(String.format("AAI DEBUG:  family name: ", userInfo.getFamilyName()));

            if (authority != null) {
                logger.info(String.format("%s mapped as %s", email, authority.getAuthority()));
                out.add(authority);
            }
        }
        return out;
    }

    public void mapProviders(List<User> providers) {
        if (userRolesMap == null) {
            userRolesMap = new HashMap<>();
        }
        for (User user : providers) {
            userRolesMap.putIfAbsent(user.getEmail(), new SimpleGrantedAuthority("ROLE_PROVIDER"));
        }
    }
}
