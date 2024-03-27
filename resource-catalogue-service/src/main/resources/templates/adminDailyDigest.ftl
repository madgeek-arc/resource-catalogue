<p>
    <#if changes == false>
        There are no changes to ${project} Resources today.
    <#else>
        There are new changes to ${project} Resources!
        <br>
        Below is the daily digest for provider and resource activities:
        <br>
        <#if newProviders?? && newProviders?size &gt; 0>
            New Providers:
            <br>
            <#list newProviders as provider>
                ${provider}
                <br>
            </#list>
        </#if>
        <#if updatedProviders?? && updatedProviders?size &gt; 0>
            Updated Providers:
            <br>
            <#list updatedProviders as provider>
                ${provider}
                <br>
            </#list>
        </#if>
        <#if newServices?? && newServices?size &gt; 0>
            New Resources:
            <br>
            <#list newServices as service>
                ${service}
                <br>
            </#list>
        </#if>
        <#if updatedServices?? && updatedServices?size &gt; 0>
            Updated Resources:
            <br>
            <#list updatedServices as service>
                ${service}
                <br>
            </#list>
        </#if>
        <#if loggingInfoProviderMap?? && loggingInfoProviderMap?size &gt; 0>
            Provider Logging Info:
            <br>
            <#list loggingInfoProviderMap as key, value>
                <#list value as item>
                    <#assign providerItem = item>
                    Provider "${key}" got "${providerItem.type}" by [${providerItem.userRole}
                    <#if providerItem.userEmail??>- ${providerItem.userEmail}</#if>]
                    <br>
                </#list>
            </#list>
        </#if>
        <#if loggingInfoServiceMap?? && loggingInfoServiceMap?size &gt; 0>
            Resource Logging Info:
            <br>
            <#list loggingInfoServiceMap as key, value>
                <#list value as item>
                    <#assign serviceItem = item>
                    Resource "${key}" got "${serviceItem.type}" by [${serviceItem.userRole} - ${serviceItem.userEmail}]
                    <br>
                </#list>
            </#list>
        </#if>
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>