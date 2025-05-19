package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import org.springframework.security.core.Authentication;

public interface AdapterService  extends ResourceCatalogueService<AdapterBundle>, BundleOperations<AdapterBundle> {

    AdapterBundle add(AdapterBundle adapter, Authentication authentication);
    AdapterBundle add(AdapterBundle adapter, String catalogueId, Authentication auth);
    AdapterBundle update(AdapterBundle adapter, String comment, Authentication auth);
    AdapterBundle update(AdapterBundle adapterBundle, String catalogueId, String comment, Authentication auth);
    void delete(AdapterBundle adapter);
}
