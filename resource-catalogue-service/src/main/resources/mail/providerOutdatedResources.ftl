<p>Dear ${user.name},</p>
<p>
    ${resourceType} [${resourceBundleName}]-[${resourceBundleId}] of the Provider
    [${bundle.organisation.id}]-[${bundle.organisation.name}] has not been updated for quite a while.
    <br>
    We kindly suggest you to proceed with the update of your ${resourceType} and any other outdated resources you may have.
    <br>
    You can view your Provider's Resources here:
    ${endpoint}/dashboard/${bundle.catalogueId}/${bundle.organisation.id}/${resourceEndpoint}
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>