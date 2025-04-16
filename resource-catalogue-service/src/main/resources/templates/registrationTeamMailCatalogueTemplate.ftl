<p>Dear ${project} Portal Onboarding Team,</p>
<p>
    <#if bundle.status == "pending catalogue">
        A new application by [${user.fullName}] – [${user.email}] has been received for registering
        [${bundle.catalogue.name}] - ([${bundle.catalogue.id}]) as a new ${project} Catalogue
        in ${project} Portal.
        <br>
        You can review the application here
        ${endpoint}/catalogue-dashboard/${bundle.catalogue.id}/info and approve or reject it.
    </#if>
    <#if bundle.status == "approved catalogue">
        <#if bundle.active == true>
            The application by [${user.fullName}] – [${user.email}] for registering
            [${bundle.catalogue.name}] - ([${bundle.catalogue.id}]) has been approved.
            <br>
            You can view the application status here
            ${endpoint}/catalogue-dashboard/${bundle.catalogue.id}/info.
        <#else>
            The Catalogue [${bundle.catalogue.name}] - ([${bundle.catalogue.id}]) has been set to
            inactive.
            <br>
            You can view the application status here
            ${endpoint}/catalogue-dashboard/${bundle.catalogue.id}/info.
        </#if>
    </#if>
    <#if bundle.status == "rejected catalogue">
        The application by [${user.fullName}] – [${user.email}] for registering
        [${bundle.catalogue.name}] - ([${bundle.catalogue.id}]) has been rejected.
        <br>
        You can view the application status here ${endpoint}/catalogue-dashboard/${bundle.catalogue.id}/info.
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>