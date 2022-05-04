<#if changes == false>
There are no changes to ${project} Resources today.
<#else>
There are new changes to ${project} Resources!
Below is the daily digest for provider and resource activities:

    <#if newProviders?? && newProviders?size &gt; 0>
New Providers:
        <#list newProviders as provider>
     ${provider}
        </#list>
    </#if>

    <#if updatedProviders?? && updatedProviders?size &gt; 0>
Updated Providers:
        <#list updatedProviders as provider>
    ${provider}
        </#list>
    </#if>

    <#if newServices?? && newServices?size &gt; 0>
New Resources:
        <#list newServices as service>
    ${service}
        </#list>
    </#if>

    <#if updatedServices?? && updatedServices?size &gt; 0>
Updated Resources:
        <#list updatedServices as service>
    ${service}
        </#list>
    </#if>

    <#if loggingInfoProviderMap?? && loggingInfoProviderMap?size &gt; 0>
Provider Logging Info:
        <#list loggingInfoProviderMap as key, value>
            <#list value as item>
                <#assign providerItem = item>
                Provider "${key}" got "${providerItem.type}" by [${providerItem.userRole} <#if providerItem.userEmail??>- ${providerItem.userEmail}</#if>]
            </#list>
        </#list>
    </#if>

    <#if loggingInfoServiceMap?? && loggingInfoServiceMap?size &gt; 0>
Resource Logging Info:
        <#list loggingInfoServiceMap as key, value>
            <#list value as item>
                <#assign serviceItem = item>
                Resource "${key}" got "${serviceItem.type}" by [${serviceItem.userRole} - ${serviceItem.userEmail}]
            </#list>
        </#list>
    </#if>
</#if>

Best Regards,
the ${project} Team