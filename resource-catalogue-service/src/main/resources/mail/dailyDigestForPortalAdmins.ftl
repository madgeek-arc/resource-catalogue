<p>
    <#if !sections?? || sections?size == 0>
        There are no changes to ${project} Resources today.
    <#else>
        There are new changes to ${project} Resources!
        <br>
        Below is the daily digest for organisation and resource activities:
        <br>
        <#list sections as section>
            <#if section.created?size &gt; 0>
                <br>
                New ${section.label}:
                <br>
                <#list section.created as id>
                    ${id}
                    <br>
                </#list>
            </#if>
            <#if section.updated?size &gt; 0>
                <br>
                Updated ${section.label}:
                <br>
                <#list section.updated as id>
                    ${id}
                    <br>
                </#list>
            </#if>
            <#if section.activity?size &gt; 0>
                <br>
                ${section.label} Logging Info:
                <br>
                <#list section.activity as key, logs>
                    <#list logs as log>
                        "${key}" got "${log.type}" by [${log.userRole!""}<#if log.userEmail??> - ${log.userEmail}</#if>]
                        <br>
                    </#list>
                </#list>
            </#if>
        </#list>
    </#if>
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>
