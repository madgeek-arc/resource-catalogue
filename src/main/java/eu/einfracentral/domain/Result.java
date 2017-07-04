package eu.einfracentral.domain;

import java.util.List;

public class Result {

    private List<Order<Service>> services;

    public int getTotal() {
        return services.size();
    }

}