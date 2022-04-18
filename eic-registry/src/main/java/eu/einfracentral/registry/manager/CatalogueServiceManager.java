//package eu.einfracentral.registry.manager;
//
//import eu.einfracentral.domain.*;
//import eu.einfracentral.exception.ValidationException;
//import eu.einfracentral.registry.service.CatalogueServiceService;
//import eu.einfracentral.utils.SortUtils;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static eu.einfracentral.config.CacheConfig.CACHE_FEATURED;
//import static eu.einfracentral.config.CacheConfig.CACHE_PROVIDERS;
//
//@Service("CatalogueServiceManager")
//public class CatalogueServiceManager extends ResourceManager<InfraService> implements CatalogueServiceService<InfraService, Authentication> {
//
//    private static final Logger logger = LogManager.getLogger(CatalogueServiceManager.class);
//
//    @Override
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddServices(#auth, #infraService)")
//    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
//    public InfraService addService(InfraService infraService, Authentication auth) {
//        // check if Provider is approved
//        if (!resourceManager.get(infraService.getService().getResourceOrganisation()).getStatus().equals(vocabularyService.get("approved provider").getId())){
//            throw new ValidationException(String.format("The Provider '%s' you provided as a Resource Organisation is not yet approved",
//                    infraService.getService().getResourceOrganisation()));
//        }
//
//        if ((infraService.getService().getId() == null) || ("".equals(infraService.getService().getId()))) {
//            String id = idCreator.createServiceId(infraService.getService());
//            infraService.getService().setId(id);
//        }
//        validate(infraService);
//        validateEmailsAndPhoneNumbers(infraService);
//        if (resourceManager.get(infraService.getService().getResourceOrganisation()).getTemplateStatus().equals(vocabularyService.get("approved template").getId())){
//            infraService.setActive(true);
//        } else{
//            infraService.setActive(false);
//        }
//        infraService.setLatest(true);
//
//        if (infraService.getMetadata() == null) {
//            infraService.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
//        }
//
//        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
//                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
//        List<LoggingInfo> loggingInfoList = new ArrayList<>();
//        loggingInfoList.add(loggingInfo);
//
//        // latestOnboardingInfo
//        infraService.setLatestOnboardingInfo(loggingInfo);
//
//        infraService.getService().setGeographicalAvailabilities(SortUtils.sort(infraService.getService().getGeographicalAvailabilities()));
//        infraService.getService().setResourceGeographicLocations(SortUtils.sort(infraService.getService().getResourceGeographicLocations()));
//
//        // resource status & extra loggingInfo for Approval
//        ProviderBundle providerBundle = resourceManager.get(infraService.getService().getResourceOrganisation());
//        if (providerBundle.getTemplateStatus().equals("approved template")){
//            infraService.setStatus(vocabularyService.get("approved resource").getId());
//            LoggingInfo loggingInfoApproved = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
//                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
//            loggingInfoList.add(loggingInfoApproved);
//
//            // latestOnboardingInfo
//            infraService.setLatestOnboardingInfo(loggingInfoApproved);
//        } else{
//            infraService.setStatus(vocabularyService.get("pending resource").getId());
//        }
//
//        // LoggingInfo
//        infraService.setLoggingInfo(loggingInfoList);
//
//        // catalogueId
//        infraService.getService().setCatalogueId("eosc");
//
//        logger.info("Adding Service: {}", infraService);
//        InfraService ret;
//        ret = super.add(infraService, auth);
//
//        return ret;
//    }
//}
