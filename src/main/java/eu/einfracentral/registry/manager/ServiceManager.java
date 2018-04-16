package eu.einfracentral.registry.manager;

import com.fasterxml.jackson.databind.JsonNode;
import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.AnalyticsService;
import eu.openminted.registry.core.domain.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import javax.management.j2ee.statistics.Statistic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Created by pgl on 4/7/2017.
 */
@org.springframework.stereotype.Service("serviceService")
public class ServiceManager extends ResourceManager<Service> implements ServiceService {
    @Autowired
    private AddendaManager addendaManager;
    @Autowired
    private AnalyticsService analyticsService;
    @Autowired
    private StatisticalService statisticalService;

    public ServiceManager() {
        super(Service.class);
    }

    @Override
    public String getResourceType() {
        return "service";
    }

    @Override
    public Service add(Service service) {
        if (!service.getId().contains(".")) {
            service.setId(java.util.UUID.randomUUID().toString());
        }
        if (exists(service)) {
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }
        return super.add(validate(service));
    }

    private Service fixVersion(Service service) {
        if (service.getVersion() == null || service.getVersion().equals("")) {
            service.setVersion("0");
        }
        return service;
    }

    @Override
    public Service update(Service service) {
        service = validate(service);
        Service existingService = get(service.getId());
        Addenda addenda = ensureAddenda(service.getId());
        fixVersion(existingService); //remove this when it has ran for all services
        if (!service.getVersion().equals(existingService.getVersion())) {
            addenda.setModifiedAt(System.currentTimeMillis());
            addenda.setModifiedBy("pgl");
            super.update(service);
        } else {
            addenda.setRegisteredAt(System.currentTimeMillis());
            addenda.setRegisteredBy("pgl");
            super.add(service);
        }
        try {
            addendaManager.update(addenda);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return service;
    }

    private Addenda ensureAddenda(String id) {
        Addenda ret = null;
        Resource existingAddendaResource = addendaManager.where("service", id, false);
        if (existingAddendaResource != null) {
            try {
                ret = parserPool.deserialize(existingAddendaResource, Addenda.class).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                ret = makeAddenda(id);
            }
        } else {
            ret = makeAddenda(id);
        }
        return ret;
    }

    private Addenda makeAddenda(String id) {
        Addenda ret = new Addenda();
        ret.setId(UUID.randomUUID().toString());
        ret.setService(id);
        try {
            addendaManager.add(ret);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public Service validate(Service service) {
        return fixVersion(service);
    }

    @Override
    public Map<String, Integer> visits(String id) {
        return analyticsService.getVisitsForLabel("service/" + id);
    }

    private static String[] getDates() {
        String[] ret = new String[30];
        Calendar day = Calendar.getInstance();
        for (int i = 0; i < ret.length; i++) {
            day.add(Calendar.DAY_OF_YEAR, -1);
            ret[i] = new SimpleDateFormat("yyyy-MM-dd").format(day.getTime());
        }
        return ret;
    }

    @Override
    public Map<String, Integer> favourites(String id) {
        Map<String, Integer> ret = statisticalService.averageFavouritesByService(id);
        if (ret.isEmpty()) {
            Stream.of(getDates()).forEach(i -> ret.put(i, ThreadLocalRandom.current().nextInt(0, 9)));
        }
        return ret;
    }

    @Override
    public Map<String, Float> ratings(String id) {
        Map<String, Float> ret = statisticalService.averageRatingByService(id);
        if (ret.isEmpty()) {
            Stream.of(getDates()).forEach(i -> ret.put(i, 5 * ThreadLocalRandom.current().nextFloat()));
        }
        return ret;
    }

    @Override
    public Integer rating(String id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
