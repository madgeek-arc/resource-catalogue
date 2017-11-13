package eu.einfracentral.domain;

import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static javax.xml.bind.JAXBContext.newInstance;

/**
 * Created by pgl on 23/08/17.
 */
public class InterfaceTest {
    private ExecutorService executor;
    private JAXBContext jaxbContext = null;

    public InterfaceTest() {
        executor = Executors.newCachedThreadPool();
        try {
            jaxbContext = newInstance(MyPOJO.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test2() throws Exception {
        MyPOJO resource = new MyPOJO();
        resource.setField("field");
        resource.setId("id");
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter sw = new StringWriter();
        marshaller.marshal(resource, sw);
        System.err.println(sw.toString());
    }
}