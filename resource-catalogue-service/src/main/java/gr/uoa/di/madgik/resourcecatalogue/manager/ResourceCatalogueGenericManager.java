package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceCatalogueGenericService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

//TODO: resource-specific method -> inside corresponding manager/service
//TODO: true universal method (all resources will use it) -> inside here (generic)
//TODO: some but not all resources need a method -> create a new interface with default implementation

@org.springframework.stereotype.Service("resourceCatalogueGenericManager")
public abstract class ResourceCatalogueGenericManager<T extends Bundle> implements ResourceCatalogueGenericService<T> {

    private static final Logger logger = LoggerFactory.getLogger(ResourceCatalogueGenericManager.class);

    protected final GenericResourceService genericResourceService;
    protected final SecurityService securityService;
    protected final VocabularyService vocabularyService;
    protected final IdCreator idCreator;
    protected final WorkflowService workflowService;

    @Autowired
    private FacetLabelService facetLabelService;

    @Value("${catalogue.id}")
    protected String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    protected abstract String getResourceTypeName();

    protected ResourceCatalogueGenericManager(GenericResourceService genericResourceService,
                                              IdCreator idCreator,
                                              SecurityService securityService,
                                              VocabularyService vocabularyService,
                                              WorkflowService workflowService) {
        this.genericResourceService = genericResourceService;
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.workflowService = workflowService;
    }

    public void createIdentifiers(Bundle bundle) {
        String catalogueId = bundle.getCatalogueId();
        if (catalogueId == null || catalogueId.isEmpty() || catalogueId.equals(this.catalogueId)) {
            this.createIdentifiers(bundle, getResourceTypeName(), false);
            bundle.setId(bundle.getIdentifiers().getOriginalId());
        } else {
            idCreator.validateId(bundle.getId());
            this.createIdentifiers(bundle, getResourceTypeName(), true);
        }
    }

    public void createIdentifiers(Bundle bundle, String resourceType, boolean external) {
        Identifiers identifiers = new Identifiers();
        identifiers.setPid(idCreator.generate(resourceType));
        if (external) {
            identifiers.setOriginalId(bundle.getId());
        } else {
            identifiers.setOriginalId(identifiers.getPid() + "00");
        }
        bundle.setIdentifiers(identifiers);
    }

    //TODO: we don't need this
    @Override
    public T get(String id) {
        return genericResourceService.get(
                getResourceTypeName(),
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false")
        );
    }

