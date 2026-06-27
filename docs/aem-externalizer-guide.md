# AEM Externalizer — Hướng dẫn sử dụng trong dự án mysite

## 1. Externalizer là gì?

`com.day.cq.commons.Externalizer` là OSGi service của AEM dùng để **chuyển đổi đường dẫn JCR nội bộ thành URL tuyệt đối** có schema (`http://` / `https://`) và domain thật.

### Khi nào PHẢI dùng Externalizer?

| Tình huống | Ví dụ |
|---|---|
| URL trong **email / newsletter** | Link unsubscribe, link xem bài viết |
| **SEO structured data** (JSON-LD) | `"url": "https://mysite.com/content/..."` |
| **Sitemap XML** generation | `<loc>https://mysite.com/content/...</loc>` |
| **Open Graph / Social meta tags** | `og:url`, `og:image` |
| Link trong **PDF export** | Không thể dùng relative path |
| **Background job / Workflow** không có HTTP request context | Không có `request.getServerName()` |

> **Quy tắc vàng:** Bất cứ khi nào code chạy **ngoài request context** (workflow, scheduler, email service) hoặc cần URL tuyệt đối cho external system → **luôn dùng Externalizer**, không hardcode domain.

---

## 2. Cấu hình OSGi

### Cấu trúc thư mục config theo môi trường

```
ui.config/src/main/content/jcr_root/apps/mysite/osgiconfig/
├── config/                          ← Áp dụng cho TẤT CẢ môi trường (dev local)
│   └── com.day.cq.commons.impl.ExternalizerImpl.cfg.json
├── config.author/                   ← Chỉ áp dụng cho Author instance
│   └── (không cần Externalizer riêng vì dùng chung với config/)
├── config.stage/                    ← Staging environment
│   └── com.day.cq.commons.impl.ExternalizerImpl.cfg.json
└── config.prod/                     ← Production environment
    └── com.day.cq.commons.impl.ExternalizerImpl.cfg.json
```

> AEM chọn config theo thứ tự ưu tiên: `config.prod` > `config.stage` > `config.author` > `config`  
> RunMode được set khi khởi động AEM bằng `-Dsling.run.modes=author,prod`

---

### 2.1. Local Development

**File:** `ui.config/.../osgiconfig/config/com.day.cq.commons.impl.ExternalizerImpl.cfg.json`

```json
{
    "externalizer.domains": [
        "local http://localhost:4502",
        "author http://localhost:4502",
        "publish http://localhost:4503"
    ],
    "externalizer.encodedpath": true
}
```

### 2.2. Staging Environment

**File:** `ui.config/.../osgiconfig/config.stage/com.day.cq.commons.impl.ExternalizerImpl.cfg.json`

```json
{
    "externalizer.domains": [
        "local https://stage-author.mysite.com",
        "author https://stage-author.mysite.com",
        "publish https://stage.mysite.com"
    ],
    "externalizer.encodedpath": true
}
```

### 2.3. Production Environment

**File:** `ui.config/.../osgiconfig/config.prod/com.day.cq.commons.impl.ExternalizerImpl.cfg.json`

```json
{
    "externalizer.domains": [
        "local https://author.mysite.com",
        "author https://author.mysite.com",
        "publish https://www.mysite.com"
    ],
    "externalizer.encodedpath": true
}
```

---

### 2.4. Giải thích từng property

#### `externalizer.domains`

Format mỗi entry: `"<tên-domain> <scheme>://<host>[:<port>][/<contextPath>]"`

| Tên domain | Dùng với method | Ý nghĩa |
|---|---|---|
| `local` | `externalizer.externalLink(...)` | URL của instance hiện tại (author hoặc publish) |
| `author` | `externalizer.authorLink(...)` | URL của Author instance |
| `publish` | `externalizer.publishLink(...)` | URL của Publish instance |

