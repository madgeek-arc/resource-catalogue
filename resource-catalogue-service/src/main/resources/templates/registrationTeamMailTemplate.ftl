<p>Dear ${project} Onboarding Team,</p>
<p>
    <#if bundle.templateStatus == "no template status">
        <#if bundle.status == "pending">
            A new application by ${user.name} ${user.surname} – [${user.email}] has been received for registering
            [${bundle.organisation.name}] - ([${bundle.organisation.id}]) as a new ${project} Provider in
            ${project}.
            <br>
            You can review the application here
            ${endpoint}/dashboard/${project?lower_case}/${bundle.organisation.id}/info
            and approve or reject it.
        </#if>
        <#if bundle.status == "approved">
            <#if bundle.active == true>
                The application by ${user.name} ${user.surname} – [${user.email}] for registering
                [${bundle.organisation.name}] - ([${bundle.organisation.id}]) has been approved.
                <br>
                You can view the application status here
                ${endpoint}/dashboard/${project?lower_case}/${bundle.organisation.id}/info.
            <#else>
                The Provider [${bundle.organisation.name}] - ([${bundle.organisation.id}]) has been set to
                inactive.
                <br>
                You can view the application status here
                ${endpoint}/dashboard/${project?lower_case}/${bundle.organisation.id}/info.
            </#if>
        </#if>
        <#if bundle.status == "rejected">
            The application by ${user.name} ${user.surname} – [${user.email}] for registering
            [${bundle.organisation.name}] - ([${bundle.organisation.id}]) has been rejected.
            <br>
            You can view the application status here
            ${endpoint}/dashboard/${project?lower_case}/${bundle.organisation.id}/info.
        </#if>
    <#else>
        <#if bundle.templateStatus == "pending template">
            A new application by ${user.name} ${user.surname} – [${user.email}] has been received for registering
            [${resourceBundleName}] - [${resourceBundleId}], as a new Resource of
            [${bundle.organisation.name}] - ([${bundle.organisation.id}]).
            <br>
            You can review the application here
            ${endpoint}/dashboard/${project?lower_case}/${bundle.organisation.id}/info and approve or reject it.
        </#if>
        <#if bundle.templateStatus == "approved template">
            The application by ${user.name} ${user.surname} – [${user.email}] for registering
            [${resourceBundleName}] - ([${resourceBundleId}]) of
            [${bundle.organisation.name}] - ([${bundle.organisation.id}]) has been approved.
            <br>
            You can view the application status here
            ${endpoint}/dashboard/${project?lower_case}/${bundle.organisation.id}/info.
        </#if>
        <#if bundle.templateStatus == "rejected template">
            The Resource: [${resourceBundleId}] provided by ${user.name} ${user.surname} – [${user.email}] has been rejected.
            <br>
            You can view the application status
            ${endpoint}/dashboard/${project?lower_case}/${bundle.organisation.id}/info.
        </#if>
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>