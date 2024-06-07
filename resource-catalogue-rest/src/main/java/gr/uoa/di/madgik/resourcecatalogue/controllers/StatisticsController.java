package gr.uoa.di.madgik.resourcecatalogue.controllers;

import gr.uoa.di.madgik.resourcecatalogue.domain.Event;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.dto.PlaceCount;
import gr.uoa.di.madgik.resourcecatalogue.dto.Value;
import gr.uoa.di.madgik.resourcecatalogue.service.StatisticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("stats")
@Tag(name = "statistics", description = "Get information about KPIs usage statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Autowired
    StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    //    @Operation(summary = "Get visits per interval for a service.")
    @GetMapping(path = "service/visits/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> visits(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.visits(id, by), HttpStatus.OK);
    }

    //    @Operation(summary = "Get addToProject per interval for a service.")
    @GetMapping(path = "service/addToProject/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> addToProject(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.addToProject(id, by), HttpStatus.OK);
    }

    //    @Operation(summary = "Get aggregate visits per interval for all services offered by a provider.")
    @GetMapping(path = "provider/visits/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> pVisits(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.providerVisits(id, by), HttpStatus.OK);
    }

    //    @Operation(summary = "Get aggregate 'addToProject per interval for all services offered by a provider.")
    @GetMapping(path = "provider/addToProject/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> pAddToProject(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.providerAddToProject(id, by), HttpStatus.OK);
    }

    //    @Operation(summary = "Get percentage of visits for all services offered by a provider.")
    @GetMapping(path = "provider/visitation/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Float>> pVisitation(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.providerVisitation(id, by), HttpStatus.OK);
    }

    // Returns the time series of the specified Event type.
    @GetMapping(path = "events", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> events(@RequestParam Event.UserActionType type, @RequestParam Date from, @RequestParam Date to, @RequestParam StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.events(type, from, to, by), HttpStatus.OK);
    }

    //    @Operation(summary = "Providing the Provider's id, get the relation between all his services and their respective countries.")
    @GetMapping(path = "provider/mapServicesToGeographicalAvailability", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MapValues>> mapServicesToGeographicalAvailability(@RequestParam(required = false) String providerId) {
        return new ResponseEntity<>(statisticsService.mapServicesToGeographicalAvailability(providerId), HttpStatus.OK);
    }

    //    @Operation(summary = "Get a relation between all Services and their Coordinating Country")
    @GetMapping(path = "provider/mapServicesToProviderCountry", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MapValues>> mapServicesToProviderCountry() {
        return new ResponseEntity<>(statisticsService.mapServicesToProviderCountry(), HttpStatus.OK);
    }

    //    @Operation(summary = "Providing the Provider's id, get the relation between all his services and a specific Vocabulary")
    @GetMapping(path = "provider/mapServicesToVocabulary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MapValues>> mapServicesToVocabulary(@RequestParam(required = false) String providerId,
                                                                   @RequestParam StatisticsService.Vocabulary vocabulary) {
        return new ResponseEntity<>(statisticsService.mapServicesToVocabulary(providerId, vocabulary), HttpStatus.OK);
    }

    //    @Operation(summary = "Get a list of places and their corresponding number of Services offered by the specified provider.")
    @GetMapping(path = "provider/servicesPerPlace", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PlaceCount>> servicesPerPlace(@RequestParam(required = false) String providerId) {
        return new ResponseEntity<>(statisticsService.servicesPerPlace(providerId), HttpStatus.OK);
    }

    //    @Operation(summary = "Get a list of places and their corresponding Services offered by the specified provider.")
    @GetMapping(path = "provider/servicesByPlace", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Value>> servicesByPlace(@RequestParam(required = false) String providerId,
                                                       @RequestParam(required = false) String place) {
        return new ResponseEntity<>(statisticsService.servicesByPlace(providerId, place), HttpStatus.OK);
    }

    //    @Operation(summary = "Get visits per interval for a datasource.")
    @GetMapping(path = "datasource/visits/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> datasourceVisits(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.visits(id, by), HttpStatus.OK);
    }

    //    @Operation(summary = "Get addToProject per interval for a datasource.")
    @GetMapping(path = "datasource/addToProject/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> datasourceAddToProject(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.addToProject(id, by), HttpStatus.OK);
    }

    @GetMapping(path = "trainingResource/visits/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> trainingResourceVisits(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.visits(id, by), HttpStatus.OK);
    }

    @GetMapping(path = "trainingResource/addToProject/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> trainingResourceAddToProject(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.addToProject(id, by), HttpStatus.OK);
    }
}