> **Context path** (`/contextPath`) chỉ thêm khi AEM được deploy dưới sub-path.  
> Ví dụ deploy AEM trong Docker với nginx proxy: `"publish https://www.mysite.com/site"` → URL ra sẽ là `https://www.mysite.com/site/content/...`  
> Môi trường AEM mặc định (root path) → **không thêm context path**.

#### `externalizer.encodedpath`

- `true` — Encode các ký tự đặc biệt trong path (space → `%20`, v.v.). **Luôn dùng `true` trên production**.
- `false` — Giữ nguyên path, có thể gây broken URL nếu path chứa ký tự đặc biệt.

---

## 3. Các method của Externalizer API

```java
// 1. publishLink — Tạo URL absolute trỏ đến Publish instance
//    Dùng cho: Email links, SEO, Sitemap, Social meta
String url = externalizer.publishLink(resourceResolver, "/content/mysite/page.html");
// → https://www.mysite.com/content/mysite/page.html

// 2. authorLink — Tạo URL absolute trỏ đến Author instance
//    Dùng cho: Notification email gửi đến content editor, approval workflow
String url = externalizer.authorLink(resourceResolver, "/content/mysite/page.html");
// → https://author.mysite.com/content/mysite/page.html

// 3. externalLink — Dùng domain tùy chỉnh
//    Dùng cho: Multi-domain setup, custom domain mapping
String url = externalizer.externalLink(resourceResolver, "publish", "/content/mysite/page.html");
// → https://www.mysite.com/content/mysite/page.html

// 4. absoluteLink — Dùng scheme+host của request hiện tại (KHÔNG dùng config domain)
//    Dùng cho: Khi có HTTP request context và chỉ cần absolute của request đó
String url = externalizer.absoluteLink(request, request.getScheme(), "/content/mysite/page.html");
```

> **Lưu ý:** Method `publishLink` và `authorLink` **luôn ưu tiên** hơn `absoluteLink` trong production vì chúng lấy domain từ OSGi config, không phụ thuộc vào host của HTTP request (có thể là internal load balancer IP).

---

## 4. Cách dùng trong Sling Model

### Pattern chuẩn trong dự án

```java
// File: core/src/main/java/com/mysite/core/models/NewsletterModel.java

@Getter
@Model(
    adaptables = SlingHttpServletRequest.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class NewsletterModel {

    @OSGiService
    private Externalizer externalizer;          // Inject OSGi service

    @SlingObject
    private ResourceResolver resourceResolver;  // Cần thiết cho Externalizer

    @ScriptVariable
    private Page currentPage;                   // Trang hiện tại

    @ValueMapValue(name = "articlePath")
    private String articlePath;                 // Path được author chọn qua dialog

    private String absolutePublishUrl;

    @PostConstruct
    protected void init() {
        // Ưu tiên dùng articlePath nếu được set, fallback về path trang hiện tại
        String targetPath = (articlePath != null && articlePath.startsWith("/content"))
                ? articlePath
                : (currentPage != null ? currentPage.getPath() : null);

        if (targetPath != null) {
            // Append .html extension trước khi externalize
            String pathWithExtension = targetPath + ".html";
            this.absolutePublishUrl = externalizer.publishLink(resourceResolver, pathWithExtension);
        }
    }
}
```

### Các lưu ý khi dùng trong Sling Model

```java
// ✅ ĐÚNG: adaptables = SlingHttpServletRequest.class cho phép inject @ScriptVariable
@Model(adaptables = SlingHttpServletRequest.class, ...)

// ❌ SAI: adaptables = Resource.class không hỗ trợ inject @ScriptVariable (currentPage)
@Model(adaptables = Resource.class, ...)

// ✅ ĐÚNG: Luôn append extension trước khi externalize
String url = externalizer.publishLink(resolver, path + ".html");
// → https://www.mysite.com/content/mysite/page.html

// ❌ SAI: Externalize path không có extension → AEM Dispatcher có thể block
String url = externalizer.publishLink(resolver, path);
// → https://www.mysite.com/content/mysite/page  (Dispatcher thường block non-extension URL)

// ✅ ĐÚNG: Validate path trước khi externalize
if (path != null && path.startsWith("/content")) {
    url = externalizer.publishLink(resolver, path + ".html");
}

// ❌ SAI: Externalize path ngoài /content (external URL, fragment path...)
url = externalizer.publishLink(resolver, "https://external.com"); // → sẽ ra URL sai
```

