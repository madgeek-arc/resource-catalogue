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

    @ApiOperation(value = "Returns the service visits (via piwik)")
    @RequestMapping(value = "service/visits/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> visits(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.visits(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the service externals (via core)")
    @RequestMapping(value = "service/externals/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> externals(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.externals(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the service internals")
    @RequestMapping(value = "service/internals/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> internals(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.internals(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the service favourites")
    @RequestMapping(value = "service/favourites/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> favourites(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.favourites(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the service ratings")
    @RequestMapping(value = "service/ratings/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Float>> ratings(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.ratings(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the provider visits (via piwik)")
    @RequestMapping(value = "provider/visits/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pVisits(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pVisits(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the provider externals (via core)")
    @RequestMapping(value = "provider/externals/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pExternals(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pExternals(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the provider internals")
    @RequestMapping(value = "provider/internals/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pInternals(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pInternals(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the provider favourites")
    @RequestMapping(value = "provider/favourites/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Integer>> pFavourites(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pFavourites(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the provider ratings")
    @RequestMapping(value = "provider/ratings/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Float>> pRatings(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pRatings(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the visitation portions")
    @RequestMapping(value = "provider/visitation/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Float>> pVisitation(@PathVariable("id") String id, @ApiIgnore @CookieValue(defaultValue = "") String jwt) {
        return new ResponseEntity<>(statisticsService.pVisitation(id), HttpStatus.OK);
    }
}
