package com.mysite.core.services;

import com.day.cq.wcm.api.Page;

/**
 * Service đọc trực tiếp từ JCR content tree của page để sinh JSON-LD schema.
 * Approach: traverse Resource nodes, đọc property từ ValueMap — không render HTML.
 */
public interface ArticleSchemaExtractor {
    String generateArticleSchema(Page page);
}
