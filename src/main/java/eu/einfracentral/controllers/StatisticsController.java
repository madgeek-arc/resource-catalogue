package eu.einfracentral.controllers;

import eu.einfracentral.service.StatisticsService;
import io.swagger.annotations.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("stats")
@Api("Get information about KPIs and eInfraCentral usage statistics")
public class StatisticsController {
    @Autowired
    private StatisticsService statisticsService;

    @ApiOperation(value = "Get visits per day for a service")
    @RequestMapping(path = "service/visits/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> visits(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.visits(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the time series of service page visits in the provider's site")
    @RequestMapping(path = "service/orders/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> externalsAlias(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return externals(id, jwt);
    }

    @RequestMapping(path = "service/externals/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> externals(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.externals(id), HttpStatus.OK);
    }

    @RequestMapping(path = "service/internals/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> internals(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.internals(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Get favourites per day for a service")
    @RequestMapping(path = "service/favourites/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> favourites(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.favourites(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Get average ratings per day for a service")
    @RequestMapping(path = "service/ratings/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Float>> ratings(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.ratings(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Get aggregate visits per day for all services offered by a provider")
    @RequestMapping(path = "provider/visits/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pVisits(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pVisits(id), HttpStatus.OK);
    }

    @RequestMapping(path = "provider/externals/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pExternals(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pExternals(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the time series of service page visits in the provider's site for all of the provider's services")
    @RequestMapping(path = "provider/orders/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pExternalsAlias(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pExternals(id), HttpStatus.OK);
    }

    @RequestMapping(path = "provider/internals/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pInternals(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pInternals(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Get aggregate favourites per day for all services offered by a provider")
    @RequestMapping(path = "provider/favourites/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pFavourites(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pFavourites(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Get average ratings per day for all services offered by a provider")
    @RequestMapping(path = "provider/ratings/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Float>> pRatings(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pRatings(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Get percentage of visits for all services offered by a provider")
    @RequestMapping(path = "provider/visitation/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Float>> pVisitation(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pVisitation(id), HttpStatus.OK);
    }
}
