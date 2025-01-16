package gr.uoa.di.madgik.resourcecatalogue.domain;

public enum ResourceTypes {
    CATALOGUE,
    CONFIGURATION_TEMPLATE,
    CONFIGURATION_TEMPLATE_INSTANCE,
    DATASOURCE,
    HELPDESK,
    INTEROPERABILITY_RECORD,
    MONITORING,
    PROVIDER,
    RESOURCE_INTEROPERABILITY_RECORD,
    SERVICE,
    TOOL,
    TRAINING_RESOURCE,
    VOCABULARY,
    VOCABULARY_CURATION;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
