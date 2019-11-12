package eu.einfracentral.domain;

import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.StringReader;
import java.io.StringWriter;

public class ServiceTests {
    @Test
    public void createService() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Service.class);
        StringWriter writer = new StringWriter();
        Service eic = new Service();
        eic.setId("eic");
        eic.setName("E Infra Central Catalogue");
        jaxbContext.createMarshaller().marshal(eic, writer);
        jaxbContext.createUnmarshaller().unmarshal(new StringReader(writer.toString()));
    }

    @Test
    public void checkService() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Service.class);
        Service service = (Service) jaxbContext.createUnmarshaller().unmarshal(new StringReader(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<service xmlns=\"http://einfracentral.eu\">\n" +
                        "<id>00.00</id>\n" +
                        "<url>http://beta.einfracentral.eu/</url>\n" +
                        "<name>e Infra Central</name>\n" +
                        "<tagline>Your Gateway for European E-Infrastructures</tagline>\n" +
                        "<description>A service catalogue for the modern hipster</description>\n" +
                        "<options>??? just click on things</options>\n" +
                        "<targetUsers>Researchers</targetUsers>\n" +
                        "<userValue>Fast access to service information</userValue>\n" +
                        "<userBase>Researchers</userBase>\n" +
                        "<symbol>http://beta.einfracentral.eu/imgs/einfracentral_logo_vertical.png</symbol>\n" +
                        "<multimediaURL>http://einfracentral.eu/basic-page/mock-ups</multimediaURL>\n" +
                        "<providers><provider>eic</provider></providers>\n" +
                        "<version>1</version>\n" +
                        "<lastUpdate>2017-12-25</lastUpdate>\n" +
                        "<changeLog>Check it out at https://github.com/eInfraCentral/</changeLog>\n" +
                        "<validFor>2020-01-01</validFor>\n" +
                        "<lifeCycleStatus>Alpha</lifeCycleStatus>\n" +
                        "<trl>7</trl>\n" +
                        "<category>Repository</category>\n" +
                        "<subcategory>enabling_platform</subcategory>\n" +
                        "<places><place>GR</place></places>\n" +
                        "<languages><language>el</language></languages>\n" +
                        "<tags><tag>meta</tag></tags>\n" +
                        "<requiredServices></requiredServices>\n" +
                        "<relatedServices><relatedService>1.2</relatedService></relatedServices>\n" +
                        "<request>http://beta.einfracentral.eu/signUp</request>\n" +
                        "<helpdesk>http://einfracentral.eu/contact</helpdesk>\n" +
                        "<userManual>http://beta.einfracentral.eu/support/faqs</userManual>\n" +
                        "<trainingInformation>http://beta.einfracentral.eu/</trainingInformation>\n" +
                        "<feedback>http://einfracentral.eu/contact</feedback>\n" +
                        "<price>https://einfracentral.eu/</price>\n" +
                        "<serviceLevelAgreement>https://einfracentral.eu/</serviceLevelAgreement>\n" +
                        "<termsOfUse><termOfUse>http://einfracentral.eu/sites/default/files/Privacy%20Policy_Website_eInfraCentral_V0.pdf</termOfUse></termsOfUse>\n" +
                        "<funding>Horizon 2020</funding>\n" +
                        "</service>"));
        jaxbContext.createMarshaller().marshal(service, new StringWriter());
    }
}
