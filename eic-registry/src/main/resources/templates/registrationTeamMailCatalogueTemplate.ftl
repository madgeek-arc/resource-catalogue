Dear ${project} Portal Onboarding Team,

<#if catalogueBundle.status == "pending catalogue">
    A new application by [${user.fullName}] – [${user.email}] has been received for registering [${catalogueBundle.catalogue.name}] - ([${catalogueBundle.catalogue.id}]) as a new ${project} Catalogue in ${project} Portal.
    You can review the application here ${endpoint}/dashboard/${catalogueBundle.catalogue.id}/info and approve or reject it.
</#if>
<#if catalogueBundle.status == "approved catalogue">
    <#if catalogueBundle.active == true>
        The application by [${user.fullName}] – [${user.email}] for registering [${catalogueBundle.catalogue.name}] - ([${catalogueBundle.catalogue.id}]) has been approved.
        You can view the application status here ${endpoint}/dashboard/${catalogueBundle.catalogue.id}/info.
    <#else>
        The Catalogue [${catalogueBundle.catalogue.name}] - ([${catalogueBundle.catalogue.id}]) has been set to inactive.
        You can view the application status here ${endpoint}/dashboard/${catalogueBundle.catalogue.id}/info.
    </#if>
</#if>
<#if catalogueBundle.status == "rejected catalogue">
    The application by [${user.fullName}] – [${user.email}] for registering [${catalogueBundle.catalogue.name}] - ([${catalogueBundle.catalogue.id}]) has been rejected.
    You can view the application status here ${endpoint}/dashboard/${catalogueBundle.catalogue.id}/info.
</#if>

Best Regards,
the ${project} Team
