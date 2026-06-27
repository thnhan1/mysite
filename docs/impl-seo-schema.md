# Implement SEO JSON-LD Schema cho Page Component

## Tổng quan

Hướng dẫn này mô tả cách sinh structured data (JSON-LD) trong `<head>` của trang AEM, sử dụng kiến trúc **Sling Model + OSGi Service + HTL**.

**Kiến trúc:**

```
customheaderlibs.html (HTL)
  └─ ArticlePageModel (Sling Model — inject service, lấy currentPage)
       └─ ArticleSchemaExtractor (OSGi Service Interface)
            └─ ArticleSchemaExtractorImpl (traverse JCR → sinh JSON-LD)
```

Schema được render ở **base page component** (`mysite/components/page`), nên **tất cả page type** kế thừa đều tự động có JSON-LD — không cần override `customheaderlibs.html` ở từng component con.

**Stack:** Java 11, AEM 6.5, uber-jar 6.5.24, Gson (provided bởi AEM), Lombok.

---

## Bước 1: Tạo OSGi Service — `ArticleSchemaExtractor`

Service chịu trách nhiệm đọc dữ liệu từ JCR và sinh chuỗi JSON-LD.

### 1.1 Interface

File: `core/src/main/java/com/mysite/core/services/ArticleSchemaExtractor.java`

```java
package com.mysite.core.services;

import com.day.cq.wcm.api.Page;

/**
 * Service đọc trực tiếp từ JCR content tree của page để sinh JSON-LD schema.
 * Approach: traverse Resource nodes, đọc property từ ValueMap — không render HTML.
 */
public interface ArticleSchemaExtractor {
    String generateArticleSchema(Page page);
}
```

### 1.2 Implementation

File: `core/src/main/java/com/mysite/core/services/impl/ArticleSchemaExtractorImpl.java`

```java
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

@Slf4j
@Component(service = ArticleSchemaExtractor.class)
public class ArticleSchemaExtractorImpl implements ArticleSchemaExtractor {

    /** Resource types của các text component cần thu thập nội dung. */
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
        schema.addProperty("@type",
                pagePath.contains("/blog/") ? "BlogPosting" : "NewsArticle");
        schema.addProperty("headline",    StringUtils.defaultString(page.getTitle()));
        schema.addProperty("description", StringUtils.defaultString(page.getDescription()));
        schema.addProperty("articleBody", bodyBuilder.toString().trim());

        return schema.toString();
    }

    /** Đệ quy traverse resource tree → đọc property "text" từ các text component. */
    private void collectTextFromJcr(Resource resource, StringBuilder builder) {
        ValueMap props = resource.getValueMap();
        String resourceType = props.get("sling:resourceType", StringUtils.EMPTY);

        if (TEXT_RESOURCE_TYPES.contains(resourceType)) {
            String htmlText = props.get("text", StringUtils.EMPTY);
            if (StringUtils.isNotBlank(htmlText)) {
                String plainText = stripHtmlTags(htmlText);
                if (StringUtils.isNotBlank(plainText)) {
                    builder.append(plainText).append(" ");
                }
            }
        }

        for (Resource child : resource.getChildren()) {
            collectTextFromJcr(child, builder);
        }
    }

    /** Loại bỏ HTML tags và decode HTML entities phổ biến. */
    private String stripHtmlTags(String html) {
        return html
                .replaceAll("<[^>]*>", " ")
                .replace("&nbsp;",  " ")
                .replace("&amp;",   "&")
                .replace("&lt;",    "<")
                .replace("&gt;",    ">")
                .replace("&quot;",  "\"")
                .replace("&#39;",   "'")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
```

**Tại sao đọc JCR trực tiếp thay vì render HTML rồi parse:**

| Tiêu chí | RequestDispatcher + jsoup | JCR Traversal |
|----------|--------------------------|---------------|
| **Hiệu năng** | Tạo sub-request HTTP nội bộ | Đọc từ JCR cache |
| **Rủi ro** | Deadlock khi request pool cạn | Không có |
| **Dependency** | Cần jsoup (không có trong AEM) | Không cần thư viện ngoài |
| **Fragile** | Vỡ khi đổi template HTML | Không bị ảnh hưởng |

