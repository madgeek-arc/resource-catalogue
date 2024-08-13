<p>Dear ${user.name},</p>
<p>
    Resource [${serviceBundle.service.id}]-[${serviceBundle.service.name}] of the Provider
    [${providerBundle.provider.id}]-[${providerBundle.provider.name}] has not been updated for quite a while.
    <br>
    We kindly suggest you to proceed with the update of your Resource and any other outdated Resources you may have.
    <br>
    You can view your Provider's Resources here:
    ${endpoint}/dashboard/${providerBundle.provider.catalogueId}/${providerBundle.provider.id}/resources
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>