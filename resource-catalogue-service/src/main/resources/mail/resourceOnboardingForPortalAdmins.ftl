<p>Dear ${project} Onboarding Team,</p>
<p>
    <#if bundle.status == "pending">
        A new application by ${user.name} ${user.surname} - [${user.email}] has been received for registering
        [${bundle.payload.name}] - [${bundle.payload.id}] as a new ${project} ${resourceType}
        in ${project}.
        <br>
        You can review the application here ${endpoint}/${resourceAdminListPath} and approve or reject it.
    <#elseif bundle.status == "approved">
        The application by ${user.name} ${user.surname} - [${user.email}] for registering
        [${bundle.payload.name}] - [${bundle.payload.id}] of [${bundle.payload.resourceOwner}] has been approved.
        <br>
        You can view the application status here ${endpoint}/${resourceAdminListPath}.
    <#else>
        The ${resourceType}: [${bundle.payload.name}] - [${bundle.payload.id}] provided
        by ${user.name} ${user.surname} - [${user.email}] has been rejected.
        <br>
        You can view the application status here ${endpoint}/${resourceAdminListPath}.
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>
