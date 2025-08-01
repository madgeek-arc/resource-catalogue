<p>Dear ${user.fullName},</p>
<p>
    <#if bundle.status == "pending interoperability record">
        You have applied for registering[${bundle.interoperabilityRecord.title}] -
        [${bundle.interoperabilityRecord.id}] as a new ${project} Interoperability Record in
        the ${project} Beyond.
        <br>
        Your application will be reviewed and you will be notified on its approval or rejection.
    <#elseif bundle.status == "approved interoperability record">
        Your application for registering [${bundle.interoperabilityRecord.title}] -
        [${bundle.interoperabilityRecord.id}] as a new ${project} Interoperability Record in
        the ${project} Beyond has been approved.
    <#else>
        Your application for registering [${bundle.interoperabilityRecord.title}] -
        [${bundle.interoperabilityRecord.id}] as a new ${project} Interoperability Record in
        the ${project} Beyond has been rejected.
        <br>
        For any clarifications, please contact us at ${registrationEmail}.
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Beyond Onboarding Team
</p>