<p>There are Providers and Templates waiting to be approved.</p>
<p>
    <#if providersWaitingForInitialApproval?? && providersWaitingForInitialApproval?size &gt; 0>
        Providers waiting for Initial Approval:
        <br>
        <#list providersWaitingForInitialApproval as provider>
            <br>
            ${provider}
        </#list>
    </#if>
    <#if providersWaitingForTemplateApproval?? && providersWaitingForTemplateApproval?size &gt; 0>
        Providers waiting for Template Approval:
        <br>
        <#list providersWaitingForTemplateApproval as provider>
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