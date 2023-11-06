<p>There are Providers and Templates waiting to be approved.</p>
<p>
    <#if iaProviders?? && iaProviders?size &gt; 0>
        Providers waiting for Initial Approval:
        <br>
        <#list iaProviders as provider>
            <br>
            ${provider}
        </#list>
    </#if>
    <#if stProviders?? && stProviders?size &gt; 0>
        Providers waiting for Template Approval:
        <br>
        <#list stProviders as provider>
            <br>
            ${provider}
        </#list>
    </#if>
</p>
<p>You can review them at: ${endpoint}/provider/all</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>