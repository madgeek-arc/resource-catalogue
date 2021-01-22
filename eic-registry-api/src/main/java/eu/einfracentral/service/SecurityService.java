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

    boolean isProviderAdmin(Authentication auth, @NotNull String providerId, boolean noThrow);

    boolean userIsProviderAdmin(@NotNull User user, @NotNull String providerId);

    boolean isServiceProviderAdmin(Authentication auth, String serviceId);

    boolean isServiceProviderAdmin(Authentication auth, String serviceId, boolean noThrow);

    boolean isServiceProviderAdmin(Authentication auth, eu.einfracentral.domain.Service service);

    boolean isServiceProviderAdmin(Authentication auth, eu.einfracentral.domain.Service service, boolean noThrow);

    boolean isServiceProviderAdmin(Authentication auth, eu.einfracentral.domain.InfraService infraService);

    boolean isServiceProviderAdmin(Authentication auth, eu.einfracentral.domain.InfraService infraService, boolean noThrow);

    boolean userIsServiceProviderAdmin(User user, Map<String, JsonNode> json) throws JsonProcessingException;

    boolean userIsServiceProviderAdmin(User user, eu.einfracentral.domain.Service service);

    boolean userIsServiceProviderAdmin(User user, InfraService infraService);

    boolean userIsServiceProviderAdmin(User user, String serviceId);

    boolean providerCanAddServices(Authentication auth, InfraService service);

    boolean providerCanAddServices(Authentication auth, Map<String, JsonNode> json) throws JsonProcessingException;

    @Deprecated
    boolean providerIsActive(String providerId);

    boolean providerIsActiveAndUserIsAdmin(Authentication auth, String serviceId);

    boolean serviceIsActive(String serviceId);

    boolean serviceIsActive(String serviceId, String version);
}
