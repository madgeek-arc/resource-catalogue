Dear ${user.fullName},

<#if interoperabilityRecordBundle.status == "pending interoperability record">
    You have applied for registering [${interoperabilityRecordBundle.interoperabilityRecord.title}] - [${interoperabilityRecordBundle.interoperabilityRecord.id}] as a new ${project} Interoperability Record in the ${project} Portal.
    Your application will be reviewed and you will be notified on its approval or rejection.
<#elseif interoperabilityRecordBundle.status == "approved interoperability record">
    Your application for registering [${interoperabilityRecordBundle.interoperabilityRecord.title}] - [${interoperabilityRecordBundle.interoperabilityRecord.id}] as a new ${project} Interoperability Record in the ${project} Portal has been approved.
<#else>
    Your application for registering [${interoperabilityRecordBundle.interoperabilityRecord.title}] - [${interoperabilityRecordBundle.interoperabilityRecord.id}] as a new ${project} Interoperability Record in the ${project} Portal has been rejected.
    For any clarifications, please contact us at ${registrationEmail}.
</#if>

Best Regards,
the ${project} Portal Onboarding Team
