/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.manager.aspects;

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;

@Profile("beyond")
@Aspect
@Component
public class PostProcessingAspect {

    private static final Logger logger = LoggerFactory.getLogger(PostProcessingAspect.class);

    private final Map<String, Consumer<Object>> aspectRegistry = new HashMap<>();
    private final VocabularyService vocabularyService;
//    private final EmailService emailService;
    private final GenericResourceService genericResourceService;


    //TODO: one class per aspect else it will get messy
    public PostProcessingAspect(VocabularyService vocabularyService,
//                                EmailService emailService,
                                GenericResourceService genericResourceService) {
        this.vocabularyService = vocabularyService;
//        this.emailService = emailService;
        this.genericResourceService = genericResourceService;
        aspectRegistry.put("HostingLegalEntityVocabularyUpdate", obj -> {
            if (!(obj instanceof ProviderBundle bundle)) {
                logger.debug("Skipping HostingLegalEntityVocabularyUpdate – object is {}", obj.getClass());
                return;
            }
            checkAndAddProviderToHLEVocabularyAspect(bundle);
        });
        aspectRegistry.put("AfterProviderDeletionEmails", obj -> {
            if (!(obj instanceof ProviderBundle bundle)) {
                logger.debug("Skipping AfterProviderDeletionEmails – object is {}", obj.getClass());
                return;
            }
            notifyProviderAdminsForProviderDeletionAspect(bundle);
        });
        aspectRegistry.put("AfterServiceUpdateEmails", obj -> {
            if (!(obj instanceof ServiceBundle bundle)) {
                logger.debug("Skipping AfterServiceUpdateEmails – object is {}", obj.getClass());
                return;
            }
            notifyPortalAdminsForInvalidServiceUpdateAspect(bundle);
        });
    }

    @AfterReturning(pointcut = "@annotation(triggersAspects)", returning = "result")
    public void afterReturningAdvice(JoinPoint joinPoint, TriggersAspects triggersAspects, Object result) {
        Arrays.stream(triggersAspects.value())
                .forEach(aspectName -> {
                    Consumer<Object> logic = aspectRegistry.get(aspectName);
                    if (logic != null) {
                        logic.accept(result);
                    }
                });
    }

    @Around("@annotation(triggersAspects)")
    public Object aroundUpdateEmailsAdvice(ProceedingJoinPoint pjp, TriggersAspects triggersAspects) throws Throwable {
        boolean shouldSendEmails = Arrays.asList(triggersAspects.value())
                .contains("AfterProviderUpdateEmails");

        Object[] args = pjp.getArgs();
        ProviderBundle incomingBundle = null;
        for (Object arg : args) {
            if (arg instanceof ProviderBundle bundle) {
                incomingBundle = bundle;
                break;
            }
        }
        if (incomingBundle == null) {
            return pjp.proceed(); // nothing to do
        }

        ProviderBundle existingProvider = null;
        try {
            existingProvider = genericResourceService.get("provider", incomingBundle.getId());
        } catch (Exception e) {
            logger.warn("Could not retrieve existing provider bundle for emails: {}", e.getMessage());
        }

        Object result = pjp.proceed();
        if (shouldSendEmails && result instanceof ProviderBundle updatedProvider && existingProvider != null) {
            logger.info("Sending emails regarding changes to Provider Admins and Provider audit state.");
            sendEmailsAfterProviderUpdate(updatedProvider, existingProvider);
        }

        return result;
    }

    private void checkAndAddProviderToHLEVocabularyAspect(ProviderBundle bundle) {
        // TODO: field type (in model) should be 'boolean', not radio with "true"/"false" values.
        boolean legalEntity = switch (bundle.getProvider().get("legalEntity")) {
            case Boolean value -> value;
            case String str -> Boolean.parseBoolean(str);
            default -> throw new ValidationException("Error in field 'legalEntity', should be boolean.");
        };
        if (bundle.getStatus().equalsIgnoreCase("approved") && legalEntity) {
            addOrUpdateProviderHLEVocabulary(bundle.getId(), (String) bundle.getProvider().get("name"), bundle.getCatalogueId());
        }
    }