---

## Bước 2: Tạo Sling Model — `ArticlePageModel`

Model mỏng — chỉ inject service và `currentPage`, delegate logic hoàn toàn cho service.

File: `core/src/main/java/com/mysite/core/models/schema/ArticlePageModel.java`

```java
package com.mysite.core.models.schema;

import javax.annotation.PostConstruct;

import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;

import org.apache.sling.api.SlingHttpServletRequest;
import com.day.cq.wcm.api.Page;
import com.mysite.core.services.ArticleSchemaExtractor;

@Model(adaptables = SlingHttpServletRequest.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
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
```

**Điểm quan trọng:**
- `defaultInjectionStrategy = OPTIONAL` → nếu service chưa active, model vẫn khởi tạo thành công (trả về `null`).
- **Luôn null-check `schemaExtractor`** trước khi gọi — tránh NPE làm sập toàn bộ `customheaderlibs.html` (mất CSS).
- `@ScriptVariable Page currentPage` — chỉ hoạt động với model `adaptables = SlingHttpServletRequest.class`, không dùng được với `Resource.class`.

---

## Bước 3: `package-info.java` (BẮT BUỘC)

Mỗi Java package trong bundle `core` **phải** có `package-info.java` riêng với `@Version`. Nếu thiếu, bnd không export package → HTL compiler không tìm thấy class → lỗi:

```
com.mysite.core.models.schema.ArticlePageModel cannot be resolved to a type
```

File: `core/src/main/java/com/mysite/core/models/schema/package-info.java`

```java
@Version("1.0")
package com.mysite.core.models.schema;

import org.osgi.annotation.versioning.Version;
```

> **Lưu ý:** Sub-package **không** kế thừa `package-info.java` từ parent package. `com.mysite.core.models` có `package-info.java` không có nghĩa `com.mysite.core.models.schema` cũng được export.

---

## Bước 4: HTL — `customheaderlibs.html`

JSON-LD được render trong `customheaderlibs.html` của **base page component** (`mysite/components/page`). Tất cả page kế thừa đều tự động có schema.

File: `ui.apps/.../apps/mysite/components/page/customheaderlibs.html`

```html
<sly data-sly-use.clientlib="core/wcm/components/commons/v1/templates/clientlib.html">
    <sly data-sly-call="${clientlib.css @ categories='mysite.base'}"/>
</sly>

<sly data-sly-resource="${'contexthub' @ resourceType='granite/contexthub/components/contexthub'}"/>

<!--/* SEO JSON-LD schema */-->
<sly data-sly-use.pageModel="com.mysite.core.models.schema.ArticlePageModel">
    <script type="application/ld+json"
            data-sly-test="${pageModel.schemaJsonOutput}">
        ${pageModel.schemaJsonOutput @ context='unsafe'}
    </script>
</sly>
```

**Giải thích:**
- `data-sly-test="${pageModel.schemaJsonOutput}"` → chỉ render `<script>` khi có output (không render tag rỗng).
- `context='unsafe'` → HTL không escape JSON. An toàn vì dữ liệu đến từ JCR (không phải user input trực tiếp).
- Schema nằm **sau** clientlib CSS → nếu model lỗi, CSS vẫn được load.

### Custom page component

Nếu component con (vd: `custom-page`) cần override `customheaderlibs.html` cho mục đích khác, **luôn include base trước**:

```html
<!--/* Kế thừa base page head (clientlibs, contexthub, JSON-LD schema) */-->
<sly data-sly-include="/apps/mysite/components/page/customheaderlibs.html"/>

<!--/* Thêm code riêng cho custom-page ở đây nếu cần */-->
```

> **Không tạo thêm block `<script type="application/ld+json">` ở component con** — base page đã sinh rồi. Tạo thêm sẽ bị duplicate schema trên trang.

---

