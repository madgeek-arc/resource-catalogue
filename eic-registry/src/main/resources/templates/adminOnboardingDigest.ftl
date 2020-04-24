There are Providers and Service Templates waiting to be approved.

<#if iaProviders?? && iaProviders?size &gt; 0>
Providers waiting for Initial Approval:
    <#list iaProviders as provider>
    ${provider}
    </#list>

</#if>

<#if stProviders?? && stProviders?size &gt; 0>
Providers waiting for Service Template Approval:
    <#list stProviders as provider>
    ${provider}
    </#list>

</#if>

You can review them at: ${endpoint}/serviceProvidersList

Best Regards,
the ${project} Team