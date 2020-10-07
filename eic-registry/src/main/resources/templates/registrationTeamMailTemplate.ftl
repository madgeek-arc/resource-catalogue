Dear ${project} Portal Onboarding Team,

<#if providerBundle.status == "pending initial approval">
A new application by [${user.fullName}] – [${user.email}] has been received for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) as a new ${project} Provider in ${project} Portal.
You can review the application here ${endpoint}/provider/all and approve or reject it.
</#if>
<#if providerBundle.status == "pending template submission">
The application by [${user.fullName}] – [${user.email}] for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been approved.
You can view the application status here ${endpoint}/provider/all.
</#if>
<#if providerBundle.status == "pending template approval">
A new application by [${user.fullName}] – [${user.email}] has been received for registering [${service.name}] - [${service.id}], as a new ${serviceOrResource} of [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]).
You can review the application here ${endpoint}/provider/all and approve or reject it.
</#if>
<#if providerBundle.status == "approved">
    <#if providerBundle.active == true>
The application by [${user.fullName}] – [${user.email}] for registering [${service.name}] - ([${service.id}]) of [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been approved.
You can view the application status here ${endpoint}/provider/all.
    <#else>
The ${serviceOrResource} Provider [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been set to inactive.
You can view the application status here ${endpoint}/provider/all.
    </#if>
</#if>
<#if providerBundle.status == "rejected template">
The ${serviceOrResource}: [${service.id}] provided by [${user.fullName}] – [${user.email}] has been rejected.
You can view the application status ${endpoint}/provider/all.
</#if>
<#if providerBundle.status == "rejected">
The application by [${user.fullName}] – [${user.email}] for registering [${service.name}] - ([${service.id}]) of [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been rejected.
You can view the application status here ${endpoint}/provider/all.
</#if>

Best Regards,
the ${project} Team
