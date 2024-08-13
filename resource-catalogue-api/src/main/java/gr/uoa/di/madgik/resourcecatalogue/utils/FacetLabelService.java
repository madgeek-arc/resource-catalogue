package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.registry.domain.Facet;

import java.util.List;

public interface FacetLabelService {

    List<Facet> generateLabels(List<Facet> facets);
}
