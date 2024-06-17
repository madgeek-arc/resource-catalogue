package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.DraftResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final DraftResourceService<TrainingResourceBundle> draftTrainingResourceService;

    TrainingResourceCrudController(TrainingResourceService trainingResourceService,
                                   DraftResourceService<TrainingResourceBundle> draftTrainingResourceService) {
        super(trainingResourceService);
        this.trainingResourceService = trainingResourceService;
        this.draftTrainingResourceService = draftTrainingResourceService;
    }

    @PostMapping(path = "/bulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<TrainingResourceBundle> bundles, @Parameter(hidden = true) Authentication auth) {
        trainingResourceService.addBulk(bundles, auth);
    }

    @Override
    @PostMapping()
    public ResponseEntity<TrainingResourceBundle> add(@RequestBody TrainingResourceBundle trainingResourceBundle,
                                                      @Parameter(hidden = true) Authentication auth) {
        if (trainingResourceBundle.isDraft()) {
            return new ResponseEntity<>(draftTrainingResourceService.save(trainingResourceBundle), HttpStatus.CREATED);
        }
        return super.add(trainingResourceBundle, auth);
    }
}
