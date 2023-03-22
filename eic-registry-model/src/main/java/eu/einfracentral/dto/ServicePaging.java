package eu.einfracentral.dto;

import eu.einfracentral.domain.Datasource;
import eu.einfracentral.domain.Service;
import eu.openminted.registry.core.domain.Paging;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement
public class ServicePaging<T> extends Paging<T> {

    public ServicePaging() {
        super();
    }

    public ServicePaging(Paging<T> page) {
        super(page);
        this.results = page.getResults();
//        page.setResults(null);
    }

    @XmlElementWrapper(name = "results")
    @XmlElements({
            @XmlElement(name="service", type= Service.class),
            @XmlElement(name="datasource", type= Datasource.class)
    })
    private List<T> results;

    public List<T> getResults() {
        return results;
    }
}
