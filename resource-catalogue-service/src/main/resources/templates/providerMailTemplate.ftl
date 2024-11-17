<p>Dear ${user.fullName},</p>
<p>
    <#if bundle.templateStatus == "no template status">
        <#if bundle.status == "pending provider">
            You have applied for registering [${bundle.provider.name}] - ([${bundle.provider.id}]) as a
            new ${project} Provider in the ${project} Portal.
            <br>
            Your application will be reviewed and you will be notified on its approval or rejection, as well as for any
            further steps you can follow for registering successfully your organization and its services.
            <br>
            Thank you for your interest in becoming a member of the ${project} Portal.
        </#if>
        <#if bundle.status == "approved provider">
            <#if bundle.active == true>
                You have applied for registering [${bundle.provider.name}] - ([${bundle.provider.id}])
                as a new ${project} Provider in the ${project} Portal.
                <br>
                Your application has been approved and you may proceed with providing one of your Resources
                ${endpoint}/provider/my, which can help us assess the type of Resources you want to offer.
                <br>
                The Resource should be described according to the ${project} Resource Description Template;
                the specification according to which the ${project} portal is populated.
                <br>
                The Resource Description Template offers guidance, recommendations, best practices and classifications
                to facilitate the Resource description.
                <br>
                For any clarifications, please contact us at ${registrationEmail}.
            <#else>
                Your Resource Provider [${bundle.provider.name}] - ([${bundle.provider.id}]) has been
                set to inactive.
                <br>
                For any further clarifications please contact ${registrationEmail}.
            </#if>
        </#if>
        <#if bundle.status == "rejected provider">
            You have applied for registering [${bundle.provider.name}] - ([${bundle.provider.id}]) as a
            new ${project} Provider in the ${project} Portal.
            <br>
            Your application has been rejected, as your organization does not comply with the Rules of Participation
            of the ${project} Portal.
            <br>
            [Option 1: We would like to offer an information webinar or training session to improve your application.
            If you agree or for any other clarifications, please contact us at ${registrationEmail}].
            <br>
            [Option 2: For any clarifications, please contact us at ${registrationEmail}].
            <br>
            Thank you for your interest in becoming a member of the ${project} Portal.
        </#if>
    <#else>
        <#if bundle.templateStatus == "pending template">
            You have applied for registering [${resourceBundleName}] - [${resourceBundleId}] as a new Resource in the
            ${project} Portal.
            <br>
            The Resource description will be reviewed and you will be notified on its approval or rejection, as well as
            for any further steps you can follow for registering successfully your Resources.
            <br>
            Thank you for your interest in becoming a member of the ${project} Portal.
        </#if>
        <#if bundle.templateStatus == "approved template">
            You have applied for registering [${resourceBundleName}] - [${resourceBundleId}] as a new Resource in the
            ${project} Portal.
            <br>
            Your application has been approved and the Resource has been successfully listed in the ${project} Portal.
            <br>
            You can view the published Resource here
            ${endpoint}/dashboard/${project?lower_case}/${bundle.provider.id}/${resourceType}-dashboard/${resourceBundleId}/stats.
            <br>
            [Attached you may find a Resource Description Recommendations Report to further improve your
            Resource description.]
            <br>
            You may now proceed with registering more Resources for
            [${bundle.provider.name}] - ([${bundle.provider.id}]). You can register all other Resources
            either manually (via the same web interface) or via the ${project} Portal API ${endpoint}/developers.
            <br>
            For any clarifications, please contact us at ${registrationEmail}.
            <br>
            Thank you for your interest in becoming a member of the ${project} Portal.
        </#if>
        <#if bundle.templateStatus == "rejected template">
            You have applied for registering [${resourceBundleName}] - [${resourceBundleId}] as a new Resource in the
            ${project} Portal.
            <br>
            Your application has been rejected, as your Resource description does not comply with the Rules of
            Participation of the ${project} Portal.
            <br>
            [Attached you may find a Resource Description Recommendations Report].
            <br>
            [Option 1: We would like to offer an information webinar or training session to improve your application.
            If you agree or for any other clarifications, please contact us at ${registrationEmail}]
            <br>
            [Option 2: For any clarifications, please contact us at ${registrationEmail}].
            <br>
            Thank you for your interest in becoming a member of the ${project} Portal.
        </#if>
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Portal Onboarding Team
</p>