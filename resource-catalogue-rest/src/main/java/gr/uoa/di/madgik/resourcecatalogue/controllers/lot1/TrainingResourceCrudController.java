package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.resourcecatalogue.controllers.registry.ResourceController;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Profile("crud")
@RestController
@RequestMapping(path = "training-resources")
@Tag(name = "training resource")
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
