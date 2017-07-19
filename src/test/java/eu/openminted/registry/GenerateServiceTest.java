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
            "    <id>01.13</id>\n" +
            "    <brandName>Accounting</brandName>\n" +
            "    <tagline>Track and report the usage of your services</tagline>\n" +
            "    <fullName>EGI Accounting</fullName>\n" +
            "    <description>EGI Accounting stores user accounting records from various services offered by EGI, such as Cloud, HTC\n" +
            "        and storage usage. It works thanks to a network of message brokers that transfer usage data from the host to a\n" +
            "        central repository of information. The data is handled securely and can be consulted online through the EGI\n" +
            "        Accounting Portal. EGI council members can use EGI accounting services to account for the resource usage of\n" +
            "        their own services. EGI Accounting gives: - Increased control over resource consumption - Reduced overhead of\n" +
            "        defining data models, architecture and setup of an accounting system - Reduced cost of maintaining an accounting\n" +
            "        infrastructure - Access to a reliable, high available, high performance service - User friendly web interface\n" +
            "    </description>\n" +
            "    <targetUsers>Customer: User communities and EGI service providers. User: VO Managers, Resource providers\n" +
            "        management\n" +
            "    </targetUsers>\n" +
            "    <userValue>- Reduced overhead of defining data models, architecture and setup of an accounting system - Reduced cost\n" +
            "        of maintaining an accounting infrastructure\n" +
            "    </userValue>\n" +
            "    <userBase>EGI resource providers: https://www.egi.eu/federation/data-centres/</userBase>\n" +
            "    <provider>EGI Foundation (via EGI resource providers)</provider>\n" +
            "    <fundingSources>Development: EC H2020 projects (primarily) and national projects/open source projects. Operations:\n" +
            "        EGI participant fees + in-kind contribution from service providers\n" +
            "    </fundingSources>\n" +
            "    <webpage>https://www.egi.eu/internal-services/accounting/</webpage>\n" +
            "    <phase>Production</phase>\n" +
            "    <technologyReadinessLevel>9</technologyReadinessLevel>\n" +
            "    <category>Operations</category>\n" +
            "    <subcategory>Αccounting</subcategory>\n" +
            "    <request>https://www.egi.eu/request-service/</request>\n" +
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
