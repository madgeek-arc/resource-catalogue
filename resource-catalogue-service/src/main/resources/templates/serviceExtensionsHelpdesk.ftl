<p>Dear Sir/Madam,</p>
<p>
    <#if action == "post">
        ${resourceName} with id: [${helpdeskBundle.helpdesk.serviceId}] of the [${helpdeskBundle.catalogueId}] Catalogue
        has created a Helpdesk extension with id: [${helpdeskBundle.helpdesk.id}] with the following information:
        <br>
        [${helpdeskBundle.helpdesk}]
    </#if>
    <#if action == "put">
        ${resourceName} with id: [${helpdeskBundle.helpdesk.serviceId}] of the [${helpdeskBundle.catalogueId}] Catalogue
        updated its Helpdesk extension with id: [${helpdeskBundle.helpdesk.id}]
        Updated Helpdesk has the following information:
        <br>
        [${helpdeskBundle.helpdesk}]
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>