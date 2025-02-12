<p>Dear Sir/Madam,</p>
<p>
    <#if action == "post">
        Resource with id: [${bundle.monitoring.serviceId}] of the [${bundle.catalogueId}]
        Catalogue has created a Monitoring extension with id: [${bundle.monitoring.id}] with the following
        information:
        <br>
        [${bundle.monitoring}]
    </#if>
    <#if action == "put">
        Resource with id: [${bundle.monitoring.serviceId}] of the [${bundle.catalogueId}]
        Catalogue updated its Monitoring extension with id: [${bundle.monitoring.id}]
        Updated Monitoring has the following information:
        <br>
        [${bundle.monitoring}]
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>