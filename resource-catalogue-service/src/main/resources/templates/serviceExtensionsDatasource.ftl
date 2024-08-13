<p>Dear Sir/Madam,</p>
<p>
    <#if action == "post">
        Service with id: [${datasourceBundle.datasource.serviceId}] of the [${datasourceBundle.datasource.catalogueId}]
        Catalogue has created a Datasource extension with id: [${datasourceBundle.datasource.id}].
        <br>
        Please review the submission and approve or reject it.
    </#if>
    <#if action == "put">
        Service with id: [${datasourceBundle.datasource.serviceId}] of the [${datasourceBundle.datasource.catalogueId}]
        Catalogue updated its Datasource extension with id: [${datasourceBundle.datasource.id}].
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>