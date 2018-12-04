Dear ${user.name},

<#if provider.status == "pending initial approval">
You have applied for registering your organization [${provider.id}] - [${provider.name}] as a new service provider in eInfraCentral.
Your application will be reviewed by the administration team and you will be notified on its approval or rejection, as well as for any further steps you can follow for registering successfully your organization.
</#if>
<#if provider.status == "pending service template approval">
You have applied for registering your organization [${provider.id}] - [${provider.name}] as a new service provider in eInfraCentral.
Your application has been approved and you may proceed with providing one of your services ${endpoint}/newServiceProvider, which can help eInfraCentral assess the type of the service you want to offer.
The service should be described according to the eInfraCentral’s Service Description Template (SDT), which has been adopted by flagship initiatives such as the EOSC-hub (EGI, EUDAT), GÉANT, OpenAIRE(-Advance) and PRACE, as the standard with which EOSC portal will be populated.
The latest version of the SDT is available here [https://legacy.gitbook.com/@jnp]. The SDT offers recommendations, best practices for all service attributes and a classification of scientific resources in categories and subcategories to allow for the description of any resource in a catalogue.
</#if>
<#if provider.status == "service template submission">
You have provided a new service [${service.id}] – [${service.name}] in eInfraCentral.
The information provided will be reviewed by the administration team and you will be notified on its approval or rejection, as well as for any further steps you can follow for registering successfully your services.
</#if>
<#if provider.status == "approved">
The information for the service [${service.id}] – [${service.name}] has been reviewed and has been successfully added to the eInfraCentral Catalogue. You can view the new service ${endpoint}/service/${service.id}. You may now login and proceed with providing more services for [${provider.id}].
</#if>
<#if provider.status == "rejected service template">
The information for the service [${service.id}] – [${service.name}] has been reviewed and unfortunately does not comply with the SDT and the type of services being published in eInfraCentral Catalogue. For any further clarifications please contact registration office of eInfraCentral at regitration@eInfraCentral.eu.
</#if>
<#if provider.status == "rejected">
You have applied for registering your organization [${provider.id}] - [${provider.name}] as a new service provider in eInfraCentral.
Your application has been rejected as your organization does not comply with the rules of participations in eInfraCentral. For any further clarifications please contact registration office of eInfraCentral at regitration@eInfraCentral.eu.
</#if>
<#--<#if provider.status == "provider template registration">-->
<#--You have applied for registering your organization [${provider.id}] - [${provider.name}] as a new service provider in eInfraCentral.-->
<#--Your application will be reviewed by the administration team and you will be notified on its approval or rejection, as well as for any further steps you can follow for registering successfully your organization.-->
<#--</#if>-->
<#--<#if provider.status == "provider template approved">-->
<#--Your application for registering your organization [${provider.id}] - [${provider.name}] has been approved and you may proceed with providing one of your services ${endpoint}/newServiceProvider, which can help eInfraCentral assess the type of the service you want to offer.-->
<#--The service should be described according to the eInfraCentral’s Service Description Template (SDT), which has been adopted by flagship initiatives such as the EOSC-hub (EGI, EUDAT), GÉANT, OpenAIRE(-Advance) and PRACE, as the standard with which EOSC portal will be populated.-->
<#--The latest version of the SDT is available here [https://legacy.gitbook.com/@jnp]. The SDT offers recommendations, best practices for all service attributes and a classification of scientific resources in categories and subcategories to allow for the description of any resource in a catalogue.-->
<#--</#if>-->
<#--<#if provider.status == "provider template rejected">-->
<#--Your application for registering your organization [${provider.id}] - [${provider.name}] has been rejected as your organization does not comply with the rules of participations in eInfraCentral. For any further clarifications please contact registration office of eInfraCentral at regitration@eInfraCentral.eu.-->
<#--</#if>-->
<#--<#if provider.status == "service template registration">-->
<#--You have provided a new service [${service.id}] – [${service.name}] in eInfraCentral.-->
<#--The information provided will be reviewed by the administration team and you will be notified on its approval or rejection, as well as for any further steps you can follow for registering successfully your services.-->
<#--</#if>-->
<#--<#if provider.status == "service template approved">-->
<#--The information for the service [${service.id}] – [${service.name}] has been reviewed and has been successfully added to the eInfraCentral Catalogue. You can view the new service ${endpoint}/service/${service.id}. You may now login and proceed with providing more services for [${provider.id}].-->
<#--</#if>-->
<#--<#if provider.status == "service template rejected">-->
<#--The information for the service [${service.id}] – [${service.name}] has been reviewed and unfortunately does not comply with the SDT and the type of services being published in eInfraCentral Catalogue. For any further clarifications please contact registration office of eInfraCentral at regitration@eInfraCentral.eu.-->
<#--</#if>-->

Thank you for your interest in becoming a member of eInfraCentral.

Best Regards,
the eInfraCentral Team
