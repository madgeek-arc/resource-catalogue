Dear ${message.senderName},

Your message considering the services:
<#list services as service>
    > ${service} <
</#list>has been sent successfully.

You can see your message below:
Subject: ${message.subject}
Message: ${message.message}

Best Regards,
the ${project} Team