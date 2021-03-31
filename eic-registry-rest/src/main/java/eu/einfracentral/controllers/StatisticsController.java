package eu.einfracentral.controllers;

import eu.einfracentral.domain.Event;
import eu.einfracentral.dto.MapValues;
import eu.einfracentral.dto.PlaceCount;
import eu.einfracentral.dto.Value;
import eu.einfracentral.service.StatisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@Api("Get information about KPIs usage statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Autowired
    StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

//    @ApiOperation(value = "Get visits per interval for a service.")
    @GetMapping(path = "service/visits/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> visits(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.visits(id, by), HttpStatus.OK);
    }

//    @ApiOperation(value = "Get favourites per interval for a service.")
    @GetMapping(path = "service/favourites/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> favourites(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.favourites(id, by), HttpStatus.OK);
    }

    //    @ApiOperation(value = "Get addToProject per interval for a service.")
    @GetMapping(path = "service/addToProject/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> addToProject(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.addToProject(id, by), HttpStatus.OK);
    }

//    @ApiOperation(value = "Get average ratings per interval for a service.")
    @GetMapping(path = "service/ratings/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Float>> ratings(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.ratings(id, by), HttpStatus.OK);
    }

//    @ApiOperation(value = "Get aggregate visits per interval for all services offered by a provider.")
    @GetMapping(path = "provider/visits/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Map<String, Integer>>> pVisits(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.providerVisits(id, by), HttpStatus.OK);
    }

//    @ApiOperation(value = "Get aggregate 'favourites per interval for all services offered by a provider.")
    @GetMapping(path = "provider/favourites/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> pFavourites(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.providerFavourites(id, by), HttpStatus.OK);
    }

    //    @ApiOperation(value = "Get aggregate 'addToProject per interval for all services offered by a provider.")
    @GetMapping(path = "provider/addToProject/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> pAddToProject(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.providerAddToProject(id, by), HttpStatus.OK);
    }

//    @ApiOperation(value = "Get average ratings per interval for all services offered by a provider.")
    @GetMapping(path = "provider/ratings/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Float>> pRatings(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.providerRatings(id, by), HttpStatus.OK);
    }

//    @ApiOperation(value = "Get percentage of visits for all services offered by a provider.")
    @GetMapping(path = "provider/visitation/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Float>> pVisitation(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.providerVisitation(id, by), HttpStatus.OK);
    }

    // Returns the time series of the specified Event type.
    @GetMapping(path = "events", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> events(@RequestParam Event.UserActionType type, @RequestParam Date from, @RequestParam Date to, @RequestParam StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.events(type, from, to, by), HttpStatus.OK);
    }

//    @ApiOperation(value = "Providing the Provider's id, get the relation between all his services and their respective countries.")
    @GetMapping(path = "provider/mapServicesToGeographicalAvailability", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MapValues>> mapServicesToGeographicalAvailability(@RequestParam(required = false) String providerId) {
        return new ResponseEntity<>(statisticsService.mapServicesToGeographicalAvailability(providerId), HttpStatus.OK);
    }

//    @ApiOperation(value = "Get a relation between all Services and their Coordinating Country")
    @GetMapping(path = "provider/mapServicesToProviderCountry", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MapValues>> mapServicesToProviderCountry() {
        return new ResponseEntity<>(statisticsService.mapServicesToProviderCountry(), HttpStatus.OK);
    }

//    @ApiOperation(value = "Providing the Provider's id, get the relation between all his services and a specific Vocabulary")
    @GetMapping(path = "provider/mapServicesToVocabulary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MapValues>> mapServicesToVocabulary(@RequestParam(required = false) String providerId,
                                                                   @RequestParam StatisticsService.Vocabulary vocabulary) {
        return new ResponseEntity<>(statisticsService.mapServicesToVocabulary(providerId, vocabulary), HttpStatus.OK);
    }

//    @ApiOperation(value = "Get a list of places and their corresponding number of Services offered by the specified provider.")
    @GetMapping(path = "provider/servicesPerPlace", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PlaceCount>> servicesPerPlace(@RequestParam(required = false) String providerId) {
        return new ResponseEntity<>(statisticsService.servicesPerPlace(providerId), HttpStatus.OK);
    }

//    @ApiOperation(value = "Get a list of places and their corresponding Services offered by the specified provider.")
    @GetMapping(path = "provider/servicesByPlace", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Value>> servicesByPlace(@RequestParam(required = false) String providerId,
                                                       @RequestParam(required = false) String place) {
        return new ResponseEntity<>(statisticsService.servicesByPlace(providerId, place), HttpStatus.OK);
    }
}