    private void addOrUpdateProviderHLEVocabulary(String id, String name, String catalogueId) {
        String hleId = createProviderHLEId(id);
        Vocabulary hle;
        try {
            hle = vocabularyService.get(hleId);
            if (!hle.getName().equals(name)) {
                hle.setName(name);
                logger.info("Updating Hosting Legal Entity Vocabulary [{}] with new name: [{}]",
                        hle.getId(), hle.getName());
                vocabularyService.update(hle, null);
            }
        } catch (ResourceException e) {
            hle = new Vocabulary();
            hle.setId(hleId);
            hle.setName(name);
            hle.setType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY.getKey());
            hle.setExtras(new HashMap<>() {{
                put("catalogueId", catalogueId);
            }});
            logger.info("Creating a new Hosting Legal Entity Vocabulary with id: [{}] and name: [{}]",
                    hle.getId(), hle.getName());
            vocabularyService.add(hle, null);
        }
    }

    private String createProviderHLEId(String id) {
        return "%s-%s".formatted(
                Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY.getKey().toLowerCase().replace(" ", "_"), id);
    }

    private void sendEmailsAfterProviderUpdate(ProviderBundle updatedProvider, ProviderBundle existingProvider) {
        sendEmailsForAdminDifferences(updatedProvider, existingProvider);
        sendEmailsForAuditInfo(updatedProvider);
    }

    private void sendEmailsForAdminDifferences(ProviderBundle updatedProvider, ProviderBundle existingProvider) {
        List<List<String>> differences = calculateDifferences(updatedProvider, existingProvider);
        sendEmailsToProviderAdmins(differences);
    }

    private List<List<String>> calculateDifferences(ProviderBundle updatedProvider, ProviderBundle existingProvider) {
        List<String> existingAdmins = extractEmails(existingProvider);
        List<String> newAdmins = extractEmails(updatedProvider);
        List<String> adminsAdded = new ArrayList<>(newAdmins);
        adminsAdded.removeAll(existingAdmins);
        List<String> adminsDeleted = new ArrayList<>(existingAdmins);
        adminsDeleted.removeAll(newAdmins);

        List<List<String>> differences = new ArrayList<>();
        differences.add(adminsAdded);
        differences.add(adminsDeleted);
        return differences;
    }

    private List<String> extractEmails(ProviderBundle providerBundle) {
        List<String> emails = new ArrayList<>();

        Object usersObj = providerBundle.getProvider().get("users"); //TODO: how to enforce that users will be always in the model
        if (usersObj instanceof Collection<?>) {
            for (Object obj : (Collection<?>) usersObj) {
                if (obj instanceof User user) {
                    emails.add(user.getEmail().toLowerCase());
                }
            }
        }
        return emails;
    }

    private void sendEmailsToProviderAdmins(List<List<String>> differences) {
        if (!differences.getFirst().isEmpty()) {
//            emailService.sendEmailsToNewlyAddedProviderAdmins(updatedProvider, adminsAdded); //TODO: fix & enable
        }
        if (!differences.getLast().isEmpty()) {
//            emailService.sendEmailsToNewlyDeletedProviderAdmins(existingProvider, adminsDeleted); //TODO: fix & enable
        }
    }

    private void sendEmailsForAuditInfo(ProviderBundle updatedProvider) {
        if (updatedProvider.getLatestAuditInfo() != null &&
                LoggingInfo.ActionType.INVALID.getKey().equals(updatedProvider.getLatestAuditInfo().getActionType())) {
            long latestAudit = Long.parseLong(updatedProvider.getLatestAuditInfo().getDate());
            long latestUpdate = Long.parseLong(updatedProvider.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate) {
//                emailService.notifyPortalAdminsForInvalidProviderUpdate(bundle); //TODO: fix & enable
            }
        }
    }

    private void notifyProviderAdminsForProviderDeletionAspect(ProviderBundle bundle) {
//        emailService.notifyProviderAdminsForProviderDeletion(bundle); //TODO: fix & enable
    }

    private void notifyPortalAdminsForInvalidServiceUpdateAspect(ServiceBundle bundle) {
        if (bundle.getLatestAuditInfo() != null && bundle.getLatestUpdateInfo() != null) {
            long latestAudit = Long.parseLong(bundle.getLatestAuditInfo().getDate());
            long latestUpdate = Long.parseLong(bundle.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && bundle.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
//                emailService.notifyPortalAdminsForInvalidServiceUpdate(bundle); //FIXME
            }
        }
    }
}
