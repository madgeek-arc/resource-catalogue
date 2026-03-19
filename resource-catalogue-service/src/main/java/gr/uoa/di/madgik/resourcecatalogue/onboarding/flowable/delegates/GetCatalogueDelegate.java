package gr.uoa.di.madgik.resourcecatalogue.onboarding.flowable.delegates;

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.flowable.ResourceBundleHelper;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetCatalogueDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(GetCatalogueDelegate.class);

    private final GenericResourceService genericResourceService;
    private final ResourceBundleHelper resourceBundleHelper;

    public GetCatalogueDelegate(GenericResourceService genericResourceService, ResourceBundleHelper resourceBundleHelper) {
        this.genericResourceService = genericResourceService;
        this.resourceBundleHelper = resourceBundleHelper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution execution) {
        Map<String, Object> resource = (Map<String, Object>) execution.getVariable("resource");
        String catalogueId = resource != null ? (String) resource.get("catalogueId") : null;
        logger.info("Running task 'get-catalogue' | catalogueId: {}", catalogueId);
        CatalogueBundle catalogue = null;
        if (catalogueId != null && !catalogueId.isBlank()) {
            try {
                catalogue = genericResourceService.get("catalogue", catalogueId);
            } catch (Exception ignore) {}
        }
        execution.setVariable("catalogue", catalogue != null ? resourceBundleHelper.toMap(catalogue) : null);
    }
}
