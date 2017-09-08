package eu.einfracentral.registry.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.domain.aai.User;
import eu.einfracentral.domain.performance.Indicator;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.log4j.Logger;
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
    private static Logger logger = Logger.getLogger(ParserPool.class);
    private ExecutorService executor;
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
                throw new ServiceException("null resource");
            }
            try {
                if (resource.getPayloadFormat().equals("xml")) {
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    type = (T) unmarshaller.unmarshal(new StringReader(resource.getPayload()));
                } else if (resource.getPayloadFormat().equals("json")) {
                    ObjectMapper mapper = new ObjectMapper();
                    type = mapper.readValue(resource.getPayload(), tClass);
                } else {
                    throw new ServiceException("Unsupported media type");
                }
            } catch (JAXBException je) {
                throw new ServiceException(je);
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
                throw new ServiceException("Unsupported media type");
            }
        });
    }
}