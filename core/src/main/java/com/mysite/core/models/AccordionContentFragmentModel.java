package com.mysite.core.models;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface AccordionContentFragmentModel {

    /**
     * @return optional title shown above the accordion.
     */
    default String getFaqTitle() {
        return "";
    }
}
