package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.resourcecatalogue.domain.MonitoringBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.MonitoringService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Profile("crud")
@RestController
@RequestMapping(path = "monitorings")
@Tag(name = "monitorings")
public class MonitoringCrudController extends ResourceCrudController<MonitoringBundle> {

    private static final Logger logger = LogManager.getLogger(MonitoringCrudController.class);
    private final MonitoringService monitoringService;

    public MonitoringCrudController(MonitoringService monitoringService) {
        super(monitoringService);
        this.monitoringService = monitoringService;
    }

    @PostMapping(path = "/bulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<MonitoringBundle> bundles, @Parameter(hidden = true) Authentication auth) {
        monitoringService.addBulk(bundles, auth);
    }
}
