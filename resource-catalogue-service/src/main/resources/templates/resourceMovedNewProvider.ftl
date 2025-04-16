<p>Dear ${user.name},</p>
<p>
    ${resourceType} [${resourceBundleName}]-[${bundleId}] has been moved from its old Provider
    [${oldProvider.provider.id}]-[${oldProvider.provider.name}] to your Provider
    [${newProvider.provider.id}]-[${newProvider.provider.name}].
    <br>
    You can view your new ${resourceType} here:
    ${endpoint}/dashboard/${project?lower_case}/${newProvider.provider.id}/${resourceEndpoint}
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>