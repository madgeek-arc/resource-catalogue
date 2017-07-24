package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Vocabulary;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.log4j.Logger;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * Created by pgl on 24/7/2017.
 */
public class VocabularyServiceImpl<T> extends VocabularyServiceHmpl<Vocabulary> implements VocabularyService {

    private Logger logger = Logger.getLogger(VocabularyServiceImpl.class);

    public VocabularyServiceImpl() {
        super(Vocabulary.class);
    }

    @Override
    public Vocabulary get(String id) {
        Vocabulary resource;
        try {
            resource = parserPool.serialize(searchService.searchId("vocabulary", new SearchService.KeyValue("id", id)), Vocabulary.class).get();
        } catch (UnknownHostException | InterruptedException | ExecutionException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
        return resource;
    }

    @Override
    public Browsing getAll(FacetFilter facetFilter) {
        facetFilter.setBrowseBy(getBrowseBy());
        return getResults(facetFilter);
    }

    @Override
    public Browsing getMy(FacetFilter facetFilter) {
        return null;
    }

    @Override
    public void add(Vocabulary vocabulary) {
        Vocabulary $vocabulary;
//        try {
//            $vocabulary = parserPool.serialize(searchService.searchId("vocabulary", new SearchService.KeyValue("vocabid", "" + vocabulary.getId())), Service.class).get();
//        } catch (UnknownHostException | InterruptedException | ExecutionException e) {
//            logger.fatal(e);
//            throw new ServiceException(e);
//        }
//        if ($vocabulary != null) {
//            throw new ServiceException("Service already exists");
//        }
        Resource resource = new Resource();
        String serialized = null;
        try {
            serialized = parserPool.deserialize(vocabulary, ParserService.ParserServiceTypes.XML).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }

        if (!serialized.equals("failed")) {
            resource.setPayload(serialized);
        } else {
            throw new ServiceException("Serialization failed");
        }
        resource.setCreationDate(new Date());
        resource.setModificationDate(new Date());
        resource.setPayloadFormat("xml");
        resource.setResourceType(getResourceType());
        resource.setVersion("not_set");
        resource.setId("wont be saved");

        resourceService.addResource(resource);
    }

    @Override
    public void update(Vocabulary vocabulary) {
        Resource $resource;
        Resource resource = new Resource();
        try {
            $resource = searchService.searchId("vocabulary", new SearchService.KeyValue("id", "" + vocabulary.getId()));
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }

        if ($resource != null) {
            throw new ServiceException("Vocabulary already exists");
        } else {
            try {
                String serialized = parserPool.deserialize(vocabulary, ParserService.ParserServiceTypes.XML).get();

                if (!serialized.equals("failed")) {
                    resource.setPayload(serialized);
                } else {
                    throw new ServiceException("Serialization failed");
                }
                resource = (Resource) $resource;
                resource.setPayloadFormat("xml");
                resource.setPayload(serialized);
                resourceService.updateResource(resource);
            } catch (ExecutionException | InterruptedException e) {
                logger.fatal(e);
                throw new ServiceException(e);
            }
        }
    }

    @Override
    public void delete(Vocabulary vocabulary) {
        Resource resource;
        try {
            resource = searchService.searchId("vocabulary", new SearchService.KeyValue("id", "" + vocabulary.getId()));
            if (resource == null) {
                throw new ServiceException("Vocabulary doesn't exist");
            } else {
                resourceService.deleteResource(resource.getId());
            }
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
    }

    @Override
    public String getResourceType() {
        return "vocabulary";
    }

}
