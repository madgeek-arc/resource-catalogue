<p>Dear ${project} Onboarding Team,</p>
<p>
    <#if bundle.status == "pending">
        A new application by ${user.name} ${user.surname} - [${user.email}] has been received for registering
        [${bundle.payload.name}] - [${bundle.payload.id}] as a new ${project} Interoperability Record
        in ${project}.
        <br>
        You can review the application here ${endpoint}/guidelines/all and approve or reject it.
    <#elseif bundle.status == "approved">
        The application by ${user.name} ${user.surname} - [${user.email}] for registering
        [${bundle.payload.name}] - [${bundle.payload.id}] of [${bundle.payload.resourceOwner}] has been approved.
        <br>
        You can view the application status here ${endpoint}/guidelines/all.
    <#else>
        The Interoperability Record: [${bundle.payload.name}] - [${bundle.payload.id}] provided
        by ${user.name} ${user.surname} - [${user.email}] has been rejected.
        <br>
        You can view the application status here ${endpoint}/guidelines/all.
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>