Dear ${user.fullName},

<#if providerBundle.templateStatus == "no template status">
    <#if providerBundle.status == "pending provider">
        You have applied for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) as a new ${project} provider in the ${project} Portal.
        Your application will be reviewed and you will be notified on its approval or rejection, as well as for any further steps you can follow for registering successfully your organization and its services.
        Thank you for your interest in becoming a member of the ${project} Portal.
    </#if>
    <#if providerBundle.status == "approved provider">
        <#if providerBundle.active == true>
            You have applied for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) as a new ${project} Provider in the ${project} Portal.
            Your application has been approved and you may proceed with providing one of your ${serviceOrDatasource}s ${endpoint}/provider/my, which can help us assess the type of ${serviceOrDatasource}s you want to offer.
            The ${serviceOrDatasource} should be described according to the ${project} ${serviceOrDatasource} Description Template; the specification according to which the ${project} portal is populated.
            The ${serviceOrDatasource} Description Template offers guidance, recommendations, best practices and classifications to facilitate the ${serviceOrDatasource} description.
            For any clarifications, please contact us at ${registrationEmail}.
        <#else>
            Your ${serviceOrDatasource} Provider [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) has been set to inactive.
            For any further clarifications please contact ${registrationEmail}.
        </#if>
    </#if>
    <#if providerBundle.status == "rejected provider">
        You have applied for registering [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]) as a new ${project} Provider in the ${project} Portal.
        Your application has been rejected, as your organization does not comply with the Rules of Participation of the ${project} Portal.
        [Option 1: We would like to offer an information webinar or training session to improve your application. If you agree or for any other clarifications, please contact us at ${registrationEmail}].
        [Option 2: For any clarifications, please contact us at ${registrationEmail}].
        Thank you for your interest in becoming a member of the ${project} Portal.
    </#if>
<#else>
    <#if providerBundle.templateStatus == "pending template">
        You have applied for registering [${resource.name}] - [${resource.id}] as a new ${serviceOrDatasource} in the ${project} Portal.
        The ${serviceOrDatasource} description will be reviewed and you will be notified on its approval or rejection, as well as for any further steps you can follow for registering successfully your ${serviceOrDatasource}s.
        Thank you for your interest in becoming a member of the ${project} Portal.
    </#if>
    <#if providerBundle.templateStatus == "approved template">
            You have applied for registering [${resource.name}] - [${resource.id}] as a new ${serviceOrDatasource} in the ${project} Portal.
            Your application has been approved and the ${serviceOrDatasource} has been successfully listed in the ${project} Portal. You can view the published ${serviceOrDatasource} here ${endpoint}/service/${resource.id}.
            [Attached you may find a ${serviceOrDatasource} Description Recommendations Report to further improve your ${serviceOrDatasource} description.]
            You may now proceed with registering more ${serviceOrDatasource}s for [${providerBundle.provider.name}] - ([${providerBundle.provider.id}]). You can register all other ${serviceOrDatasource}s either manually (via the same web interface) or via the ${project} Portal API ${endpoint}/developers.
            For any clarifications, please contact us at ${registrationEmail}.
            Thank you for your interest in becoming a member of the ${project} Portal.
    </#if>
    <#if providerBundle.templateStatus == "rejected template">
        You have applied for registering [${resource.name}] - [${resource.id}] as a new ${serviceOrDatasource} in the ${project} Portal.
        Your application has been rejected, as your ${serviceOrDatasource} description does not comply with the Rules of Participation of the ${project} Portal.
        [Attached you may find a ${serviceOrDatasource} Description Recommendations Report].
        [Option 1: We would like to offer an information webinar or training session to improve your application. If you agree or for any other clarifications, please contact us at ${registrationEmail}]
        [Option 2: For any clarifications, please contact us at ${registrationEmail}].
        Thank you for your interest in becoming a member of the ${project} Portal.
    </#if>
</#if>

Best Regards,
the ${project} Portal Onboarding Team
