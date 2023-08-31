Dear Sir/Madam,

<#if action == "post">
    Service with id: [${datasourceBundle.datasource.serviceId}] of the [${datasourceBundle.datasource.catalogueId}] Catalogue has created a Datasource extension with id: [${datasourceBundle.datasource.id}].
    Please review the submission and approve or reject it.
</#if>
<#if action == "put">
    Service with id: [${datasourceBundle.datasource.serviceId}] of the [${datasourceBundle.datasource.catalogueId}] Catalogue updated its Datasource extension with id: [${datasourceBundle.datasource.id}]
    Updated Datasource has the following information:
    [${datasourceBundle.datasource}]
</#if>

Best Regards,
the ${project} Team