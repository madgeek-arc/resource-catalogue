package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.User;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ResourceCRUDService;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Service("publicProviderManager")
public class PublicProviderManager extends ResourceManager<ProviderBundle> implements ResourceCRUDService<ProviderBundle, Authentication> {

    private final JmsTemplate jmsTopicTemplate;
    private final SecurityService securityService;

    @Autowired
    public PublicProviderManager(JmsTemplate jmsTopicTemplate, SecurityService securityService) {
        super(ProviderBundle.class);
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.securityService = securityService;
    }

    @Override
    public String getResourceType() {
        return "provider";
    }

//    @Override
//    public ProviderBundle get(String s) {
//        return super.get(s);
//    }

    @Override
    public Browsing<ProviderBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    @Override
    public Browsing<ProviderBundle> getMy(FacetFilter facetFilter, Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedUserException("Please log in.");
        }
        User user = User.of(authentication);

        List<ProviderBundle> providerList = new ArrayList<>();
        Browsing<ProviderBundle> providerBundleBrowsing = super.getAll(facetFilter, authentication);
        for (ProviderBundle providerBundle : providerBundleBrowsing.getResults()) {
            if (providerBundle.getProvider().getUsers().contains(user)) {
                providerList.add(providerBundle);
            }
        }
        return new Browsing<>(providerBundleBrowsing.getTotal(), providerBundleBrowsing.getFrom(),
                providerBundleBrowsing.getTo(), providerList, providerBundleBrowsing.getFacets());
    }

    @Override
    public ProviderBundle add(ProviderBundle providerBundle, Authentication authentication) {
        providerBundle.getIdentifier().setOriginalId(providerBundle.getId());
        providerBundle.setId(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        providerBundle.getMetadata().setPublished(true);
        ProviderBundle ret;
        ret = super.add(providerBundle, null);
        jmsTopicTemplate.convertAndSend("provider.create", providerBundle);
        return ret;
    }

    @Override
    public ProviderBundle update(ProviderBundle providerBundle, Authentication authentication) {
        ProviderBundle ret = super.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        try {
            BeanUtils.copyProperties(providerBundle, ret);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        providerBundle.getIdentifier().setOriginalId(providerBundle.getId());
        providerBundle.setId(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        providerBundle.getMetadata().setPublished(true);
        ret = super.update(ret, null);
        jmsTopicTemplate.convertAndSend("provider.update", providerBundle);
        return ret;
    }

    @Override
    public void delete(ProviderBundle providerBundle) {
        ProviderBundle publicProviderBundle = get(String.format("%s.%s",providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        super.delete(publicProviderBundle);
        jmsTopicTemplate.convertAndSend("provider.delete", providerBundle);
    }
}