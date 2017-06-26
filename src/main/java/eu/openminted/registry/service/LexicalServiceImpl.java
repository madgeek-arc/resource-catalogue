package eu.openminted.registry.service;

import eu.openminted.registry.domain.Lexical;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Created by stefanos on 13/1/2017.
 */
@Service("lexicalService")
@Primary
public class LexicalServiceImpl extends AbstractGenericService<Lexical>{


    public LexicalServiceImpl() {
        super(Lexical.class);
    }

    @Override
    public String getResourceType() {
        return "lexical";
    }

}
