package com.mysite.core.models.impl;

import com.day.crx.JcrConstants;
import com.mysite.core.models.AccordionContentFragmentModel;
import com.mysite.core.models.CFModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.settings.SlingSettingsService;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Slf4j
@Model(adaptables = SlingHttpServletRequest.class, adapters = {AccordionContentFragmentModel.class})
public class AccordionContentFragmentModelImpl implements AccordionContentFragmentModel {

    private static final String CF_FRAGMENT_PATH = "fragmentPath";
    private static final String CF_ELEMENTS_NAME = "elementNames";
    private static final String CF_FAQ_QUESTION = "question";
    private static final String CF_FAQ_ANSWER = "answer";
    private static final String CF_FAQ_ITEMS = "/item_";
    private static final String CF_FAQ_PANEL_TITLE = "cq:panelTitle";
    private static final String CF_FAQ_ITEM = "item_";
    private static final String UNDERSCORE = "_";
    private static final String PUBLISH_ENV = "publish";

    private static final Map<Integer, String> NUMBERS = new HashMap<>();
    private static final Map<String, Object> DEFAULT_FAQ_PROPERTIES = new HashMap<>();

    static {
        NUMBERS.put(1, "One");
        NUMBERS.put(2, "Two");
        NUMBERS.put(3, "Three");
        NUMBERS.put(4, "Four");
        NUMBERS.put(5, "Five");
        NUMBERS.put(6, "Six");
        NUMBERS.put(7, "Seven");
        NUMBERS.put(8, "Eight");
        NUMBERS.put(9, "Nine");
        NUMBERS.put(10, "Ten");
        NUMBERS.put(11, "Eleven");
        NUMBERS.put(12, "Twelve");
        NUMBERS.put(13, "Thirteen");
        NUMBERS.put(14, "Fourteen");
        NUMBERS.put(15, "Fifteen");
        NUMBERS.put(16, "Sixteen");
        NUMBERS.put(17, "Seventeen");
        NUMBERS.put(18, "Eighteen");
        NUMBERS.put(19, "Nineteen");
        NUMBERS.put(20, "Twenty");
    }

    static {
        DEFAULT_FAQ_PROPERTIES.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        DEFAULT_FAQ_PROPERTIES.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, "mysite/components/contentfragment");
        DEFAULT_FAQ_PROPERTIES.put("containerOpted", "dynamic");
        DEFAULT_FAQ_PROPERTIES.put("displayMode", "singleText");
        DEFAULT_FAQ_PROPERTIES.put("paragraphScope", "all");
        DEFAULT_FAQ_PROPERTIES.put("variationName", "master");
    }

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Resource resource;

    @SlingObject
    private ResourceResolver resourceResolver;

    @OSGiService
    private SlingSettingsService slingSettings;

    @OSGiService
    private ModelFactory modelFactory;

    private String faqTitle = StringUtils.EMPTY;
    private List<Integer> faqCount = new ArrayList<>();

    @PostConstruct
    private void initModel() {
        faqTitle = resource.getValueMap().get("faqTitle", StringUtils.EMPTY);

        if (isPublish()) {
            return;
        }

        try {
            String fragmentPath = resource.getValueMap().get(CF_FRAGMENT_PATH, StringUtils.EMPTY);
            if (StringUtils.isEmpty(fragmentPath)) {
                return;
            }

            Resource cfResource = resourceResolver.getResource(fragmentPath);
            if (cfResource == null) {
                log.warn("Content Fragment not found at path: {}", fragmentPath);
                return;
            }

            CFModel cfModel = modelFactory.getModelFromWrappedRequest(request, cfResource, CFModel.class);
            if (cfModel == null) {
                log.warn("Unable to adapt resource {} to CFModel", fragmentPath);
                return;
            }

            getBodyElementCounts(cfModel);

            List<Integer> children = StreamSupport.stream(resource.getChildren().spliterator(), false)
                    .map(accRes -> StringUtils.substringAfter(accRes.getName(), UNDERSCORE))
                    .filter(StringUtils::isNotBlank)
                    .map(NumberUtils::toInt)
                    .sorted()
                    .collect(Collectors.toList());

            if (!faqCount.equals(children)) {
                deleteInvalidResources(children);
                addFaqItems(fragmentPath, cfModel);
                commitChanges();
            }
        } catch (Exception e) {
            log.error("Unable to sync accordion items from Content Fragment", e);
        }
    }

    private boolean isPublish() {
        return slingSettings.getRunModes().contains(PUBLISH_ENV);
    }

    private void getBodyElementCounts(CFModel cfModel) {
        faqCount = IntStream.rangeClosed(1, 20)
                .filter(num -> StringUtils.isNotEmpty(cfModel.getElementContent(CF_FAQ_QUESTION + NUMBERS.get(num))))
                .boxed()
                .collect(Collectors.toList());
    }

    private void deleteInvalidResources(List<Integer> children) {
        children.removeAll(faqCount);
        children.forEach(this::deleteResource);
    }

    private void deleteResource(Integer num) {
        try {
            Resource child = resource.getChild(CF_FAQ_ITEM + num);
            if (child != null) {
                resourceResolver.delete(child);
            }
        } catch (PersistenceException e) {
            log.error("Unable to delete resource item_{}: {}", num, e.getMessage());
        }
    }

    private void addFaqItems(String fragmentPath, CFModel cfModel) {
        for (Integer num : faqCount) {
            try {
                Map<String, Object> props = new HashMap<>(DEFAULT_FAQ_PROPERTIES);
                props.put(CF_FRAGMENT_PATH, fragmentPath);
                props.put(CF_ELEMENTS_NAME, CF_FAQ_ANSWER + NUMBERS.get(num));
                props.put(CF_FAQ_PANEL_TITLE, cfModel.getElementContent(CF_FAQ_QUESTION + NUMBERS.get(num)));

                ResourceUtil.getOrCreateResource(
                        resourceResolver,
                        resource.getPath() + CF_FAQ_ITEMS + num,
                        props,
                        StringUtils.EMPTY,
                        false);
            } catch (PersistenceException e) {
                log.error("Unable to create FAQ item {}: {}", num, e.getMessage());
            }
        }
    }

    private void commitChanges() {
        if (resourceResolver.hasChanges()) {
            try {
                resourceResolver.commit();
            } catch (PersistenceException e) {
                log.error("Unable to commit changes: {}", e.getMessage());
            }
        }
    }

    @Override
    public String getFaqTitle() {
        return faqTitle;
    }
}
