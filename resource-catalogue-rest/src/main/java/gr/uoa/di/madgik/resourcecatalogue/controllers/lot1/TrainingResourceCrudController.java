package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
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
@RequestMapping(path = "training-resources")
@Tag(name = "training resources")
public class TrainingResourceCrudController extends ResourceCrudController<TrainingResourceBundle> {

    private static final Logger logger = LogManager.getLogger(TrainingResourceCrudController.class.getName());
    private final TrainingResourceService trainingResourceService;

    TrainingResourceCrudController(TrainingResourceService trainingResourceService) {
        super(trainingResourceService);
        this.trainingResourceService = trainingResourceService;
    }

    @PostMapping(path = "/bulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<TrainingResourceBundle> bundles, @Parameter(hidden = true) Authentication auth) {
        trainingResourceService.addBulk(bundles, auth);
    }
}
