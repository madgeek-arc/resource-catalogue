<p>Dear Sir/Madam,</p>
<p>
    <#if action == "post">
        Service with id: [${bundle.datasource.serviceId}] of the [${bundle.datasource.catalogueId}]
        Catalogue has created a Datasource extension with id: [${bundle.datasource.id}].
        <br>
        Please review the submission and approve or reject it.
    </#if>
    <#if action == "put">
        Service with id: [${bundle.datasource.serviceId}] of the [${bundle.datasource.catalogueId}]
        Catalogue updated its Datasource extension with id: [${bundle.datasource.id}].
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>