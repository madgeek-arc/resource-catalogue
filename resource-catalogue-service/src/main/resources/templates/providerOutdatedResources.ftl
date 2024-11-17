<p>Dear ${user.name},</p>
<p>
    ${resourceType} [${resourceBundleName}]-[${resourceBundleId}] of the Provider
    [${bundle.provider.id}]-[${bundle.provider.name}] has not been updated for quite a while.
    <br>
    We kindly suggest you to proceed with the update of your ${resourceType} and any other outdated resources you may have.
    <br>
    You can view your Provider's Resources here:
    ${endpoint}/dashboard/${bundle.provider.catalogueId}/${bundle.provider.id}/${resourceEndpoint}
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>