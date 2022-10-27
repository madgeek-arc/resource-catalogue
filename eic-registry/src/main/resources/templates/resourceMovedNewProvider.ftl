Dear ${user.name},

Resource [${resourceBundle.payload.name}] has been moved from its old Provider [${oldProvider.provider.id}]-[${oldProvider.provider.name}] to your Provider [${newProvider.provider.id}]-[${newProvider.provider.name}].
You can view your new Resource here: ${endpoint}/dashboard/${project?lower_case}/${newProvider.provider.id}/resources

Best Regards,
the ${project} Team
