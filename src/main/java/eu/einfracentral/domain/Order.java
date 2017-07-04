package eu.einfracentral.domain;

public class Order<T> {
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
