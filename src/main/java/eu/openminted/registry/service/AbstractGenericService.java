package eu.openminted.registry.service;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.registry.domain.BaseMetadataRecord;
import eu.openminted.registry.domain.Browsing;
import eu.openminted.registry.domain.MetadataHeaderInfo;
import eu.openminted.registry.domain.Order;
import eu.openminted.registry.generate.MetadataHeaderInfoGenerate;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by stefanos on 20/6/2017.
 */

@Service("genericService")
abstract public class AbstractGenericService<T extends BaseMetadataRecord> implements ResourceCRUDService<T>{

    private Logger logger = Logger.getLogger(ComponentServiceImpl.class);

    @Autowired
    SearchService searchService;

    @Autowired
    ResourceService resourceService;

    public abstract String getResourceType();

    final private Class<T> typeParameterClass;

    public AbstractGenericService(Class<T> typeParameterClass) {
        this.typeParameterClass = typeParameterClass;
    }

    @Override
    public T get(String id) {
        T resource;
        try {
            resource = Utils.serialize(searchService.searchId(getResourceType(), id), typeParameterClass);
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
        return resource;
    }

    @Override
    public Browsing getAll(FacetFilter filter) {
//        filter.addFilter("public",true);
        return getResults(filter);
    }

    @Override
    public Browsing getMy(FacetFilter filter) {
        OIDCAuthenticationToken authentication = (OIDCAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        filter.addFilter("personIdentifier",authentication.getSub());
        return getResults(filter);
    }

    @Override
    public void add(T resource) {
        T $component;
        XMLGregorianCalendar calendar;
        try {
            $component = Utils.serialize(searchService.searchId(getResourceType(),
                    resource.getMetadataHeaderInfo().getMetadataRecordIdentifier().getValue()), typeParameterClass);
            GregorianCalendar gregory = new GregorianCalendar();
            gregory.setTime(new Date());

            calendar = DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(
                            gregory);
        } catch (UnknownHostException | DatatypeConfigurationException e ) {
            logger.fatal(e);
            throw new ServiceException(e);
        }

        if ($component != null) {
            throw new ServiceException(getResourceType() + " already exists");
        }




        Resource resourceDb = new Resource();
        resource.getMetadataHeaderInfo().setMetadataCreationDate(calendar);
        resource.getMetadataHeaderInfo().setMetadataLastDateUpdated(calendar);
        String serialized = Utils.unserialize(resource, BaseMetadataRecord.class);

        if (!serialized.equals("failed")) {
            resourceDb.setPayload(serialized);
        } else {
            throw new ServiceException("Serialization failed");
        }

        resourceDb.setCreationDate(new Date());
        resourceDb.setModificationDate(new Date());
        resourceDb.setPayloadFormat("xml");
        resourceDb.setResourceType(getResourceType());
        resourceDb.setVersion("not_set");
        resourceDb.setId("wont be saved");


        resourceService.addResource(resourceDb);
    }

    @Override
    public void update(T resources) {
        Resource $resource;

        try {
            $resource = searchService.searchId(getResourceType(), resources.getMetadataHeaderInfo().getMetadataRecordIdentifier().getValue());
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }

        Resource resource = new Resource();

        if ($resource != null) {
            throw new ServiceException(getResourceType() + " already exists");
        } else {
            String serialized = Utils.unserialize(resources, typeParameterClass);

            if (!serialized.equals("failed")) {
                resource.setPayload(serialized);
            } else {
                throw new ServiceException("Serialization failed");
            }
            resource = $resource;
            resource.setPayloadFormat("xml");
            resource.setPayload(serialized);
            resourceService.updateResource(resource);
        }
    }

    @Override
    public void delete(T component) {
        Resource resource;
        try {
            resource = searchService.searchId(getResourceType(), component.getMetadataHeaderInfo().getMetadataRecordIdentifier().getValue());
            if (resource != null) {
                throw new ServiceException(getResourceType() + " already exists");
            } else {
                resourceService.deleteResource(resource.getId());
            }
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
    }

    private Browsing<T> getResults(FacetFilter filter) {
        List<Order<T>> result = new ArrayList<>();
        Paging paging;
        filter.setResourceType(getResourceType());
        Browsing<T> browsing;
        try {
            paging = searchService.search(filter);
            int index = 0;
            for(Object res : paging.getResults()) {
                Resource resource = (Resource) res;
                T resourceSpecific = Utils.serialize(resource,typeParameterClass);
                result.add(new Order(index,resourceSpecific));
                index++;
            }
        } catch (UnknownHostException e ) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
        browsing = new Browsing(paging.getTotal(), filter.getFrom(), filter.getFrom() + result.size(), result, null);
        return browsing;
    }
}
