package eu.openminted.registry;

import eu.einfracentral.domain.Service;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Created by pgl on 19/7/2017.
 */

public class GenerateServiceTest {

    @Test
    public void createService() throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(Service.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter sw = new StringWriter();

        Service testService = new Service();

        testService.setId("Hello World");
        marshaller.marshal(testService, sw);
        System.out.println(sw.toString());
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Service type = (Service) unmarshaller.unmarshal(new StringReader(sw.toString()));

        System.out.println(type.getId());
    }

    @Test
    public void checkService() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<service xmlns=\"http://einfracentral.eu\">\n" +
                "    <id>00.00</id>\n" +
                "    <url>http://example.org/</url>\n" +
                "    <name>Name</name>\n" +
                "    <description>A service that does things</description>\n" +
                "    <provider>Jo Mama</provider>\n" +
                "    <lifeCyYcleStatus>Production</lifeCyYcleStatus>\n" +
                "    <trl>9</trl>\n" +
                "    <category>Cat</category>\n" +
                "    <subcategory>Subcat</subcategory>\n" +
                "    <place>Nowhere</place>\n" +
                "    <language>None</language>\n" +
                "    <tag>None</tag>\n" +
                "    <request>https://example.org/</request>\n" +
                "    <price>https://example.org/</price>\n" +
                "    <serviceLevelAgreement>https://example.org/</serviceLevelAgreement>\n" +
                "</service>";
        JAXBContext jaxbContext = JAXBContext.newInstance(Service.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Service type = (Service) unmarshaller.unmarshal(new StringReader(xml));

        System.out.println(type.getId());

        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter sw = new StringWriter();
        marshaller.marshal(type, sw);
        System.out.println(sw.toString());

    }
}
