package com.mysite.core.services.impl;

import com.day.cq.wcm.api.Page;
import com.google.gson.JsonObject;
import com.mysite.core.services.ArticleSchemaExtractor;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation đúng chuẩn AEM: đọc trực tiếp từ JCR thông qua Resource/ValueMap.
 *
 * <p>Tại sao KHÔNG render HTML rồi parse:
 * <ul>
 *   <li>Tạo HTTP request nội bộ — tốn tài nguyên, có thể gây deadlock</li>
 *   <li>Phụ thuộc vào output HTML — fragile, vỡ khi đổi template</li>
 *   <li>Cần thư viện ngoài (jsoup) không có trong AEM platform</li>
 *   <li>Sai bản chất AEM: dữ liệu đã có sẵn trong JCR, không cần render</li>
 * </ul>
 *
 * <p>Cách làm chuẩn: traverse Resource tree → đọc ValueMap property → strip HTML tags đơn giản.
 */
@Slf4j
@Component(service = ArticleSchemaExtractor.class)
public class ArticleSchemaExtractorImpl implements ArticleSchemaExtractor {

    /**
     * Resource types của các text component cần thu thập nội dung.
     * Thêm vào đây nếu dự án có thêm custom text component.
     */
    private static final Set<String> TEXT_RESOURCE_TYPES = new HashSet<>(Arrays.asList(
            "mysite/components/text",
            "core/wcm/components/text/v2/text",
            "core/wcm/components/text/v1/text"
    ));

    @Override
    public String generateArticleSchema(Page page) {
        if (page == null) {
            return StringUtils.EMPTY;
        }

        // Lấy jcr:content resource — đây là root của toàn bộ nội dung trang
        Resource contentResource = page.getContentResource();
        if (contentResource == null) {
            log.warn("Không tìm thấy jcr:content cho page: {}", page.getPath());
            return StringUtils.EMPTY;
        }

        // Traverse cây JCR để thu thập text từ tất cả text component
        StringBuilder bodyBuilder = new StringBuilder();
        collectTextFromJcr(contentResource, bodyBuilder);

        String pagePath = page.getPath();

        // Sinh JSON-LD theo chuẩn Schema.org
        JsonObject schema = new JsonObject();
        schema.addProperty("@context", "https://schema.org");
        schema.addProperty("@type", pagePath.contains("/blog/") ? "BlogPosting" : "NewsArticle");
        schema.addProperty("headline",     StringUtils.defaultString(page.getTitle()));
        schema.addProperty("description",  StringUtils.defaultString(page.getDescription()));
        schema.addProperty("articleBody",  bodyBuilder.toString().trim());

        return schema.toString();
    }

    /**
     * Đệ quy traverse toàn bộ resource tree.
     * Với mỗi node có resourceType là text component → đọc property "text" từ ValueMap.
     *
     * @param resource node hiện tại
     * @param builder  accumulator chứa text thu thập được
     */
    private void collectTextFromJcr(Resource resource, StringBuilder builder) {
        ValueMap props = resource.getValueMap();
        String resourceType = props.get("sling:resourceType", StringUtils.EMPTY);

        if (TEXT_RESOURCE_TYPES.contains(resourceType)) {
            String htmlText = props.get("text", StringUtils.EMPTY);
            if (StringUtils.isNotBlank(htmlText)) {
                // AEM text component lưu HTML (vd: "<p>Hello <b>world</b></p>")
                // Strip tags bằng regex — không cần jsoup
                String plainText = stripHtmlTags(htmlText);
                if (StringUtils.isNotBlank(plainText)) {
                    builder.append(plainText).append(" ");
                }
            }
        }

        // Tiếp tục traverse vào các child node
        for (Resource child : resource.getChildren()) {
            collectTextFromJcr(child, builder);
        }
    }

    /**
     * Loại bỏ HTML tags và decode HTML entities phổ biến.
     * Đủ dùng cho mục đích Schema.org articleBody — không cần thư viện ngoài.
     */
    private String stripHtmlTags(String html) {
        return html
                .replaceAll("<[^>]*>", " ")   // xóa tất cả thẻ HTML
                .replace("&nbsp;",  " ")
                .replace("&amp;",   "&")
                .replace("&lt;",    "<")
                .replace("&gt;",    ">")
                .replace("&quot;",  "\"")
                .replace("&#39;",   "'")
                .replaceAll("\\s+", " ")       // collapse nhiều khoảng trắng
                .trim();
    }
}
