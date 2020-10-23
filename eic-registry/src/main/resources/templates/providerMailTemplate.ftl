Dear ${user.fullName},

<#if providerBundle.status == "pending initial approval">
You have applied for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) as a new ${project} provider in the ${project} Portal.
Your application will be reviewed and you will be notified on its approval or rejection, as well as for any further steps you can follow for registering successfully your organization and its services.
Thank you for your interest in becoming a member of the ${project} Portal.
</#if>
<#if providerBundle.status == "pending template submission">
You have applied for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) as a new ${project} Provider in the ${project} Portal.
Your application has been approved and you may proceed with providing one of your ${serviceOrResource}s ${endpoint}/provider/my, which can help us assess the type of ${serviceOrResource}s you want to offer.
The ${serviceOrResource} should be described according to the ${project} ${serviceOrResource} Description Template; the specification according to which the ${project} portal is populated.
The ${serviceOrResource} Description Template offers guidance, recommendations, best practices and classifications to facilitate the ${serviceOrResource} description.
For any clarifications, please contact us at ${registrationEmail}.
</#if>
<#if providerBundle.status == "pending template approval">
You have applied for registering [${service.name}] - [${service.id}] as a new ${serviceOrResource} in the ${project} Portal.
The ${serviceOrResource} description will be reviewed and you will be notified on its approval or rejection, as well as for any further steps you can follow for registering successfully your ${serviceOrResource}s.
Thank you for your interest in becoming a member of the ${project} Portal.
</#if>
<#if providerBundle.status == "approved">
    <#if providerBundle.active == true>
You have applied for registering [${service.name}] - [${service.id}] as a new ${serviceOrResource} in the ${project} Portal.
Your application has been approved and the ${serviceOrResource} has been successfully listed in the ${project} Portal. You can view the published ${serviceOrResource} here ${endpoint}/service/${service.id}.
[Attached you may find a ${serviceOrResource} Description Recommendations Report to further improve your ${serviceOrResource} description.]
You may now proceed with registering more ${serviceOrResource}s for [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]). You can register all other ${serviceOrResource}s either manually (via the same web interface) or via the ${project} Portal API ${endpoint}/developers.
For any clarifications, please contact us at ${registrationEmail}.
Thank you for your interest in becoming a member of the ${project} Portal.
    <#else>
Your ${serviceOrResource} Provider [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been set to inactive.
For any further clarifications please contact ${registrationEmail}.
    </#if>
</#if>
<#if providerBundle.status == "rejected template">
You have applied for registering [${service.name}] - [${service.id}] as a new ${serviceOrResource} in the ${project} Portal.
Your application has been rejected, as your ${serviceOrResource} description does not comply with the Rules of Participation of the ${project} Portal.
[Attached you may find a ${serviceOrResource} Description Recommendations Report].
[Option 1: We would like to offer an information webinar or training session to improve your application. If you agree or for any other clarifications, please contact us at ${registrationEmail}]
[Option 2: For any clarifications, please contact us at ${registrationEmail}].
Thank you for your interest in becoming a member of the ${project} Portal.
</#if>
<#if providerBundle.status == "rejected">
You have applied for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) as a new ${project} Provider in the ${project} Portal.
Your application has been rejected, as your organization does not comply with the Rules of Participation of the ${project} Portal.
[Option 1: We would like to offer an information webinar or training session to improve your application. If you agree or for any other clarifications, please contact us at ${registrationEmail}].
[Option 2: For any clarifications, please contact us at ${registrationEmail}].
Thank you for your interest in becoming a member of the ${project} Portal.
</#if>

Best Regards,
the ${project} Portal Onboarding Team
