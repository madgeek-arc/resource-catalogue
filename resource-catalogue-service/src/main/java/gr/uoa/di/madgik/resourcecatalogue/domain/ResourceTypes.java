package gr.uoa.di.madgik.resourcecatalogue.domain;

public enum ResourceTypes {
    CATALOGUE,
    PROVIDER,
    SERVICE,
    TRAINING_RESOURCE,
    INTEROPERABILITY_RECORD,
    RESOURCE_INTEROPERABILITY_RECORD,
    DATASOURCE,
    HELPDESK,
    MONITORING,
    CONFIGURATION_TEMPLATE,
    CONFIGURATION_TEMPLATE_INSTANCE,
    VOCABULARY;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
