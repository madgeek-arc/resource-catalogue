Dear Sir/Madam,

<#if action == "post">
    Service with id: [${monitoringBundle.monitoring.serviceId}] of the [${monitoringBundle.catalogueId}] Catalogue has created a Monitoring extension with id: [${monitoringBundle.monitoring.id}] with the following information:
    [${monitoringBundle.monitoring}]
</#if>
<#if action == "put">
    Service with id: [${monitoringBundle.monitoring.serviceId}] of the [${monitoringBundle.catalogueId}] Catalogue updated its Monitoring extension with id: [${monitoringBundle.monitoring.id}]
    Updated Monitoring has the following information:
    [${monitoringBundle.monitoring}]
</#if>

Best Regards,
the ${project} Team