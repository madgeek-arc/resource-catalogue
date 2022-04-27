Dear ${user.fullName},

<#if catalogueBundle.status == "pending catalogue">
    You have applied for registering [${catalogueBundle.catalogue.name}] - ([${catalogueBundle.catalogue.id}]) as a new ${project} Catalogue in the ${project} Portal.
    Your application will be reviewed and you will be notified on its approval or rejection, as well as for any further steps you can follow for registering successfully your catalogue.
    Thank you for your interest in becoming a member of the ${project} Portal.
</#if>
<#if catalogueBundle.status == "approved catalogue">
    <#if catalogueBundle.active == true>
        You have applied for registering [${catalogueBundle.catalogue.name}] - ([${catalogueBundle.catalogue.id}]) as a new ${project} Catalogue in the ${project} Portal.
        Your application has been approved.
    <#else>
        Your Catalogue [${catalogueBundle.catalogue.name}] - ([${catalogueBundle.catalogue.id}]) has been set to inactive.
        For any further clarifications please contact ${registrationEmail}.
    </#if>
</#if>
<#if catalogueBundle.status == "rejected catalogue">
    You have applied for registering [${catalogueBundle.catalogue.name}] - ([${catalogueBundle.catalogue.id}]) as a new ${project} Catalogue in the ${project} Portal.
    Your application has been rejected, as your catalogue does not comply with the Rules of Participation of the ${project} Portal.
    [Option 1: We would like to offer an information webinar or training session to improve your application. If you agree or for any other clarifications, please contact us at ${registrationEmail}].
    [Option 2: For any clarifications, please contact us at ${registrationEmail}].
    Thank you for your interest in becoming a member of the ${project} Portal.
</#if>

Best Regards,
the ${project} Portal Onboarding Team
