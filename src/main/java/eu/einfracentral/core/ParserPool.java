package eu.einfracentral.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import java.io.*;
import java.util.concurrent.*;
import javax.xml.bind.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import static javax.xml.bind.JAXBContext.newInstance;

/**
 * Created by pgl on 12/7/2017.
 */
@Component("parserPool")
public class ParserPool implements ParserService {
    private final ExecutorService executor;
    private JAXBContext jaxbContext = null;

    public ParserPool() {
        executor = Executors.newCachedThreadPool();
        try {
            jaxbContext = newInstance(Access.class, Manager.class, Provider.class, ServiceAddenda.class, Service.class, User.class,
                                      Vocabulary.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> Future<T> deserialize(Resource resource, Class<T> typeParameterClass) {
        return executor.submit(() -> {
            T ret;
            if (resource == null) {
                throw new ResourceException("Could not serialize null resource", HttpStatus.BAD_REQUEST);
            }
            try {
                if (resource.getPayloadFormat().equals("xml")) {
                    ret = typeParameterClass.cast(jaxbContext.createUnmarshaller().unmarshal(new StringReader(resource.getPayload())));
                } else if (resource.getPayloadFormat().equals("json")) {
                    ret = new ObjectMapper().readValue(resource.getPayload(), typeParameterClass);
                } else {
                    throw new ResourceException(resource.getPayloadFormat() + " is unsupported", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                }
            } catch (JAXBException e) {
                e.printStackTrace();
                throw new ResourceException(e, HttpStatus.I_AM_A_TEAPOT);
            }
            return ret;
        });
    }

    @Override
    public <T> Future<T> deserialize(String json, Class<T> typeParameterClass) {
        return executor.submit(() -> new ObjectMapper().readValue(json, typeParameterClass));
    }

    @Override
    public Resource deserializeResource(File file, ParserServiceTypes mediaType) {
        try {
            if (mediaType == ParserServiceTypes.XML) {
                return (Resource) (jaxbContext.createUnmarshaller().unmarshal(file));
            } else if (mediaType == ParserServiceTypes.JSON) {
                return new ObjectMapper().readValue(file, Resource.class);
            }
        } catch (IOException | JAXBException | ClassCastException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Future<String> serialize(Object resource, ParserServiceTypes mediaType) {
        return executor.submit(() -> {
            if (mediaType == ParserServiceTypes.XML) {
                StringWriter sw = new StringWriter();
                jaxbContext.createMarshaller().marshal(resource, sw);
                return sw.toString();
            } else if (mediaType == ParserServiceTypes.JSON) {
                return new ObjectMapper().writeValueAsString(resource);
            } else {
                throw new ResourceException(mediaType + " is unsupported", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
        });
    }
}