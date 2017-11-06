package eu.einfracentral.registry;

import eu.einfracentral.domain.Provider;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;

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
                "<ns0:provider xmlns=\"http://einfracentral.eu\" xmlns:ns0=\"http://einfracentral.eu\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://einfracentral.eu https://builds.openminted.eu/job/eic-registry-model/ws/target/generated-resources/schemagen/schema1.xsd\">\n" +
                "\t<ns0:id>prace</ns0:id>\n" +
                "\t<ns0:name>PRACE</ns0:name>\n" +
                "\t<ns0:contactInformation></ns0:contactInformation>\n" +
                "\t<ns0:users></ns0:users>\n" +
                "\t<ns0:services></ns0:services>\n" +
                "</ns0:provider>";

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
