Dear ${project} Portal Onboarding Team,

<#if providerBundle.templateStatus == "no template status">
    <#if providerBundle.status == "pending provider">
        A new application by [${user.fullName}] – [${user.email}] has been received for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) as a new ${project} Provider in ${project} Portal.
        You can review the application here ${endpoint}/dashboard/${project?lower_case}/${providerBundle.provider.id}/info and approve or reject it.
    </#if>
    <#if providerBundle.status == "approved provider">
        <#if providerBundle.active == true>
            The application by [${user.fullName}] – [${user.email}] for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been approved.
            You can view the application status here ${endpoint}/dashboard/${providerBundle.provider.id}/info.
        <#else>
            The Provider [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been set to inactive.
            You can view the application status here ${endpoint}/dashboard/${project?lower_case}/${providerBundle.provider.id}/info.
        </#if>
    </#if>
    <#if providerBundle.status == "rejected provider">
        The application by [${user.fullName}] – [${user.email}] for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been rejected.
        You can view the application status here ${endpoint}/dashboard/${project?lower_case}/${providerBundle.provider.id}/info.
    </#if>
<#else>
    <#if providerBundle.templateStatus == "pending template">
    A new application by [${user.fullName}] – [${user.email}] has been received for registering [${resource.name}] - [${resource.id}], as a new Resource of [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]).
    You can review the application here ${endpoint}/dashboard/${project?lower_case}/${providerBundle.provider.id}/info and approve or reject it.
    </#if>
    <#if providerBundle.templateStatus == "approved template">
    The application by [${user.fullName}] – [${user.email}] for registering [${resource.name}] - ([${resource.id}]) of [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been approved.
    You can view the application status here ${endpoint}/dashboard/${project?lower_case}/${providerBundle.provider.id}/info.
    </#if>
    <#if providerBundle.templateStatus == "rejected template">
    The Resource: [${resource.id}] provided by [${user.fullName}] – [${user.email}] has been rejected.
    You can view the application status ${endpoint}/dashboard/${project?lower_case}/${providerBundle.provider.id}/info.
    </#if>
</#if>

Best Regards,
the ${project} Team
