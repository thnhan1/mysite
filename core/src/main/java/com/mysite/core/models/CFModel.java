package com.mysite.core.models;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface CFModel {
    default String getElementContent(String elementName) {
        throw new UnsupportedOperationException();
    }
}
