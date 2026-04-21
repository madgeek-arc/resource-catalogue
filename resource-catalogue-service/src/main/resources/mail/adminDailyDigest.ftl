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
            New Services:
            <br>
            <#list newServices as service>
                ${service}
                <br>
            </#list>
        </#if>
        <#if updatedServices?? && updatedServices?size &gt; 0>
            Updated Services:
            <br>
            <#list updatedServices as service>
                ${service}
                <br>
            </#list>
        </#if>
        <#if newTrainings?? && newTrainings?size &gt; 0>
            New Training Resources:
            <br>
            <#list newTrainings as training>
                ${training}
                <br>
            </#list>
        </#if>
        <#if updatedTrainings?? && updatedTrainings?size &gt; 0>
            Updated Training Resources:
            <br>
            <#list updatedTrainings as training>
                ${training}
                <br>
            </#list>
        </#if>
        <#if newGuidelines?? && newGuidelines?size &gt; 0>
            New Interoperability Records:
            <br>
            <#list newGuidelines as guideline>
                ${guideline}
                <br>
            </#list>
        </#if>
        <#if updatedGuidelines?? && updatedGuidelines?size &gt; 0>
            Updated Interoperability Records:
            <br>
            <#list updatedGuidelines as guideline>
                ${guideline}
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
            Service Logging Info:
            <br>
            <#list loggingInfoServiceMap as key, value>
                <#list value as item>
                    <#assign serviceItem = item>
                    Service "${key}" got "${serviceItem.type}" by [${serviceItem.userRole} - ${serviceItem.userEmail}]
                    <br>
                </#list>
            </#list>
        </#if>
        <#if loggingInfoTrainingMap?? && loggingInfoTrainingMap?size &gt; 0>
            Training Resource Logging Info:
            <br>
            <#list loggingInfoTrainingMap as key, value>
                <#list value as item>
                    <#assign trainingItem = item>
                    Training Resource "${key}" got "${trainingItem.type}" by [${trainingItem.userRole} - ${trainingItem.userEmail}]
                    <br>
                </#list>
            </#list>
        </#if>
        <#if loggingInfoGuidelineMap?? && loggingInfoGuidelineMap?size &gt; 0>
            Interoperability Record Logging Info:
            <br>
            <#list loggingInfoGuidelineMap as key, value>
                <#list value as item>
                    <#assign guidelineItem = item>
                    Interoperability Record "${key}" got "${guidelineItem.type}" by [${guidelineItem.userRole} - ${guidelineItem.userEmail}]
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