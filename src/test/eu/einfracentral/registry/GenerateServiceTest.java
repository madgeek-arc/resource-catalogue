package eu.einfracentral.registry;

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
                "    <url>http://beta.einfracentral.eu/</url>\n" +
                "    <name>e Infra Central</name>\n" +
                "    <tagline>Your Gateway for European E-Infrastructures</tagline>\n" +
                "    <description>A service catalogue for the modern hipster</description>\n" +
                "    <options>??? just click on things</options>\n" +
                "    <targetUsers>Researchers</targetUsers>\n" +
                "    <userValue>Fast access to service information</userValue>\n" +
                "    <userBase>Researchers</userBase>\n" +
                "    <symbol>http://beta.einfracentral.eu/imgs/einfracentral_logo_vertical.png</symbol>\n" +
                "    <multimediaURL>http://einfracentral.eu/basic-page/mock-ups</multimediaURL>\n" +
                "    <providers><provider>eic</provider></providers>\n" +
                "    <version>1</version>\n" +
                "    <lastUpdate>2017-12-25</lastUpdate>\n" +
                "    <changeLog>Check it out at https://github.com/eInfraCentral/</changeLog>\n" +
                "    <validFor>2020-01-01</validFor>\n" +
                "    <lifeCycleStatus>Alpha</lifeCycleStatus>\n" +
                "    <trl>7</trl>\n" +
                "    <category>Category_Repository</category>\n" +
                "    <subcategory>Subcategory_enabling_platform</subcategory>\n" +
                "    <places><place>Country_GR</place></places>\n" +
                "    <languages><language>Language_el</language></languages>\n" +
                "    <tags><tag>meta</tag></tags>\n" +
                "    <requiredServices></requiredServices>\n" +
                "    <relatedServices><relatedService>1.2</relatedService></relatedServices>\n" +
                "    <request>http://beta.einfracentral.eu/signUp</request>\n" +
                "    <helpdesk>http://einfracentral.eu/contact</helpdesk>\n" +
                "    <userManual>http://beta.einfracentral.eu/support/faqs</userManual>\n" +
                "    <trainingInformation>http://beta.einfracentral.eu/</trainingInformation>\n" +
                "    <feedback>http://einfracentral.eu/contact</feedback>\n" +
                "    <price>https://einfracentral.eu/</price>\n" +
                "    <serviceLevelAgreement>https://einfracentral.eu/</serviceLevelAgreement>\n" +
                "    <termsOfUse><termOfUse>http://einfracentral.eu/sites/default/files/Privacy%20Policy_Website_eInfraCentral_V0.pdf</termOfUse></termsOfUse>\n" +
                "    <funding>Horizon 2020</funding>\n" +
                "</service>";
        JAXBContext jaxbContext = JAXBContext.newInstance(Service.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Service type = (Service) unmarshaller.unmarshal(new StringReader(xml));
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter sw = new StringWriter();
        marshaller.marshal(type, sw);
        System.out.println(sw.toString());
    }
}
