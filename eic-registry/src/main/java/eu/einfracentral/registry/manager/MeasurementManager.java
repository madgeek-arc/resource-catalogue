package eu.einfracentral.registry.manager;

import com.google.gson.Gson;
import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.MeasurementService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.SynchronizerService;
import eu.einfracentral.utils.TextUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class MeasurementManager extends ResourceManager<Measurement> implements MeasurementService<Measurement, Authentication> {

    private static final Logger logger = LogManager.getLogger(MeasurementManager.class);
    private IndicatorManager indicatorManager;
    private VocabularyService vocabularyService;
    private InfraServiceService<InfraService, InfraService> infraService;
    private SynchronizerService synchronizerService;

    @Autowired
    public MeasurementManager(IndicatorManager indicatorManager, VocabularyService vocabularyService,
                              InfraServiceService<InfraService, InfraService> service,
                              SynchronizerService synchronizerService) {
        super(Measurement.class);
        this.vocabularyService = vocabularyService;
        this.infraService = service;
        this.indicatorManager = indicatorManager;
        this.synchronizerService = synchronizerService;
    }

    @Override
    public String getResourceType() {
        return "measurement";
    }

    @Override
    public Measurement add(Measurement measurement, Authentication auth) {
        if ((measurement.getId() == null) || measurement.getId().equals("")) {
            measurement.setId(UUID.randomUUID().toString());
        }
        existsIdentical(measurement);
        validate(measurement);
        super.add(measurement, auth);
        logger.debug("Adding Measurement {}", measurement);
        synchronizerService.syncAdd(measurement);
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
        logger.debug("Updating Measurement {}", measurement);
        synchronizerService.syncUpdate(measurement);
        return measurement;
    }


    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') and " +
            "@securityService.userIsServiceProviderAdmin(#auth, #serviceId)")
    public List<Measurement> updateAll(String serviceId, List<Measurement> allMeasurements, Authentication auth) {
        List<Measurement> updatedMeasurements = new ArrayList<>();
        List<Measurement> existingMeasurements = getAll(serviceId, auth).getResults();

        // set Service ID to every measurement
        allMeasurements.forEach(measurement -> measurement.setServiceId(serviceId));

        for (Measurement existingMeasurement : existingMeasurements) {
            for (int i = 0; i < allMeasurements.size(); i++) {
                if (existingMeasurement.getId().equals(allMeasurements.get(i).getId())) {
                    String existingObject = new Gson().toJson(existingMeasurement);
                    String newObject = new Gson().toJson(allMeasurements.get(i));

                    //if not identical, update
                    if (!existingObject.equals(newObject)) {
                        update(allMeasurements.get(i), auth);
                    }
                    updatedMeasurements.add(allMeasurements.get(i));
                    allMeasurements.remove(allMeasurements.get(i));
                    break;
                }
            }
        }

        //if there are new Measurements, add them
        for (Measurement measurement : allMeasurements) {
            add(measurement, auth);
            updatedMeasurements.add(measurement);
        }

        //Measurement's not given will be deleted
        for (Measurement updatedMeasurement : updatedMeasurements) {
            for (Measurement existingMeasurement : existingMeasurements) {
                if (existingMeasurement.getId().equals(updatedMeasurement.getId())) {
                    existingMeasurements.remove(existingMeasurement);
                    break;
                }
            }
        }
        for (Measurement existing : existingMeasurements) {
            delete(existing);
        }

        return updatedMeasurements;
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

    // Gets ONLY the latest Measurements
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
        logger.debug("Deleting Measurement: {}", measurement);
        synchronizerService.syncDelete(measurement);
    }

    @Override
    public Measurement validate(Measurement measurement) {

        Indicator indicator = indicatorManager.get(measurement.getIndicatorId());

        // validate measurement fields
        validateMeasurementStructure(measurement);

        // validate measurement values // TODO: move methods below to a function

        // Validates Indicator's ID
        if (indicator == null) {
            throw new ValidationException("Indicator with id: " + measurement.getIndicatorId() + " does not exist");
        }

        // Validates Service existence
        if (infraService.get(measurement.getServiceId()) == null) {
            throw new ValidationException("Service with id: " + measurement.getServiceId() + " does not exist");
        }

        // Validates that at least one of time, locations is mandatory
        if (measurement.getLocations() == null && measurement.getTime() == null) {
            throw new ValidationException("You must provide at least one of: locations, time"); // TODO: get values from type
        } else if (measurement.getLocations() == null && measurement.getTime().toString().equals("")) {
            throw new ValidationException(("Measurement's time cannot be empty"));
        } else if (measurement.getTime() == null && measurement.getLocations().isEmpty()) {
            throw new ValidationException(("Measurement's locations cannot be empty"));
        }

        if (measurement.getLocations() != null) {
            List<String> verifiedLocations = new ArrayList<>();
            Map<String, Vocabulary> placesVocabulary = vocabularyService.getByType(Vocabulary.Type.PLACE)
                    .stream()
                    .collect(Collectors.toMap(Vocabulary::getId, v -> v));
            for (String location : measurement.getLocations()) {
                if (placesVocabulary.get(location) != null && !verifiedLocations.contains(location)) {
                    verifiedLocations.add(location);
                }
            }
            if (verifiedLocations.isEmpty()) {
                throw new ValidationException("One or more of the locations given is wrong. Accepted format according to ISO 3166-1 alpha-2");
            }

            measurement.setLocations(verifiedLocations);
        }

        // Validates Measurement's time
        if (measurement.getTime() != null && "".equals(measurement.getTime().toString())) {
            throw new ValidationException("Measurement's time cannot be empty");
        }

        // Validate Measurement's value
        if (!measurement.getValueIsRange()) {
            // Validate that Measurement's value exists
            if (measurement.getValue() == null || "".equals(measurement.getValue())) {
                throw new ValidationException("Measurement's value cannot be 'null' or 'empty'");
            }
            // trim whitespace from value
            measurement.setValue(measurement.getValue().replaceAll(" ", ""));
            // Validate that rangeValue is null
            if (measurement.getRangeValue() != null) {
                throw new ValidationException("valueIsRange is set to false. You can't have a rangeValue.");
            }
        } else {
            if (measurement.getRangeValue() != null) {
                // Validate that Measurement's rangeValue.fromValue exists
                if (measurement.getRangeValue().getFromValue() == null || "".equals(measurement.getRangeValue().getFromValue())) {
                    throw new ValidationException("Measurement's fromValue cannot be 'null' or 'empty'");
                }
                // Validate that Measurement's rangeValue.toValue exists
                if (measurement.getRangeValue().getToValue() == null || "".equals(measurement.getRangeValue().getToValue())) {
                    throw new ValidationException("Measurement's toValue cannot be 'null' or 'empty'");
                }
                // Validate that value is null
                if (measurement.getValue() != null) {
                    throw new ValidationException("valueIsRange is set to true. You can't have a value.");
                }
                // Validate that fromValue > toValue
                try {
                    if (Float.parseFloat(measurement.getRangeValue().getFromValue()) >= Float.parseFloat(measurement.getRangeValue().getToValue())) {
                        throw new ValidationException("toValue can't be less than or equal to fromValue.");
                    }
                } catch (ValidationException e) {
                    logger.error(e);
                    throw new ValidationException("Value provided is not a number");
                }
            } else {
                throw new ValidationException("valueIsRange is set to true - rangeValue cannot be null.");
            }
        }

        // Validates if values provided complies with the Indicator's UnitType
        switch (Indicator.UnitType.fromString(indicator.getUnit())) {

            case NUM:
                try {
                    if (measurement.getValueIsRange()) {
                        RangeValue rangeValue = measurement.getRangeValue();
                        rangeValue.setFromValue(createNumericValue(rangeValue.getFromValue()));
                        rangeValue.setToValue(createNumericValue(rangeValue.getToValue()));
                        measurement.setRangeValue(rangeValue);
                    } else {
                        measurement.setValue(createNumericValue(measurement.getValue()));
                    }
                } catch (NumberFormatException e) {
                    throw new ValidationException("Measurement's values must be numeric");
                }
                break;

            case PCT:
                try {
                    if (measurement.getValueIsRange()) {
                        RangeValue rangeValue = measurement.getRangeValue();
                        rangeValue.setFromValue(createPercentageValue(rangeValue.getFromValue()));
                        rangeValue.setToValue(createPercentageValue(rangeValue.getToValue()));
                        measurement.setRangeValue(rangeValue);
                    } else {
                        measurement.setValue(createPercentageValue(measurement.getValue()));
                    }
                } catch (NumberFormatException e) {
                    throw new ValidationException("Measurement's values must be percentages");
                }
                break;

            case BOOL:
                if (measurement.getValueIsRange()) {
                    throw new ValidationException("Boolean UnitType cannot be a RangeValue");
                }
                if (!"true".equals(measurement.getValue()) && !"false".equals(measurement.getValue())) {
                    throw new ValidationException("Measurement's value should be either 'true' or 'false'");
                }
                break;

            default:
                // should never enter this
        }

        return measurement;
    }

    // Validates if Measurement's index complies with Indicator's structure
    public boolean validateMeasurementStructure(Measurement measurement) {
        Indicator existingIndicator = indicatorManager.get(measurement.getIndicatorId());

        if ((indicatorManager.hasTime(existingIndicator) && measurement.getTime() == null) ||
                (!indicatorManager.hasTime(existingIndicator) && measurement.getTime() != null) ||
                (indicatorManager.hasLocations(existingIndicator) && measurement.getLocations() == null) ||
                (!indicatorManager.hasLocations(existingIndicator) && measurement.getLocations() != null)) {
            throw new ValidationException("Measurement's index does not comply with the specific Indicator's structure. Please review the dimensions the specific Indicator supports.");
        }
        return true;

    }

    // Assures that no other Measurement with the same fields (IndicatorId, ServiceId, Locations, Time) exists.
    public boolean existsIdentical(Measurement measurement) {
        Paging<Measurement> existingMeasurements = getAll(measurement.getIndicatorId(), measurement.getServiceId(), null);
        for (Measurement entry : existingMeasurements.getResults()) {
            if (entry.getLocations() == null && measurement.getLocations() == null) {
                if (entry.getTime().equals(measurement.getTime())) {
                    throw new ValidationException("Measurement with IndicatorId " + measurement.getIndicatorId() + " and ServiceId " + measurement.getServiceId() +
                            " for the specific timestamp already exists!");
                }
            } else if (entry.getTime() == null && measurement.getTime() == null) {
                if (entry.getLocations().equals(measurement.getLocations())) {
                    throw new ValidationException("Measurement with IndicatorId " + measurement.getIndicatorId() + " and ServiceId " + measurement.getServiceId() +
                            " for the specific location-s already exists!");
                }
            } else if (entry.getTime() != null && entry.getLocations() != null && measurement.getLocations() != null && measurement.getTime() != null) {
                if (entry.getTime().equals(measurement.getTime()) && entry.getLocations().equals(measurement.getLocations())) {
                    throw new ValidationException("Measurement with IndicatorId " + measurement.getIndicatorId() + " and ServiceId " + measurement.getServiceId() +
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

    private String createPercentageValue(String value) {
        value = TextUtils.trimWhitespace(value);
        float floatValue;
        floatValue = Float.parseFloat(value);
        if (floatValue < 0 || floatValue > 100) {
            throw new ValidationException("Please enter a valid number in the range [0,100]");
        }
        return TextUtils.formatArithmeticPrecision(Float.toString(floatValue), 4); // enable if you want to force max decimal digits
    }

    private String createNumericValue(String value) {
        value = TextUtils.trimWhitespace(value);
        float floatValue = Float.parseFloat(value);
        if (floatValue < 0) {
            throw new ValidationException("Measurement's value cannot be negative");
        }
        return TextUtils.formatArithmeticPrecision(Float.toString(floatValue), 4); // enable if you want to force max decimal digits
    }

}
