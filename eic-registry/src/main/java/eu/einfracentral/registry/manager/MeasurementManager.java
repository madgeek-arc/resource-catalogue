package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.MeasurementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MeasurementManager extends ResourceManager<Measurement> implements MeasurementService<Measurement, Authentication> {

    private IndicatorManager indicatorManager;
    private VocabularyManager vocabularyManager;
    private InfraServiceService<InfraService, InfraService> infraService;

    @Autowired
    public MeasurementManager(IndicatorManager indicatorManager, VocabularyManager vocabularyManager, InfraServiceService<InfraService, InfraService> service) {
        super(Measurement.class);
        this.vocabularyManager = vocabularyManager;
        this.infraService = service;
        this.indicatorManager = indicatorManager;
    }

    @Override
    public String getResourceType() {
        return "measurement";
    }

    @Override
    public Measurement add(Measurement measurement, Authentication auth) {
        validate(measurement);
        super.add(measurement, auth);
        return measurement;
    }

    @Override
    public Measurement get(String id, Authentication auth) {
        return null;
    }

    @Override
    public void delete(Measurement measurement) {
        super.delete(measurement);
    }

    @Override
    public Measurement validate(Measurement measurement) {

        // Validates Measurement's ID
        if (measurement.getId() == null || measurement.getId().equals("")) {
            throw new ValidationException("Indicator's id cannot be 'null' or 'empty'");
        }

        // Validates Indicator's ID
        if (indicatorManager.get(measurement.getIndicatorId()) == null) {
            throw new ValidationException("Indicator with id: " + measurement.getIndicatorId() + " does not exist");
        }

        // Validates Service existence
        if (infraService.get(measurement.getServiceId()) == null) {
            throw new ValidationException("Service with id: " + measurement.getServiceId() + " does not exist");
        }

        // Validates that at least one of time, locations is mandatory
        if (measurement.getLocations() == null && measurement.getTime() == null){
            throw new ValidationException("You must provide at least one of: locations, time"); // TODO: get values from type
        } else if (measurement.getLocations() == null && measurement.getTime().toString().equals("")){
            throw new ValidationException(("Measurement's time cannot be empty"));
        } else if (measurement.getTime() == null && measurement.getLocations().isEmpty()){
            throw new ValidationException(("Measurement's locations cannot be empty"));
        }

        if (measurement.getLocations() != null){
            List<String> verifiedLocations = new ArrayList<>();
            Vocabulary placesVocabulary = vocabularyManager.get("places");
            Map<String, VocabularyEntry> places = placesVocabulary.getEntries();
            for (String location : measurement.getLocations()){
                if (places.containsKey(location) && !verifiedLocations.contains(location)){
                    verifiedLocations.add(location);
                }
            }
            if (verifiedLocations.isEmpty()){
                throw new ValidationException("One or more of the locations given is wrong. Accepted format according to ISO 3166-1 alpha-2");
            }

            measurement.setLocations(verifiedLocations);
        }

        // Validates Measurement's time
        if (measurement.getTime() != null && measurement.getTime().toString().equals("")) {
            throw new ValidationException("Measurement's time cannot be empty");
        }

        //Validates Measurement's value
        if (measurement.getValue() == null || measurement.getValue().equals("")) {
            throw new ValidationException("Measurement's value cannot be 'null' or 'empty'");
        }

        return measurement;
    }


}