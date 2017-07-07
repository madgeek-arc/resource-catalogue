package eu.einfracentral.domain;

import java.util.List;

public class Page<T> {

    int total;
    int from;
    int to;
    List<T> results;
    List<Facet> facets;

    public Page(int total, int from, int to, List<T> results, List<Facet> facets) {
        this.total = total;
        this.from = from;
        this.to = to;
        this.results = results;
        this.facets = facets;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public List<T> getResults() {
        return results;
    }

    public void setResults(List<T> results) {
        this.results = results;
    }

    public List<Facet> getFacets() {
        return facets;
    }

    public void setFacets(List<Facet> facets) {
        this.facets = facets;
    }

}