## Bước 5: Build và Deploy

```powershell
# Build core bundle
mvn clean install -pl core -PautoInstallBundle

# Build ui.apps
mvn clean install -pl ui.apps -PautoInstallPackage "-Dskip.installnodenpm=true" "-Dskip.npm=true"

# Hoặc full build
mvn clean install -PautoInstallSinglePackage "-Dskip.installnodenpm=true" "-Dskip.npm=true"
```

> **PowerShell:** Các tham số `-D` phải được bọc trong `"..."` để tránh lỗi parse.

---

## Bước 6: Kiểm tra

1. Mở trang bất kỳ ở chế độ **Preview** hoặc **View as Published**.
2. **View Source** → tìm trong `<head>`:
   ```html
   <script type="application/ld+json">
       {"@context":"https://schema.org","@type":"NewsArticle","headline":"...","description":"...","articleBody":"..."}
   </script>
   ```
3. Kiểm tra bằng [Google Rich Results Test](https://search.google.com/test/rich-results).

> **Lưu ý:** JSON-LD có thể không hiện ở Edit mode (AEM editor wrap resource khác so với preview).

---

## Lỗi thường gặp

| Lỗi | Nguyên nhân | Cách sửa |
|-----|-------------|----------|
| `ArticlePageModel cannot be resolved to a type` | Package `models.schema` thiếu `package-info.java` | Thêm `package-info.java` với `@Version` |
| Mất CSS toàn trang | `schemaExtractor` null + thiếu null-check → NPE trong `@PostConstruct` → model fail → HTL dừng render | Luôn check `schemaExtractor != null` trước khi gọi |
| Duplicate JSON-LD | Component con tự sinh schema riêng + include base (đã có schema) | Chỉ sinh schema ở base page, component con chỉ include |
| `articleBody` rỗng | Trang chưa có text component nào | Thêm Text component vào trang và nhập nội dung |
| Schema không hiện ở Edit mode | AEM editor wrap resource → model nhận resource khác | Đây là behavior chuẩn — chỉ cần hiện ở Preview/Publish |

---

## Mở rộng schema type

Hiện tại service tự phân loại dựa trên URL path:
- URL chứa `/blog/` → `BlogPosting`
- Còn lại → `NewsArticle`

Để thêm loại khác (vd: `Product`, `FAQPage`), sửa logic trong `ArticleSchemaExtractorImpl.generateArticleSchema()`:

```java
if (pagePath.contains("/blog/")) {
    schema.addProperty("@type", "BlogPosting");
} else if (pagePath.contains("/product/")) {
    schema.addProperty("@type", "Product");
} else {
    schema.addProperty("@type", "NewsArticle");
}
```

Hoặc đọc `@type` từ page property để author tự chọn trong Page Properties dialog.

---

## Danh sách file

| File | Mục đích |
|------|----------|
| `core/services/ArticleSchemaExtractor.java` | Interface — định nghĩa contract cho service |
| `core/services/impl/ArticleSchemaExtractorImpl.java` | Implementation — traverse JCR, sinh JSON-LD |
| `core/models/schema/ArticlePageModel.java` | Sling Model — inject service, expose getter cho HTL |
| `core/models/schema/package-info.java` | OSGi version marker — bắt buộc để bnd export package |
| `ui.apps/components/page/customheaderlibs.html` | HTL — render `<script type="application/ld+json">` trong `<head>` |

---

## Các file đã xóa (dead code)

Những file sau đã bị xóa trong quá trình refactor vì không còn sử dụng:

| File đã xóa | Lý do |
|-------------|-------|
| `DispatcherExtractorImpl.java` | Cách cũ dùng RequestDispatcher — anti-pattern, rủi ro deadlock |
| `rawbody.html` | Chỉ phục vụ cho DispatcherExtractor |
| `ArticleSchemaModel.java` | Duplicate với ArticlePageModel — gây sinh 2 JSON-LD trên cùng 1 trang |
| `CommonConstants.java` | Orphan — chứa 1 private const không ai dùng |
