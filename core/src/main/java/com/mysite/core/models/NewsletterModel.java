package com.mysite.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.PostConstruct;

import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;

import lombok.Getter;

@Getter
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class NewsletterModel {

    @OSGiService
    private Externalizer externalizer;

    @SlingObject
    private ResourceResolver resourceResolver;

    @ScriptVariable
    private Page currentPage;

    @ValueMapValue(name = "articlePath")
    private String articlePath;

    private String absolutePublishUrl;

    @PostConstruct
    protected void init() {
        // Ưu tiên dùng articlePath nếu được set, fallback về path trang hiện tại
        String targetPath = (articlePath != null && articlePath.startsWith("/content"))
                ? articlePath
                : (currentPage != null ? currentPage.getPath() : null);

        if (targetPath != null) {
            String pathWithExtension = targetPath + ".html";
            this.absolutePublishUrl = externalizer.publishLink(resourceResolver, pathWithExtension);
        }
    }
}
