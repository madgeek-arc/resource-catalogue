package eu.einfracentral.registry;

import eu.einfracentral.domain.Provider;
import java.io.*;
import javax.xml.bind.*;
import org.junit.Test;

/**
 * Created by pgl on 19/7/2017.
 */
public class GenerateProviderTest {
    @Test
    public void createProvider() throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(Provider.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter sw = new StringWriter();
        Provider testProvider = new Provider();
        testProvider.setId("egi");
        testProvider.setName("EGI Foundation");
        marshaller.marshal(testProvider, sw);
        System.out.println(sw.toString());
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Provider type = (Provider) unmarshaller.unmarshal(new StringReader(sw.toString()));
        //System.out.println(type.getId());
    }

    @Test
    public void checkProvider() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<provider xmlns=\"http://einfracentral.eu\">\n" +
                "\t<id>prace</id>\n" +
                "\t<name>PRACE</name>\n" +
                "\t<contactInformation></contactInformation>\n" +
                "\t<users></users>\n" +
                "\t<services></services>\n" +
                "</provider>";
        JAXBContext jaxbContext = JAXBContext.newInstance(Provider.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Provider type = (Provider) unmarshaller.unmarshal(new StringReader(xml));
        System.out.println(type.getId());
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter sw = new StringWriter();
        marshaller.marshal(type, sw);
        System.out.println(sw.toString());
    }
}