    @Override
    public T get(String id, String catalogueId) {
        if (catalogueId != null && !catalogueId.isBlank()) {
            return genericResourceService.get(getResourceTypeName(),
                    new SearchService.KeyValue("resource_internal_id", id),
                    new SearchService.KeyValue("catalogue_id", catalogueId),
                    new SearchService.KeyValue("published", "false"));
        }
        return genericResourceService.get(getResourceTypeName(),
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false"));
    }

    //TODO: probably we do not need this IF we use the same get for drafts and non-drafts
    //TODO: draft functionality is default-catalogue specific, meaning the IDs are always unique
    @Override
    public T get(SearchService.KeyValue... keyValues) {
        return genericResourceService.get(getResourceTypeName(), keyValues);
    }

    @Override
    public T getOrElseReturnNull(String id) {
        T bundle;
        try {
            bundle = get(id);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
        return bundle;
    }

    @Override
    public Browsing<T> getMyProviders(FacetFilter ff, Authentication auth, String resourceType) {
        ff.setResourceType(resourceType);
        ff.setQuantity(maxQuantity);
        ff.addFilter("published", false);
        ff.addFilter("users", AuthenticationInfo.getEmail(auth).toLowerCase());
        ff.addOrderBy("name", "asc");
        return genericResourceService.getResults(ff);
    }

    @Override
    public Browsing<T> getMyResources(FacetFilter filter, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", false); // A Draft Provider cannot have resources
        List<T> providers = getMyProviders(ff, auth, "organisation").getResults();
        if (providers.isEmpty()) {
            return new Browsing<>();
        }

        filter.setResourceType(getResourceTypeName());
        filter.setQuantity(maxQuantity);
        filter.addFilter("published", false);
        filter.addFilter("resource_owner", providers.stream().map(T::getId).toList());
        filter.addOrderBy("name", "asc");
        return genericResourceService.getResults(filter);
    }

    @Override
    public List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> listResources(String catalogueId) {
        final String effectiveCatalogueId = catalogueId != null ? catalogueId : this.catalogueId;
        List<Bundle> bundles = Stream.concat(
                this.getAll(createFacetFilter(effectiveCatalogueId, false, getResourceTypeName()))
                        .getResults()
                        .stream()
                        .filter(Objects::nonNull)
                        .map(c -> (Bundle) c),
                this.getAll(createFacetFilter(effectiveCatalogueId, true, getResourceTypeName()))
                        .getResults()
                        .stream()
                        .filter(Objects::nonNull)
                        .map(c -> (Bundle) c)
                        .filter(b -> !Objects.equals(b.getCatalogueId(), effectiveCatalogueId))
        ).toList();

        List<gr.uoa.di.madgik.resourcecatalogue.dto.Value> allResources = bundles.stream()
                .map(b -> new gr.uoa.di.madgik.resourcecatalogue.dto.Value(
                        b.getId(),
                        b.getPayload().get("name").toString()
                ))
                .toList();

        return allResources;
    }

    private FacetFilter createFacetFilter(String catalogueId, boolean isPublic, String resourceType) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("status", "approved");
        ff.addFilter("active", true);
        ff.addFilter("draft", false);
        if (isPublic) {
            ff.addFilter("published", true);
        } else {
            ff.addFilter("catalogue_id", catalogueId);
            ff.addFilter("published", false);
        }
        ff.setResourceType(resourceType);
        return ff;
    }

    @Override
    public Browsing<T> getAll(FacetFilter ff, Authentication auth) {
        ff.setResourceType(getResourceTypeName());
        boolean authenticated = auth != null && auth.isAuthenticated();
        if (authenticated) {
            if (securityService.hasPortalAdminRole(auth)) {
                return getAll(ff);
            }
            if (securityService.hasRole(auth, "ROLE_PROVIDER")) {
                //TODO: this works only for old Catalogues, Providers. How to proceed
//                ff.addFilter("users", AuthenticationInfo.getEmail(auth).toLowerCase());
                return getAll(ff);
            }
        }
        ff.addFilter("status", "approved");
        ff.addFilter("active", true);
        return getAll(ff);
    }

    @Override
    public Browsing<T> getAll(FacetFilter ff) {
        ff.setResourceType(getResourceTypeName());
        Browsing<T> browsing = genericResourceService.getResults(ff);
        if (!browsing.getResults().isEmpty() && !browsing.getFacets().isEmpty()) {
            browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
        }
        return browsing;
    }

    @Override
    public T add(T bundle, Authentication auth) {
        createIdentifiers(bundle);
        T ret = genericResourceService.add(getResourceTypeName(), bundle);
        try {
            ret = workflowService.onboard(getResourceTypeName(), ret, auth);
            ret = genericResourceService.update(getResourceTypeName(), ret.getId(), ret); // adds logging info - possibly replace with generic update
        } catch (ResourceException e) {
            genericResourceService.delete(getResourceTypeName(), bundle.getId());
            throw e;
        } catch (NoSuchFieldException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Override
    public T update(T bundle, Authentication auth) {
        if (!hasChanged(bundle)) {
            return bundle;
        }
        bundle.markUpdate(UserInfo.of(auth), null); //TODO: make sure all resources will use this
        try {
            return genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasChanged(T bundle) {
        T existing = get(bundle.getId(), bundle.getCatalogueId());
        return !bundle.equals(existing);
    }

    public T validate(T bundle) {
        logger.debug("Validating resource '{}' with id: '{}'", getResourceTypeName(), bundle.getId());
        return genericResourceService.validate(getResourceTypeName(), bundle);
    }

    @Override
    public T audit(String id, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        T existing = get(id, catalogueId);
        existing.markAudit(comment, actionType, auth);

        logger.info("Audited '{}' with ID '{}' [actionType: {}]", getResourceTypeName(), existing.getId(), actionType);
        try {
            genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return existing;
    }

    @Override
    public T setSuspend(String id, String catalogueId, boolean suspend, Authentication auth) {
        T bundle = get(id, catalogueId);
        String resourceOwner = (String) bundle.getPayload().get("resourceOwner");
        suspensionValidation(bundle, resourceOwner, suspend);

        logger.info("{} resource '{}' with id: '{}'", suspend ? "Suspending" : "Unsuspending",
                getResourceTypeName(), bundle.getId());
        bundle.markSuspend(suspend, auth);

        try {
            return genericResourceService.update(getResourceTypeName(), id, bundle);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    //TODO: delete catalogueId if not used
    private void suspensionValidation(Bundle bundle, String resourceOwner, boolean suspend) {
        if (bundle.getMetadata().isPublished()) {
            throw new ResourceException("You cannot directly suspend a Public resource", HttpStatus.FORBIDDEN);
        }
        //TODO: probably remove
//        OrganisationBundle organisationBundle = genericResourceService.get("organisation", resourceOwner);
//        if (organisationBundle.isSuspended() && !suspend) {
//            throw new ResourceException("You cannot unsuspend a Resource when its Provider is suspended",
//                    HttpStatus.CONFLICT);
//        }

        //TODO: enable if Catalogues return.
//        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId, auth);
//        if (bundle instanceof OrganisationBundle) {
//            if (catalogueBundle.isSuspended() && !suspend) {
//                throw new ResourceException("You cannot unsuspend a Provider when its Catalogue is suspended",
//                        HttpStatus.CONFLICT);
//            }
//        } else {
//            if (providerId != null && !providerId.isEmpty()) {
//                OrganisationBundle OrganisationBundle = providerService.get(providerId, catalogueId);
//                if ((catalogueBundle.isSuspended() || OrganisationBundle.isSuspended()) && !suspend) {
//                    throw new ResourceException("You cannot unsuspend a Resource when its Provider and/or Catalogue are suspended",
//                            HttpStatus.CONFLICT);
//                }
//            }
//        }
    }

    @Override
    public Paging<T> getRandomResourcesForAuditing(int quantity, int auditingInterval, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getResourceTypeName());
        ff.setQuantity(10000);
        ff.addFilter("status", "approved");
        ff.addFilter("published", false);
        ff.addFilter("draft", false);

        Browsing<T> resourcesBrowsing = getAll(ff, auth);
        List<T> resourcesToBeAudited = new ArrayList<>();

        long todayEpochMillis = System.currentTimeMillis();
        long intervalEpochSeconds = Instant.ofEpochMilli(todayEpochMillis)
                .atZone(ZoneId.systemDefault())
                .minusMonths(auditingInterval)
                .toEpochSecond();

        for (T bundle : resourcesBrowsing.getResults()) {
            LoggingInfo auditInfo = bundle.getLatestAuditInfo();
            if (auditInfo == null) {
                // Include providers that have never been audited
                resourcesToBeAudited.add(bundle);
            } else {
                try {
                    long auditEpochSeconds = Long.parseLong(auditInfo.getDate());
                    if (auditEpochSeconds < intervalEpochSeconds) {
                        // Include providers that were last audited before the threshold
                        resourcesToBeAudited.add(bundle);
                    }
                } catch (NumberFormatException ignore) {
                }
            }
        }

        // Shuffle the list randomly
        Collections.shuffle(resourcesToBeAudited);

        // Limit the list to the requested quantity
        if (resourcesToBeAudited.size() > quantity) {
            resourcesToBeAudited = resourcesToBeAudited.subList(0, quantity);
        }

        return new Browsing<>(resourcesToBeAudited.size(), 0, resourcesToBeAudited.size(), resourcesToBeAudited,
                resourcesBrowsing.getFacets());
    }

    @Override
    public T addDraft(T bundle, Authentication auth) {
        bundle.markDraft(auth, null);
        this.createIdentifiers(bundle, getResourceTypeName(), false);
        bundle.setId(bundle.getIdentifiers().getOriginalId());

        return genericResourceService.add(getResourceTypeName(), bundle, false);
    }

    @Override
    public T updateDraft(T bundle, Authentication auth) {
        bundle.markUpdate(UserInfo.of(auth), null);
        try {
            return genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle, false);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new ResourceException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteDraft(T bundle) {
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    public T finalizeDraft(T t, Authentication auth) {
        t = workflowService.onboard(getResourceTypeName(), t, auth);
        return update(t, auth);
    }

    //region helper
    public void blockResourceDeletion(String status, boolean isPublished) {
        if (status.equals(vocabularyService.get("pending").getId())) {
            throw new ResourceException("You cannot delete a Template that is under review", HttpStatus.FORBIDDEN);
        }
        if (isPublished) {
            throw new ResourceException("You cannot directly delete a Public Resource", HttpStatus.FORBIDDEN);
        }
    }
    //endregion

    //region unused
    @Override
    public String createId(T bundle) {
        return "";
    }

    @Override
    public T save(T bundle) {
        return null;
    }

    @Override
    public Map<String, List<T>> getBy(String field) {
        return Map.of();
    }

    @Override
    public List<T> getSome(String... ids) {
        return List.of();
    }

    @Override
    public List<T> delAll() {
        return List.of();
    }

    @Override
    public Resource getResource(String id) {
        return null;
    }

    @Override
    public Resource getResource(String id, String catalogueId) {
        return null;
    }

    @Override
    public boolean exists(T bundle) {
        return false;
    }

    @Override
    public boolean exists(String id) {
        return false;
    }
    //endregion
}
