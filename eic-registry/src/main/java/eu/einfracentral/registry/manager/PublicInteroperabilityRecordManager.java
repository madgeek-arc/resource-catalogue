package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Identifiers;
import eu.einfracentral.domain.InteroperabilityRecordBundle;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ResourceCRUDService;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service
public class PublicInteroperabilityRecordManager extends ResourceManager<InteroperabilityRecordBundle> implements ResourceCRUDService<InteroperabilityRecordBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(PublicInteroperabilityRecordManager.class);
    private final JmsTemplate jmsTopicTemplate;

    @Autowired
    public PublicInteroperabilityRecordManager(JmsTemplate jmsTopicTemplate) {
        super(InteroperabilityRecordBundle.class);
        this.jmsTopicTemplate = jmsTopicTemplate;
    }

    @Override
    public String getResourceType() {
        return "interoperability_record";
    }

    @Override
    public Browsing<InteroperabilityRecordBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    @Override
    public InteroperabilityRecordBundle add(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication authentication) {
        String lowerLevelResourceId = interoperabilityRecordBundle.getId();
        interoperabilityRecordBundle.setIdentifiers(Identifiers.createIdentifier(interoperabilityRecordBundle.getId()));
        interoperabilityRecordBundle.setId(String.format("%s.%s", interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getId()));

        // set providerId to Public
        interoperabilityRecordBundle.getInteroperabilityRecord().setProviderId(String.format("%s.%s",
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId()));

        interoperabilityRecordBundle.getMetadata().setPublished(true);
        InteroperabilityRecordBundle ret;
        logger.info(String.format("Interoperability Record [%s] is being published with id [%s]", lowerLevelResourceId, interoperabilityRecordBundle.getId()));
        ret = super.add(interoperabilityRecordBundle, null);
        logger.info("Sending JMS with topic 'interoperability_record.create'");
        jmsTopicTemplate.convertAndSend("interoperability_record.create", interoperabilityRecordBundle);
        return ret;
    }

    @Override
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication authentication) {
        InteroperabilityRecordBundle published = super.get(String.format("%s.%s", interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getId()));
        InteroperabilityRecordBundle ret = super.get(String.format("%s.%s", interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getId()));
        try {
            BeanUtils.copyProperties(ret, interoperabilityRecordBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        // set providerId to Public
        ret.getInteroperabilityRecord().setProviderId(published.getInteroperabilityRecord().getProviderId());

        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.setMetadata(published.getMetadata());
        logger.info(String.format("Updating public Interoperability Record with id [%s]", ret.getId()));
        ret = super.update(ret, null);
        logger.info("Sending JMS with topic 'interoperability_record.update'");
        jmsTopicTemplate.convertAndSend("interoperability_record.update", ret);
        return ret;
    }

    @Override
    public void delete(InteroperabilityRecordBundle interoperabilityRecordBundle) {
        try{
            InteroperabilityRecordBundle publicInteroperabilityRecordBundle = get(String.format("%s.%s", interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getId()));
            logger.info(String.format("Deleting public Interoperability Record with id [%s]", publicInteroperabilityRecordBundle.getId()));
            super.delete(publicInteroperabilityRecordBundle);
            logger.info("Sending JMS with topic 'interoperability_record.delete'");
            jmsTopicTemplate.convertAndSend("interoperability_record.delete", publicInteroperabilityRecordBundle);
        } catch (ResourceException | ResourceNotFoundException ignore){
        }
    }

}
