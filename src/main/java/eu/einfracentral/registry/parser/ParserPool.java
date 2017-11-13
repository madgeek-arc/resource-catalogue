package eu.einfracentral.registry.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.User;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.RESTException;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static javax.xml.bind.JAXBContext.newInstance;

/**
 * Created by pgl on 12/7/2017.
 */
@Component("parserPool")
public class ParserPool implements ParserService {
    private static final Logger logger = Logger.getLogger(ParserPool.class);
    private final ExecutorService executor;
    private JAXBContext jaxbContext = null;

    public ParserPool() {
        executor = Executors.newCachedThreadPool();
        try {
            jaxbContext = newInstance(Service.class, User.class, Provider.class, Vocabulary.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> Future<T> serialize(Resource resource, Class<T> tClass) {
        return executor.submit(() -> {
            T type;
            if (resource == null) {
                throw new RESTException("Could not serialize null resource", HttpStatus.BAD_REQUEST);
            }
            try {
                if (resource.getPayloadFormat().equals("xml")) {
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    type = (T) unmarshaller.unmarshal(new StringReader(resource.getPayload()));
                } else if (resource.getPayloadFormat().equals("json")) {
                    ObjectMapper mapper = new ObjectMapper();
                    type = mapper.readValue(resource.getPayload(), tClass);
                } else {
                    throw new RESTException(resource.getPayloadFormat() + " is unsupported", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                }
            } catch (JAXBException e) {
                logger.fatal(e);
                throw new RESTException(e, HttpStatus.I_AM_A_TEAPOT);
            }
            return type;
        });
    }

    @Override
    public Future<String> deserialize(Object resource, ParserServiceTypes mediaType) {
        return executor.submit(() -> {
            if (mediaType == ParserServiceTypes.XML) {
                Marshaller marshaller = jaxbContext.createMarshaller();
                StringWriter sw = new StringWriter();
                marshaller.marshal(resource, sw);
                return sw.toString();
            } else if (mediaType == ParserServiceTypes.JSON) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(resource);
            } else {
                throw new RESTException(mediaType + " is unsupported", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
        });
    }
}