---

## 5. Dùng trong workflow ngoài request context

Khi chạy trong OSGi Service, Scheduler, Workflow — **không có HTTP request** → không thể dùng `absoluteLink`. Chỉ dùng `publishLink` / `authorLink`:

```java
@Component(service = NewsletterEmailService.class)
public class NewsletterEmailService {

    @Reference
    private Externalizer externalizer;

    @Reference
    private ResourceResolverFactory resolverFactory;

    public String buildEmailLink(String contentPath) {
        // Mở service ResourceResolver (không có request)
        Map<String, Object> param = new HashMap<>();
        param.put(ResourceResolverFactory.SUBSERVICE, "newsletter-service");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(param)) {
            return externalizer.publishLink(resolver, contentPath + ".html");
            // → https://www.mysite.com/content/mysite/en/article.html
        } catch (LoginException e) {
            log.error("Cannot open service resolver", e);
            return null;
        }
    }
}
```

> Cần cấu hình **Service User Mapping** trong OSGi để `newsletter-service` có quyền đọc `/content`.

---

## 6. Dùng trong HTL (Sightly)

```html
<!--/* Khởi tạo model */-->
<sly data-sly-use.newsletter="com.mysite.core.models.NewsletterModel"/>

<!--/* Hiển thị URL — dùng data-sly-test để tránh render khi null */-->
<div data-sly-test="${newsletter.absolutePublishUrl}">
    Publish URL: ${newsletter.absolutePublishUrl}
</div>

<!--/* Dùng trong href — context='uri' để encode đúng */-->
<a href="${newsletter.absolutePublishUrl @ context='uri'}">Read article</a>

<!--/* Dùng trong JSON-LD — context='unsafe' vì là JSON trong script tag */-->
<script type="application/ld+json">${schema.jsonLd @ context='unsafe'}</script>
```

> **HTL Context quan trọng:**
> - `@ context='uri'` — Dùng cho `href`, `src`, `action`. Encode URL theo RFC 3986.
> - `@ context='unsafe'` — Dùng cho JSON trong `<script>` tag. Không encode.
> - Mặc định (không có context) — HTML-encode, an toàn cho text content.

---

## 7. Checklist production deployment

- [ ] **Không hardcode domain** trong code Java hoặc HTL. Luôn dùng Externalizer.
- [ ] Tạo file `config.prod/com.day.cq.commons.impl.ExternalizerImpl.cfg.json` với domain thật.
- [ ] Dùng `https://` cho tất cả domain trên production.
- [ ] Kiểm tra AEM RunMode: `-Dsling.run.modes=author,prod` (author) / `-Dsling.run.modes=publish,prod` (publish).
- [ ] Kiểm tra output URL tại: **AEM → Tools → Operations → Web Console → Configuration → Day CQ Link Externalizer**.
- [ ] `externalizer.encodedpath = true` trên tất cả môi trường production.
- [ ] Nếu dùng CDN / custom domain, `publish` domain phải là CDN domain (không phải IP nội bộ).

---

## 8. Kiểm tra nhanh tại AEM Console

1. Truy cập: `http://localhost:4502/system/console/configMgr`
2. Tìm: **"Day CQ Link Externalizer"**
3. Xác nhận giá trị `Domains` đúng với môi trường.

Hoặc test qua Groovy Console (nếu đã cài AEM Groovy Console):

```groovy
def externalizer = getService(com.day.cq.commons.Externalizer)
def resolver = resourceResolver
println externalizer.publishLink(resolver, "/content/mysite/en.html")
// Expected: https://www.mysite.com/content/mysite/en.html
```
