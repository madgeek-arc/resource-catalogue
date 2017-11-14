package eu.einfracentral.domain;

import java.io.StringWriter;
import java.util.concurrent.*;
import javax.xml.bind.*;
import org.junit.Test;
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