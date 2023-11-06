<p>Dear Sir/Madam,</p>
<p>
    <#if action == "post">
        ${resourceName} with id: [${monitoringBundle.monitoring.serviceId}] of the [${monitoringBundle.catalogueId}]
        Catalogue has created a Monitoring extension with id: [${monitoringBundle.monitoring.id}] with the following
        information:
        <br>
        [${monitoringBundle.monitoring}]
    </#if>
    <#if action == "put">
        ${resourceName} with id: [${monitoringBundle.monitoring.serviceId}] of the [${monitoringBundle.catalogueId}]
        Catalogue updated its Monitoring extension with id: [${monitoringBundle.monitoring.id}]
        Updated Monitoring has the following information:
        <br>
        [${monitoringBundle.monitoring}]
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>