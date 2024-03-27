<p>Dear ${project} Portal Onboarding Team,</p>
<p>
    <#if catalogueBundle.status == "pending catalogue">
        A new application by [${user.fullName}] – [${user.email}] has been received for registering
        [${catalogueBundle.catalogue.name}] - ([${catalogueBundle.catalogue.id}]) as a new ${project} Catalogue
        in ${project} Portal.
        <br>
        You can review the application here
        ${endpoint}/catalogue-dashboard/${catalogueBundle.catalogue.id}/info and approve or reject it.
    </#if>
    <#if catalogueBundle.status == "approved catalogue">
        <#if catalogueBundle.active == true>
            The application by [${user.fullName}] – [${user.email}] for registering
            [${catalogueBundle.catalogue.name}] - ([${catalogueBundle.catalogue.id}]) has been approved.
            <br>
            You can view the application status here
            ${endpoint}/catalogue-dashboard/${catalogueBundle.catalogue.id}/info.
        <#else>
            The Catalogue [${catalogueBundle.catalogue.name}] - ([${catalogueBundle.catalogue.id}]) has been set to
            inactive.
            <br>
            You can view the application status here
            ${endpoint}/catalogue-dashboard/${catalogueBundle.catalogue.id}/info.
        </#if>
    </#if>
    <#if catalogueBundle.status == "rejected catalogue">
        The application by [${user.fullName}] – [${user.email}] for registering
        [${catalogueBundle.catalogue.name}] - ([${catalogueBundle.catalogue.id}]) has been rejected.
        <br>
        You can view the application status here ${endpoint}/catalogue-dashboard/${catalogueBundle.catalogue.id}/info.
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>