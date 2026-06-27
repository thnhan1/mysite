package com.mysite.core.models.impl;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.mysite.core.models.CFModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Model(adaptables = SlingHttpServletRequest.class, adapters = {CFModel.class})
public class CFModelImpl implements CFModel {

    @ScriptVariable
    private Resource resource;

    private Optional<ContentFragment> contentFragment;

    @PostConstruct
    private void initModel() {
        contentFragment = Optional.ofNullable(resource.adaptTo(ContentFragment.class));
    }

    @Override
    public String getElementContent(String elementName) {
        return contentFragment
                .map(cf -> cf.getElement(elementName))
                .map(ContentElement::getContent)
                .orElse(StringUtils.EMPTY);
    }
}