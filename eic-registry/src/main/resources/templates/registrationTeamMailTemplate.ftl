Dear ${project} Portal Onboarding Team,

<#if providerBundle.status == "pending initial approval">
A new application by [${user.name}] – [${user.email}] has been received for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) as a new ${project} Provider in ${project} Portal.
You can review the application here ${endpoint}/serviceProvidersList and approve or reject it.
</#if>
<#if providerBundle.status == "pending service template submission">
The application by [${user.name}] – [${user.email}] for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been approved. You can view the application status here ${endpoint}/serviceProvidersList
</#if>
<#if providerBundle.status == "pending service template approval">
A new application by [${user.name}] – [${user.email}] has been received for registering [${service.name}] - [${service.id}], as a new Resource of [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]).
You can review the application here ${endpoint}/serviceProvidersList and approve or reject it.
</#if>
<#if providerBundle.status == "approved">
    <#if providerBundle.active == true>
The application by [${user.name}] – [${user.email}] for registering [${service.name}] - ([${service.id}]) of [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been approved.
You can view the application status here ${endpoint}/serviceProvidersList.
    <#else>
The Resource Provider [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been set to inactive.
You can view the application status here ${endpoint}/serviceProvidersList.
    </#if>
</#if>
<#if providerBundle.status == "rejected service template">
The service: [${service.id}] provided by [${user.name}] – [${user.email}] has been rejected.
You can view the application status ${endpoint}/serviceProvidersList.
</#if>
<#if providerBundle.status == "rejected">
The application by [${user.name}] – [${user.email}] for registering [${service.name}] - ([${service.id}]) of [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been rejected.
You can view the application status here ${endpoint}/serviceProvidersList.
</#if>

Best Regards,
the ${project} Team
