<p>Dear ${user.fullName},</p>
<p>
    <#if bundle.status == "pending catalogue">
        You have applied for registering [${bundle.catalogue.name}] - ([${bundle.catalogue.id}]) as a
        new ${project} Catalogue in the ${project}.
        <br>
        Your application will be reviewed and you will be notified on its approval or rejection, as well as for any
        further steps you can follow for registering successfully your catalogue.
        <br>
        Thank you for your interest in becoming a member of the ${project}.
    </#if>
    <#if bundle.status == "approved catalogue">
        <#if bundle.active == true>
            You have applied for registering [${bundle.catalogue.name}] - ([${bundle.catalogue.id}])
            as a new ${project} Catalogue in the ${project}.
            <br>
            Your application has been approved.
        <#else>
            Your Catalogue [${bundle.catalogue.name}] - ([${bundle.catalogue.id}]) has been set to
            inactive.
            <br>
            For any further clarifications please contact ${registrationEmail}.
        </#if>
    </#if>
    <#if bundle.status == "rejected catalogue">
        You have applied for registering [${bundle.catalogue.name}] - ([${bundle.catalogue.id}]) as a
        new ${project} Catalogue in the ${project}.
        <br>
        Your application has been rejected, as your catalogue does not comply with the Rules of Participation of
        the ${project}.
        <br>
        [Option 1: We would like to offer an information webinar or training session to improve your application.
        If you agree or for any other clarifications, please contact us at ${registrationEmail}].
        <br>
        [Option 2: For any clarifications, please contact us at ${registrationEmail}].
        <br>
        Thank you for your interest in becoming a member of the ${project}.
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Onboarding Team
</p>