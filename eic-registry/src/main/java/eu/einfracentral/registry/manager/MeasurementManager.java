package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.MeasurementService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class MeasurementManager extends ResourceManager<Measurement> implements MeasurementService<Measurement, Authentication> {

    private IndicatorManager indicatorManager;
    private VocabularyManager vocabularyManager;
    private InfraServiceService<InfraService, InfraService> infraService;

    @Autowired
    public MeasurementManager(IndicatorManager indicatorManager, VocabularyManager vocabularyManager,
                              InfraServiceService<InfraService, InfraService> service) {
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
        measurement.setId(UUID.randomUUID().toString());
        validateMeasurementStructure(measurement);
        existsIdentical(measurement);
        validate(measurement);
        super.add(measurement, auth);
        return measurement;
    }

    @Override
    public Measurement update(Measurement measurement, Authentication auth) {
        validate(measurement);
        Measurement previous = get(measurement.getId());
        if (!previous.getServiceId().equals(measurement.getServiceId())) {
            throw new ValidationException("You cannot change the Service id of the measurement");
        }
        if (!previous.getIndicatorId().equals(measurement.getIndicatorId())) {
            throw new ValidationException("You cannot change the Indicator of the measurement");
        }
        super.update(measurement, auth);
        return measurement;
    }

    @Override
    public Paging<Measurement> getAll(String serviceId, Authentication authentication) {
        Paging<Resource> measurementResources = searchService.cqlQuery(String.format("service=\"%s\"", serviceId), getResourceType(),
                10000, 0, "creation_date", "DESC");
        return pagingResourceToMeasurement(measurementResources);
    }

    @Override
    public Paging<Measurement> getAll(String indicatorId, String serviceId, Authentication authentication) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("indicator", indicatorId);
        ff.addFilter("service", serviceId);
        return getAll(ff, authentication);
    }

    @Override
    public Paging<Measurement> getLatestServiceMeasurements(String serviceId, Authentication authentication) {
        Paging<Resource> measurementResources = searchService.cqlQuery(String.format("service=\"%s\"", serviceId), getResourceType(),
                10000, 0, "creation_date", "DESC");
        List<Measurement> measurements = measurementResources
                .getResults()
                .stream()
                .map(resource -> parserPool.deserialize(resource, Measurement.class))
                .collect(Collectors.toList());
        Map<String, Measurement> measurementMap = new HashMap<>();
        for (Measurement measurement : measurements) {
            String id = measurement.getIndicatorId(); // create an id using indicator, locations and time fields
            if (measurement.getLocations() != null) {
                id += String.join("", measurement.getLocations());
            }
            if (measurement.getTime() != null) {
                id += measurement.getTime();
            }
            measurementMap.putIfAbsent(id, measurement); // put only the first occurred item with that id
        }
        measurements = new ArrayList<>(measurementMap.values());
        return new Paging<>(measurements.size(), 0, measurements.size(), measurements, null);
    }

    @Override
    public void delete(Measurement measurement) {
        super.delete(measurement);
    }

    @Override
    public Measurement validate(Measurement measurement) {

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

    // Validates if Measurement's index complies with Indicator's structure
    public boolean validateMeasurementStructure(Measurement measurement){
        Indicator existingIndicator = indicatorManager.get(measurement.getIndicatorId());

        for (String dimension : existingIndicator.getDimensions()){
            if ((Indicator.DimensionType.fromString(dimension) == Indicator.DimensionType.LOCATION && measurement.getLocations() == null) ||
                (Indicator.DimensionType.fromString(dimension) != Indicator.DimensionType.LOCATION && measurement.getLocations() != null) ||
                (Indicator.DimensionType.fromString(dimension) == Indicator.DimensionType.TIME && measurement.getTime() == null) ||
                (Indicator.DimensionType.fromString(dimension) != Indicator.DimensionType.TIME && measurement.getTime() != null)){
                throw new ValidationException("Measurement's index does not comply with the specific Indicator's structure. Please review the dimensions the specific Indicator supports.");
            }
        }

        return true;
    }

    // Assures that no other Measurement with the same fields (IndicatorId, ServiceId, Locations, Time) exists.
    public boolean existsIdentical(Measurement measurement){
        Paging<Measurement> existingMeasurements = getAll(measurement.getIndicatorId(), measurement.getServiceId(), null);
        for (Measurement entry : existingMeasurements.getResults()) {
            if (entry.getLocations() == null && measurement.getLocations() == null){
                if(entry.getTime().equals(measurement.getTime())){
                    throw new ValidationException("Measurement with IndicatorId " +measurement.getIndicatorId()+ " and ServiceId " +measurement.getServiceId()+
                            " for the specific timestamp already exists!");
                }
            } else if (entry.getTime() == null && measurement.getTime() == null){
                if(entry.getLocations().equals(measurement.getLocations())){
                    throw new ValidationException("Measurement with IndicatorId " +measurement.getIndicatorId()+ " and ServiceId " +measurement.getServiceId()+
                            " for the specific location-s already exists!");
                }
            } else if (entry.getTime() != null && entry.getLocations() != null && measurement.getLocations() != null && measurement.getTime() != null){
                if(entry.getTime().equals(measurement.getTime()) && entry.getLocations().equals(measurement.getLocations())){
                    throw new ValidationException("Measurement with IndicatorId " +measurement.getIndicatorId()+ " and ServiceId " +measurement.getServiceId()+
                            " for the specific timestamp and location-s already exists!");
                }
            }
        }
        return true;
    }


    private Paging<Measurement> pagingResourceToMeasurement(Paging<Resource> measurementResources) {
        List<Measurement> measurements = measurementResources
                .getResults()
                .stream()
                .map(resource -> parserPool.deserialize(resource, Measurement.class))
                .collect(Collectors.toList());
        return new Paging<>(measurementResources.getTotal(), measurementResources.getFrom(),
                measurementResources.getTotal(), measurements, measurementResources.getFacets());
    }

}