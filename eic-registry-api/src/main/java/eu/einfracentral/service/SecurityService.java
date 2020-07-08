package eu.einfracentral.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.User;
import org.springframework.security.core.Authentication;

import javax.validation.constraints.NotNull;
import java.util.Map;

public interface SecurityService {

    Authentication getAdminAccess();

    boolean hasRole(Authentication auth, String role);

    boolean isProviderAdmin(Authentication auth, @NotNull String providerId);

    boolean userIsProviderAdmin(@NotNull User user, @NotNull String providerId);

    boolean userIsServiceProviderAdmin(Authentication auth, Map<String, JsonNode> json) throws JsonProcessingException;

    boolean userIsServiceProviderAdmin(Authentication auth, eu.einfracentral.domain.Service service);

    boolean userIsServiceProviderAdmin(Authentication auth, InfraService infraService);

    boolean userIsServiceProviderAdmin(Authentication auth, String serviceId);

    boolean providerCanAddServices(Authentication auth, String serviceId);

    boolean providerCanAddServices(Authentication auth, InfraService service);

    boolean providerCanAddServices(Authentication auth, Map<String, JsonNode> json) throws JsonProcessingException;

    @Deprecated
    boolean providerIsActive(String providerId);

    boolean providerIsActiveAndUserIsAdmin(Authentication auth, String serviceId);

    boolean serviceIsActive(String serviceId);

    boolean serviceIsActive(String serviceId, String version);
}
