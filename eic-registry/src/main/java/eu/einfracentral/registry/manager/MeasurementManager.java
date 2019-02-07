package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Measurement;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.MeasurementService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@Component
public class MeasurementManager extends ResourceManager<Measurement> implements MeasurementService {

    private VocabularyManager vocabularyManager;

    public MeasurementManager() {
        super(Measurement.class);
    }

    public MeasurementManager(VocabularyManager vocabularyManager) {
        super(Measurement.class);
        this.vocabularyManager = vocabularyManager;
    }

    @Override
    public String getResourceType() {
        return "measurement";
    }

    @Override
    public Measurement add(Measurement measurement, Authentication auth) {
        super.add(measurement, auth);
        return measurement;
    }

//    public Measurement validate(Measurement measurement){
//        Vocabulary vocabulary = vocabularyManager.get
//
//        for (int i=0; i<measurement.getLocations().size(); i++){
//            if ()
//        }
//
//    }


}