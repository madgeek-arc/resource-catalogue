package gr.uoa.di.madgik.resourcecatalogue.config;

import gr.uoa.di.madgik.catalogue.domain.Model;
import gr.uoa.di.madgik.catalogue.service.ModelResourceTypeMapper;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class ResourceCatalogueModelMapperConfig {

    @Bean
    ModelResourceTypeMapper modelResourceTypeMapper() {
        return new ModelResourceTypeMapper() {
            @Override
            protected List<IndexField> additionalIndexFields(Model model, ResourceType resourceType) {
                List<IndexField> fields = new ArrayList<>();

                String className = resourceType.getProperties() == null
                        ? null
                        : resourceType.getProperties().get("class");

                if (OrganisationBundle.class.getName().equals(className)) {
                    fields.add(additionalIndexField(resourceType, "templateStatus", "Template Status", "$.templateStatus", String.class.getName(), false));
                    fields.add(additionalIndexField(resourceType, "users", null, "$.organisation.users[*].email", String.class.getName(), true));
                }

                try {
                    Class<?> clazz = Class.forName(className);
                    if (Bundle.class.isAssignableFrom(clazz)) {
                        fields.add(additionalIndexField(resourceType, "catalogueId", "Catalogue ID", "$.catalogueId", String.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "suspended", null, "$.suspended", Boolean.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "status", null, "$.status", String.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "active", null, "$.active", Boolean.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "draft", null, "$.draft", Boolean.class.getName(), false));

                        // metadata
                        fields.add(additionalIndexField(resourceType, "registeredAt", null, "$.metadata.registeredAt", Instant.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "registeredBy", null, "$.metadata.registeredBy", String.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "modifiedAt", null, "$.metadata.modifiedAt", Instant.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "modifiedBy", null, "$.metadata.modifiedBy", String.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "published", null, "$.metadata.published", String.class.getName(), false));

                        // identifiers
                        fields.add(additionalIndexField(resourceType, "originalId", null, "$.identifiers.originalId", String.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "pid", null, "$.identifiers.pid", String.class.getName(), false));

                        // audit info
                        fields.add(additionalIndexField(resourceType, "auditState", null, "$.auditState", String.class.getName(), false));

                        fields.add(additionalIndexField(resourceType, "latestAuditDate", null, "$.latestAuditInfo.date", Instant.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "latestAuditType", null, "$.latestAuditInfo.type", String.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "latestAuditActionType", null, "$.latestAuditInfo.actionType", String.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "latestAuditUserEmail", null, "$.latestAuditInfo.userEmail", String.class.getName(), false));

                        fields.add(additionalIndexField(resourceType, "latestOnboardingDate", null, "$.latestOnboardingInfo.date", Instant.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "latestOnboardingType", null, "$.latestOnboardingInfo.type", String.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "latestOnboardingActionType", null, "$.latestOnboardingInfo.actionType", String.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "latestOnboardingUserEmail", null, "$.latestOnboardingInfo.userEmail", String.class.getName(), false));

                        fields.add(additionalIndexField(resourceType, "latestUpdateDate", null, "$.latestUpdateInfo.date", Instant.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "latestUpdateType", null, "$.latestUpdateInfo.type", String.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "latestUpdateActionType", null, "$.latestUpdateInfo.actionType", String.class.getName(), false));
                        fields.add(additionalIndexField(resourceType, "latestUpdateUserEmail", null, "$.latestUpdateInfo.userEmail", String.class.getName(), false));
                    }
                } catch (ClassNotFoundException e) {
                    throw new ServiceException(e);
                }

                return fields;
            }
        };
    }
}
