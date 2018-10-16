<body>
    <p>Dear Registration Team</p>
    <br>
    <#if provider.status == "initialized">
        <p>A new application by [${user.name}] – [${user.email}] has been received for registering [provider.id]
            – [Service Provider Name] as a new service provider in eInfraCentral.
            You can review the application <a href="${endpoint}/serviceProvidersList">here</a> and approve or reject it.
        </p>
    </#if>
    <#if provider.status == "pending 1">
        <p>The application by [${user.name}] – [${user.email}] for registering [${provider.id}] has been accepted.
            You can view the application status <a href="${endpoint}/serviceProvidersList">here</a>.
        </p>
    </#if>
    <#if provider.status == "pending 2">
        <p>Information about the new service: [${service.id}] has been provided by [${user.name}] – [${user.email}].
            You can review the information <a href="${endpoint}/service/${service.id}">here</a> and approve or reject it.
        </p>
    </#if>
    <#if provider.status == "approved">
        <p>The service: [Service ID] provided by [user name] – [user email] has been accepted.
            You can view the application status <a href="${endpoint}/serviceProvidersList">here</a>.
        </p>
    </#if>
    <#if provider.status == "rejected_sp">
        <p>The service: [Service ID] provided by [${user.name}] – [user email] has been [accepted \ rejected].
            You can view the application status <a href="${endpoint}/serviceProvidersList">here</a>.
        </p>
    </#if>
    <#if provider.status == "rejected">
        <p>The application by ${user.name} ${user.surname} – ${user.email} for registering ${provider.name} has been rejected.
            You can view the application status here <a href="${endpoint}/serviceProvidersList">here</a>.
        </p>
    </#if>
    <br>
    <p>Best Regards<br>
        the eInfraCentral Team
    </p>
</body>