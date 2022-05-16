package eu.einfracentral.service;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.User;
import org.springframework.security.core.Authentication;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface SecurityService {

    Authentication getAdminAccess();

    String getRoleName(Authentication authentication);

    boolean hasRole(Authentication auth, String role);

    boolean isProviderAdmin(Authentication auth, @NotNull String providerId);

    boolean isProviderAdmin(Authentication auth, @NotNull String providerId, boolean noThrow);

    boolean userIsProviderAdmin(@NotNull User user, @NotNull String providerId);

    boolean isCatalogueAdmin(Authentication auth, @NotNull String catalogueId);

    boolean isCatalogueAdmin(Authentication auth, @NotNull String catalogueId, boolean noThrow);

    boolean userIsCatalogueAdmin(@NotNull User user, @NotNull String catalogueId);

    boolean isServiceProviderAdmin(Authentication auth, String serviceId);

    boolean isServiceProviderAdmin(Authentication auth, String serviceId, boolean noThrow);

    boolean isServiceProviderAdmin(Authentication auth, eu.einfracentral.domain.Service service);

    boolean isServiceProviderAdmin(Authentication auth, eu.einfracentral.domain.Service service, boolean noThrow);

    boolean isServiceProviderAdmin(Authentication auth, eu.einfracentral.domain.InfraService infraService);

    boolean isServiceProviderAdmin(Authentication auth, eu.einfracentral.domain.InfraService infraService, boolean noThrow);

    boolean userIsServiceProviderAdmin(User user, eu.einfracentral.domain.Service service);

    boolean userIsServiceProviderAdmin(User user, InfraService infraService);

    boolean userIsServiceProviderAdmin(User user, String serviceId);

    boolean userIsServiceProviderAdmin(User user, List<String> serviceId);

    boolean isHelpdeskProviderAdmin(Authentication auth, String helpdeskId);

    boolean isMonitoringProviderAdmin(Authentication auth, String monitoringId);

    boolean providerCanAddServices(Authentication auth, InfraService service);

    boolean providerIsActiveAndUserIsAdmin(Authentication auth, String serviceId);

    boolean serviceIsActive(String serviceId);

    boolean serviceIsActive(String serviceId, String version);
}
