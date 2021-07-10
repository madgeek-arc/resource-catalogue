package eu.einfracentral.registry.controller;


import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.sql.DataSource;

@RestController
@RequestMapping({"service"})
@ApiIgnore
public class ServiceControllerCompatibility extends ServiceController {

    @Autowired
    ServiceControllerCompatibility(InfraServiceService<InfraService, InfraService> service,
                                   ProviderService<ProviderBundle, Authentication> provider,
                                   DataSource dataSource) {
        super(service, provider, dataSource);
    }
}
