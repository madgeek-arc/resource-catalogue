package eu.einfracentral.domain;

import java.io.*;
import javax.xml.bind.*;
import org.junit.Test;

public class ProviderTests {
    @Test
    public void createProvider() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Provider.class);
        StringWriter writer = new StringWriter();
        Provider eic = new Provider();
        eic.setId("eic");
        eic.setName("E Infra Central Provider");
        jaxbContext.createMarshaller().marshal(eic, writer);
        jaxbContext.createUnmarshaller().unmarshal(new StringReader(writer.toString()));
    }

    @Test
    public void checkProvider() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Provider.class);
        Provider provider = (Provider) jaxbContext.createUnmarshaller().unmarshal(new StringReader(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<provider xmlns=\"http://einfracentral.eu\">\n" +
                        "\t<id>eic</id>\n" +
                        "\t<name>E Infra Central</name>\n" +
                        "</provider>"));
        jaxbContext.createMarshaller().marshal(provider, new StringWriter());
    }
}
