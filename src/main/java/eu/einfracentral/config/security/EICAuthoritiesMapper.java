package eu.einfracentral.config.security;

import com.nimbusds.jwt.JWT;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.User;
import eu.einfracentral.registry.manager.ProviderManager;
import eu.openminted.registry.core.domain.FacetFilter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mitre.openid.connect.client.OIDCAuthoritiesMapper;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
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

    final private static Logger logger = LogManager.getLogger(EICAuthoritiesMapper.class);
    final private static String ROLE_CLAIMS = "edu_person_entitlements";
    private Map<String, SimpleGrantedAuthority> userRolesMap;

    private ProviderManager providerManager;

    @Autowired
    public EICAuthoritiesMapper(@Value("${eic.admins}") String admins, ProviderManager manager) throws Exception {
        this.providerManager = manager;
        if (admins == null) {
            throw new Exception("No Admins Provided");
        }
        userRolesMap = new HashMap<>();

        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        Optional<List<Provider>> providers = Optional.of(providerManager.getAll(ff, null).getResults());
        userRolesMap = providers.get()
                .stream()
                .distinct()
                .filter(Provider::getActive)
                .flatMap((Function<Provider, Stream<String>>) provider -> provider.getUsers()
                        .stream()
                        .filter(Objects::nonNull)
                        .map(User::getEmail))
                .distinct()
                .collect(Collectors
                        .toMap(Function.identity(), (a) -> new SimpleGrantedAuthority("ROLE_PROVIDER")));

        userRolesMap.putAll(Arrays.stream(admins.split(","))
                .collect(Collectors.toMap(
                        Function.identity(),
                        (a) -> new SimpleGrantedAuthority("ROLE_ADMIN"))
                ));
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(JWT idToken, UserInfo userInfo) {
        Set<GrantedAuthority> out = new HashSet<>();
        out.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (userInfo.getEmail() != null) {
            String email = userInfo.getEmail();
            SimpleGrantedAuthority authority = userRolesMap.get(email);
            if (authority != null) {
                logger.info(String.format("%s mapped as %s", email, authority.getAuthority()));
                out.add(authority);
            }
        }
        return out;
    }
}
