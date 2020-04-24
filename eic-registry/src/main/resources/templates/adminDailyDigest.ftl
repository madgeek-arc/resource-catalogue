<#if changes == false>
There are no changes to ${project} Resources today.
<#else>
There are new changes to CatRIS Resources!

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
New Services:
        <#list newServices as service>
    ${service}
        </#list>

    </#if>
    <#if updatedServices?? && updatedServices?size &gt; 0>
Updated Services:
        <#list updatedServices as service>
    ${service}
        </#list>

    </#if>
</#if>

Best Regards,
the ${project} Team