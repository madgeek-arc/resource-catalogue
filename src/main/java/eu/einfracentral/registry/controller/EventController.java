package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.Event;
import eu.einfracentral.registry.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("event")
public class EventController extends ResourceController<Event> {
    @Autowired
    EventController(EventService service) {
        super(service);
    }
}
