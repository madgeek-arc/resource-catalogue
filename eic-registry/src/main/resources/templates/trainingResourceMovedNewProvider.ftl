Dear ${user.name},

Training Resource [${trainingResourceBundle.payload.title}] has been moved from its old Provider [${oldProvider.provider.id}]-[${oldProvider.provider.name}] to your Provider [${newProvider.provider.id}]-[${newProvider.provider.name}].
You can view your new Training Resource here: ${endpoint}/dashboard/${project?lower_case}/${newProvider.provider.id}/training-resources

Best Regards,
the ${project} Team
