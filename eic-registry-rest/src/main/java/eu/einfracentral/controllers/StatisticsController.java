package eu.einfracentral.controllers;

import eu.einfracentral.domain.Event;
import eu.einfracentral.service.StatisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("stats")
@Api("Get information about KPIs and eInfraCentral usage statistics")
public class StatisticsController {

    private StatisticsService statisticsService;

    @Autowired
    StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @ApiOperation(value = "Get visits per interval for a service.")
    @RequestMapping(path = "service/visits/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> visits(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(statisticsService.visits(id, by), HttpStatus.OK);
    }

    @ApiOperation(value = "Get favourites per interval for a service.")
    @RequestMapping(path = "service/favourites/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> favourites(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(statisticsService.favourites(id, by), HttpStatus.OK);
    }

    @ApiOperation(value = "Get average ratings per interval for a service.")
    @RequestMapping(path = "service/ratings/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Float>> ratings(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(statisticsService.ratings(id, by), HttpStatus.OK);
    }

    @ApiOperation(value = "Get aggregate visits per interval for all services offered by a provider.")
    @RequestMapping(path = "provider/visits/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pVisits(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(statisticsService.providerVisits(id, by), HttpStatus.OK);
    }

    @ApiOperation(value = "Get aggregate 'favourites per interval for all services offered by a provider.")
    @RequestMapping(path = "provider/favourites/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pFavourites(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(statisticsService.providerFavourites(id, by), HttpStatus.OK);
    }

    @ApiOperation(value = "Get average ratings per interval for all services offered by a provider.")
    @RequestMapping(path = "provider/ratings/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Float>> pRatings(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(statisticsService.providerRatings(id, by), HttpStatus.OK);
    }

    @ApiOperation(value = "Get percentage of visits for all services offered by a provider.")
    @RequestMapping(path = "provider/visitation/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Float>> pVisitation(@PathVariable("id") String id, @RequestParam(defaultValue = "MONTH") StatisticsService.Interval by, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(statisticsService.providerVisitation(id, by), HttpStatus.OK);
    }

    // Returns the time series of the specified Event type.
    @RequestMapping(path = "events", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> events(@RequestParam Event.UserActionType type, @RequestParam Date from, @RequestParam Date to, @RequestParam StatisticsService.Interval by) {
        return new ResponseEntity<>(statisticsService.events(type, from, to, by), HttpStatus.OK);
    }
}
