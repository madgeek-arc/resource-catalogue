@XmlSchema(
    namespace = "http://einfracentral.eu",
    elementFormDefault = XmlNsForm.QUALIFIED,
    xmlns = {
        @XmlNs(prefix="tns", namespaceURI="http://einfracentral.eu")
    }
)
package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;