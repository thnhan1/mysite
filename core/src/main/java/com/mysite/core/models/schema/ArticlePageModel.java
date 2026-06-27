package com.mysite.core.models.schema;

import javax.annotation.PostConstruct;

import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;

import org.apache.sling.api.SlingHttpServletRequest;
import com.day.cq.wcm.api.Page;
import com.mysite.core.services.ArticleSchemaExtractor;

/**
 * Sling Model cho page-level JSON-LD schema.
 *
 * <p>Được dùng trong customheaderlibs.html của page component để inject
 * JSON-LD vào {@code <head>} của trang.
 *
 * <p>Delegate toàn bộ logic sang {@link ArticleSchemaExtractor}
 * để traverse JCR tree và thu thập articleBody từ tất cả text component.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ArticlePageModel {

    @OSGiService
    private ArticleSchemaExtractor schemaExtractor;

    @ScriptVariable
    private Page currentPage;

    private String schemaJsonOutput;

    @PostConstruct
    protected void init() {
        if (currentPage != null && schemaExtractor != null) {
            this.schemaJsonOutput = schemaExtractor.generateArticleSchema(currentPage);
        }
    }

    public String getSchemaJsonOutput() {
        return schemaJsonOutput;
    }
}
