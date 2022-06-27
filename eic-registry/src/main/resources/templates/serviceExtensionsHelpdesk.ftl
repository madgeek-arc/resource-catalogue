Dear Sir/Madam,

<#if action == "post">
    Service with id: [${helpdeskBundle.helpdesk.serviceId}] of the [${helpdeskBundle.catalogueId}] Catalogue has created a Helpdesk extension with id: [${helpdeskBundle.helpdesk.id}] with the following information:
    [${helpdeskBundle.helpdesk}]
</#if>
<#if action == "put">
    Service with id: [${helpdeskBundle.helpdesk.serviceId}] of the [${helpdeskBundle.catalogueId}] Catalogue updated its Helpdesk extension with id: [${helpdeskBundle.helpdesk.id}]
    Updated Helpdesk has the following information:
    [${helpdeskBundle.helpdesk}]
</#if>

Best Regards,
the ${project} Team