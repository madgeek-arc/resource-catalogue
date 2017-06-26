package eu.openminted.registry.domain;

/**
 * Created by stefanos on 29/11/2016.
 */
public class Order<T extends BaseMetadataRecord> {
    private int order;
    private T resource;

    public Order() {
    }

    public Order(int order, T resource) {
        this.order = order;
        this.resource = resource;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public T getResource() {
        return resource;
    }

    public void setResource(T resource) {
        this.resource = resource;
    }
}
