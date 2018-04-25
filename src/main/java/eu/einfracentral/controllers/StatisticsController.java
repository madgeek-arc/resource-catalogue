package eu.einfracentral.controllers;

import eu.einfracentral.service.StatisticsService;
import io.swagger.annotations.ApiOperation;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Created by pgl on 23/04/18.
 */
@RestController
@RequestMapping("stats")
public class StatisticsController {
    @Autowired
    private StatisticsService statisticsService;

    @ApiOperation(value = "Returns the time series of service page visits in eic (via piwik)")
    @RequestMapping(path = "service/visits/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> visits(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.visits(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the time series of service page visits in the provider's site")
    @RequestMapping(path = "service/externals/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> externals(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.externals(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the time series of service page visits in eic (via registry-core)")
    @RequestMapping(path = "service/orders/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> internals(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.internals(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the time series of service favourites")
    @RequestMapping(path = "service/favourites/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> favourites(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.favourites(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the time series of service ratings")
    @RequestMapping(path = "service/ratings/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Float>> ratings(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.ratings(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the time series of service page visits in eic (via piwik) for all of the provider's services")
    @RequestMapping(path = "provider/visits/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pVisits(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pVisits(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the time series of service page visits in the provider's site for all of the provider's services")
    @RequestMapping(path = "provider/externals/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pExternals(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pExternals(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the time series of service page visits in eic (via registry-core) for all of the provider's services")
    @RequestMapping(path = "provider/orders/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pInternals(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pInternals(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the time series of service favourites for all of the provider's services")
    @RequestMapping(path = "provider/favourites/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pFavourites(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pFavourites(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the time series of service ratings for all of the provider's services")
    @RequestMapping(path = "provider/ratings/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Float>> pRatings(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pRatings(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the visitation portions for all of the provider's services")
    @RequestMapping(path = "provider/visitation/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Float>> pVisitation(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pVisitation(id), HttpStatus.OK);
    }
}
