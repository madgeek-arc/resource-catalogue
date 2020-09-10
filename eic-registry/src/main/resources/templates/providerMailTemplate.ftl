Dear ${user.fullName},

<#if providerBundle.status == "pending initial approval">
You have applied for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) as a new ${project} provider in the ${project} Portal.
Your application will be reviewed and you will be notified on its approval or rejection, as well as for any further steps you can follow for registering successfully your organization and its services.
Thank you for your interest in becoming a member of the ${project} Portal.
</#if>
<#if providerBundle.status == "pending service template submission">
You have applied for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) as a new ${project} Provider in the ${project} Portal.
Your application has been approved and you may proceed with providing one of your Resources ${endpoint}/provider/my, which can help us assess the type of Resources you want to offer.
The Resource should be described according to the ${project} Resource Description Template; the specification according to which the ${project} portal is populated.
The latest version of the ${project} RDT […]. The RDT offers guidance, recommendations, best practices and classifications to facilitate the Resource description.
For any clarifications, please contact us at ${registrationEmail}.
</#if>
<#if providerBundle.status == "pending service template approval">
You have applied for registering [${service.name}] - [${service.id}] as a new Resource in the ${project} Portal.
The Resource description will be reviewed and you will be notified on its approval or rejection, as well as for any further steps you can follow for registering successfully your Resources.
Thank you for your interest in becoming a member of the ${project} Portal.
</#if>
<#if providerBundle.status == "approved">
    <#if providerBundle.active == true>
You have applied for registering [${service.name}] - [${service.id}] as a new Resource in the ${project} Portal.
Your application has been approved and the Resource has been successfully listed in the ${project} Portal. You can view the published Resource here ${endpoint}/service/${service.id}.
[Attached you may find a Resource Description Recommendations Report to further improve your Resource description.]
You may now proceed with registering more Resources for [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]). You can register all other Resources either manually (via the same web interface) or via the ${project} Portal API ${endpoint}/developers.
For any clarifications, please contact us at ${registrationEmail}.
Thank you for your interest in becoming a member of the ${project} Portal.
    <#else>
Your Resource Provider [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been set to inactive.
For any further clarifications please contact ${registrationEmail}.
    </#if>
</#if>
<#if providerBundle.status == "rejected service template">
You have applied for registering [${service.name}] - [${service.id}] as a new Resource in the ${project} Portal.
Your application has been rejected, as your Resource description does not comply with the Rules of Participation of the ${project} Portal (...).
[Attached you may find a Resource Description Recommendations Report].
[Option 1: We would like to offer an information webinar or training session to improve your application. If you agree or for any other clarifications, please contact us at ${registrationEmail}]
[Option 2: For any clarifications, please contact us at ${registrationEmail}].
Thank you for your interest in becoming a member of the EOSC Portal.
</#if>
<#if providerBundle.status == "rejected">
You have applied for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) as a new ${project} Provider in the ${project} Portal.
Your application has been rejected, as your organization does not comply with the Rules of Participation of the EOSC Portal (...).
[Option 1: We would like to offer an information webinar or training session to improve your application. If you agree or for any other clarifications, please contact us at ${registrationEmail}].
[Option 2: For any clarifications, please contact us at ${registrationEmail}].
Thank you for your interest in becoming a member of the ${project}.
</#if>

Best Regards,
the ${project} Portal Onboarding Team
