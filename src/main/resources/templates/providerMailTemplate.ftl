<body>
    <p>Dear ${user.name}</p>
    <br>
    <#if provider.status == "initialized">
        <p>You have applied for registering your organization [${provider.id}] - [${provider.name}] as a new service provider in eInfraCentral.<br>
        You application will be reviewed by the administration team and you will be notified on its<br>
        approval or rejection, as well as for any further steps you can follow for registering successfully<br>
        your organization.</p>
        <br><br>
    </#if>
    <#if provider.status == "pending">
        <p>You have applied for registering your organization [${provider.id}] - [${provider.name}] as a new service provider in eInfraCentral.</p>
        <p>Your application has been approved and you may proceed with providing one of your services [link to add a new service], which can help eInfraCentral assess the type of the service you want to offer.</p>
        <p>The service should be described according to the eInfraCentral’s Service Description Template (SDT), which has been adopted by flagship initiatives such as the EOSC-hub (EGI, EUDAT), GÉANT, OpenAIRE(-Advance) and PRACE, as the standard with which EOSC portal will be populated.</p>
        <p>The latest version of the SDT is available here [https://legacy.gitbook.com/@jnp]. The SDT offers recommendations, best practices for all service attributes and a classification of scientific resources in categories and subcategories to allow for the description of any resource in a catalogue.</p>
    </#if>
    <#if provider.status == "pending 2">
        <p>You have provided a new service [${service.id}] – [${service.name}] in eInfraCentral.
            The information provided will be reviewed by the administration team and you will be notified on its approval or rejection, as well as for any further steps you can follow for registering successfully your services.</p>
    </#if>
    <#if provider.status == "approved">
        <p>The information for the service [${service.id}] – [${service.name}] has been reviewed and has been successfully added to the eInfraCentral Catalogue. You can view the new service <a href="${endpoint}/service/${service.id}">here</a>. You may now login and proceed with providing more services for [${provider.id}].</p>
    </#if>
    <#if provider.status == "rejected_sp">
        <p>The information for the service [${service.id}] – [${service.name}] has been reviewed and unfortunately does not comply with the SDT and the type of services being published in eInfraCentral Catalogue. For any further clarifications please contact registration office of eInfraCentral at regitration@eInfraCentral.eu.</p>
    </#if>
    <#if provider.status == "rejected">
        <p>You have applied for registering your organization [${provider.id}] - [${provider.name}] as a new service provider in eInfraCentral.</p>
        <p>Your application has been rejected as your organization does not comply with the rules of participations in eInfraCentral. For any further clarifications please contact registration office of eInfraCentral at regitration@eInfraCentral.eu.</p>
    </#if>
    <p>Thank you for your interest in becoming a member of eInfraCentral.</p>
    <br>
    <p>Best Regards<br>the eInfraCentral Team</p>
</body>