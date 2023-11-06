<p>Dear ${message.senderName},</p>
<p>
    Your message considering the services:
    <br>
    <#list services as service>
    <br>
    > ${service} <
    <br>
    </#list>has been sent successfully.
</p>
<p>
    You can see your message below:
    <br>
    Subject: ${message.subject}
    <br>
    Message: ${message.message}
</p>
<p>
    Best Regards,
    <br>
    the ${project} Team
</p>