package gr.uoa.di.madgik.resourcecatalogue.controllers;

import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.dto.PlaceCount;
import gr.uoa.di.madgik.resourcecatalogue.dto.Value;
import gr.uoa.di.madgik.resourcecatalogue.service.StatisticsService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping("stats")
@Tag(name = "statistics", description = "Get information about KPIs usage statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Autowired
    StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Deprecated(forRemoval = true)
    //    @Operation(summary = "Get visits per interval for a service.")
    @GetMapping(path = "service/visits/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> visits(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                       @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        String id = prefix + "/" + suffix;
//        return new ResponseEntity<>(statisticsService.visits(id, by), HttpStatus.OK);
        throw new UnsupportedOperationException("Method has been removed");
    }

    //    @Operation(summary = "Get addToProject per interval for a service.")
    @GetMapping(path = "service/addToProject/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> addToProject(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                             @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                             @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(statisticsService.addToProject(id, by), HttpStatus.OK);
    }

    @Deprecated(forRemoval = true)
    //    @Operation(summary = "Get aggregate visits per interval for all services offered by a provider.")
    @GetMapping(path = "provider/visits/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> pVisits(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                        @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                        @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(statisticsService.providerVisits(id, by), HttpStatus.OK);
//        throw new UnsupportedOperationException("Method has been removed");
    }

    //    @Operation(summary = "Get aggregate 'addToProject per interval for all services offered by a provider.")
    @GetMapping(path = "provider/addToProject/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> pAddToProject(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                              @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                              @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(statisticsService.providerAddToProject(id, by), HttpStatus.OK);
    }

    @Deprecated(forRemoval = true)
    //    @Operation(summary = "Get percentage of visits for all services offered by a provider.")
    @GetMapping(path = "provider/visitation/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Float>> pVisitation(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                          @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                          @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(statisticsService.providerVisitation(id, by), HttpStatus.OK);
//        throw new UnsupportedOperationException("Method has been removed");
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

    @Deprecated(forRemoval = true)
    //    @Operation(summary = "Get visits per interval for a datasource.")
    @GetMapping(path = "datasource/visits/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> datasourceVisits(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                 @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                 @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        String id = prefix + "/" + suffix;
//        return new ResponseEntity<>(statisticsService.visits(id, by), HttpStatus.OK);
        throw new UnsupportedOperationException("Method has been removed");
    }

    //    @Operation(summary = "Get addToProject per interval for a datasource.")
    @GetMapping(path = "datasource/addToProject/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> datasourceAddToProject(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                       @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(statisticsService.addToProject(id, by), HttpStatus.OK);
    }

    @Deprecated(forRemoval = true)
    @GetMapping(path = "trainingResource/visits/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> trainingResourceVisits(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                       @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                       @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        String id = prefix + "/" + suffix;
//        return new ResponseEntity<>(statisticsService.visits(id, by), HttpStatus.OK);
        throw new UnsupportedOperationException("Method has been removed");
    }

    @GetMapping(path = "trainingResource/addToProject/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> trainingResourceAddToProject(@Parameter(description = "The left part of the ID before the '/'") @PathVariable("prefix") String prefix,
                                                                             @Parameter(description = "The right part of the ID after the '/'") @PathVariable("suffix") String suffix,
                                                                             @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by) {
        String id = prefix + "/" + suffix;
        return new ResponseEntity<>(statisticsService.addToProject(id, by), HttpStatus.OK);
    }
}
