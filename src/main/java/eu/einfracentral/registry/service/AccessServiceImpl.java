package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Access;

/**
 * Created by pgl on 05/01/18.
 */
@org.springframework.stereotype.Service("accessService")
public class AccessServiceImpl extends ResourceServiceImpl<Access> implements AccessService {
    public AccessServiceImpl() {
        super(Access.class);
    }

    @Override
    public String getResourceType() {
        return "access";
    }
}
