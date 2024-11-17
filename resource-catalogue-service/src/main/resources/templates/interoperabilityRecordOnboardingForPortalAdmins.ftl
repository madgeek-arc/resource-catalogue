<p>Dear ${project} Portal Onboarding Team,</p>
<p>
    <#if bundle.status == "pending interoperability record">
        A new application by [${registrant.fullName}] - [${registrant.email}] has been received for registering
        [${bundle.interoperabilityRecord.title}] -
        [${bundle.interoperabilityRecord.id}] as a new ${project} Interoperability Record
        in ${project} Portal.
        <br>
        You can review the application here ${endpoint}/guidelines/all and approve or reject it.
    <#elseif bundle.status == "approved interoperability record">
        The application by [${registrant.fullName}] - [${registrant.email}] for registering
        [${bundle.interoperabilityRecord.title}] -
        [${bundle.interoperabilityRecord.id}] of
        [${bundle.interoperabilityRecord.providerId}] has been approved.
        <br>
        You can view the application status here ${endpoint}/guidelines/all.
    <#else>
        The Interoperability Record: [${bundle.interoperabilityRecord.title}] -
        [${bundle.interoperabilityRecord.id}] provided
        by [${registrant.fullName}] - [${registrant.email}] has been rejected.
        <br>
        You can view the application status here ${endpoint}/guidelines/all.
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>