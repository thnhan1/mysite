# Hướng dẫn tạo Custom Page Template trong AEM 6.5

Hướng dẫn này được viết dựa trên cấu trúc thực tế của dự án `mysite`. Tên template mẫu sử dụng xuyên suốt là **`custom-page`**. Khi áp dụng cho template mới, thay `custom-page` bằng tên phù hợp (ví dụ: `article-page`, `landing-page`).

---

## Tổng quan kiến trúc

Một Custom Page Template hoàn chỉnh trong AEM gồm **2 phần chính** nằm ở 2 module khác nhau:

```
mysite/
├── ui.apps/   → Chứa Component Definition (code/markup)
└── ui.content/ → Chứa Template Definition (cấu hình, structure, policies)
```

### Sơ đồ phụ thuộc

```
Template (/conf/mysite/.../templates/custom-page)
    └── sling:resourceType → Component (apps/mysite/components/custom-page)
            └── sling:resourceSuperType → apps/mysite/components/page
                    └── sling:resourceSuperType → core/wcm/components/page/v3/page
```

---

## PHẦN 1 — Component Definition (`ui.apps`)

### 1.1. Component chính

**File:** `ui.apps/src/main/content/jcr_root/apps/mysite/components/custom-page/.content.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
    jcr:primaryType="cq:Component"
    jcr:title="Custom Page"
    sling:resourceSuperType="mysite/components/page"
    componentGroup=".hidden"/>
```

> **Giải thích:**
> - `jcr:primaryType="cq:Component"` — Khai báo đây là một AEM Component.
> - `jcr:title` — Tên hiển thị trong Template Editor.
> - `sling:resourceSuperType="mysite/components/page"` — Kế thừa toàn bộ render logic từ base page component (header, footer, clientlibs, ContextHub). Không cần viết lại `body.html`, `head.html`, v.v.
> - `componentGroup=".hidden"` — Ẩn component này khỏi Component Browser (chỉ dùng làm page type).

---

### 1.2. Custom Header Libs

File này inject thêm các script/style vào `<head>` của trang, chạy **sau** `customheaderlibs.html` của component cha.

**File:** `ui.apps/src/main/content/jcr_root/apps/mysite/components/custom-page/customheaderlibs.html`

```html
<!--/* Inherit the base page head (clientlibs, contexthub) */-->
<sly data-sly-include="/apps/mysite/components/page/customheaderlibs.html"/>

<!--/* SEO Article JSON-LD schema */-->
<sly data-sly-use.schema="com.mysite.core.models.schema.ArticleSchemaModel">
    <script type="application/ld+json">${schema.jsonLd @ context='unsafe'}</script>
</sly>
```

> **Giải thích:**
> - Dòng `data-sly-include` — Bắt buộc phải có để kế thừa base clientlibs (`mysite.base`) và ContextHub. Nếu bỏ qua, trang sẽ không load được CSS/JS.
> - `data-sly-use.schema` — Gọi Sling Model Java để render JSON-LD (SEO structured data). Nếu template không cần schema, xóa block `<sly>` này đi.
> - Để thêm custom CSS chỉ cho template này: thêm `<sly data-sly-call="${clientlib.css @ categories='mysite.custom-page'}"/>` vào đây.

---

### 1.3. Page Dialog (Custom Properties Tab)

Dialog cho phép tác giả nhập thêm các trường tùy chỉnh (ngoài các trường mặc định của Core Component).

**File:** `ui.apps/src/main/content/jcr_root/apps/mysite/components/custom-page/_cq_dialog/.content.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="nt:unstructured"
    jcr:title="Page"
    sling:resourceType="cq/gui/components/authoring/dialog"
    sling:resourceSuperType="core/wcm/components/page/v3/page/cq:dialog">
    <content
        jcr:primaryType="nt:unstructured"
        sling:resourceType="granite/ui/components/coral/foundation/container">
        <items jcr:primaryType="nt:unstructured">
            <tabs
                jcr:primaryType="nt:unstructured"
                sling:resourceType="granite/ui/components/coral/foundation/tabs">
                <items jcr:primaryType="nt:unstructured">
                    <custom
                        jcr:primaryType="nt:unstructured"
                        jcr:title="Custom Properties"
                        sling:resourceType="granite/ui/components/coral/foundation/fixedcolumns">
                        <items jcr:primaryType="nt:unstructured">
                            <column
                                jcr:primaryType="nt:unstructured"
                                sling:resourceType="granite/ui/components/coral/foundation/container">
                                <items jcr:primaryType="nt:unstructured">
                                    <subTitle
                                        jcr:primaryType="nt:unstructured"
                                        sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                                        cq:showOnCreate="{Boolean}true"
                                        fieldLabel="Sub Title"
                                        name="./subTitle"
                                        emptyText="Enter sub title"/>
                                    <subDescription
                                        jcr:primaryType="nt:unstructured"
                                        sling:resourceType="granite/ui/components/coral/foundation/form/textarea"
                                        cq:showOnCreate="{Boolean}true"
                                        fieldLabel="Sub Description"
                                        name="./subDescription"
                                        emptyText="Enter sub description"/>
                                    <customName
                                        jcr:primaryType="nt:unstructured"
                                        sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                                        cq:showOnCreate="{Boolean}true"
                                        fieldLabel="Name"
                                        name="./customName"
                                        emptyText="Enter name"/>
                                </items>
                            </column>
                        </items>
                    </custom>
                </items>
            </tabs>
        </items>
    </content>
</jcr:root>
```

> **Giải thích:**
> - `sling:resourceSuperType="core/wcm/components/page/v3/page/cq:dialog"` — Kế thừa toàn bộ dialog gốc (Basic, SEO, Social Media, Cloud Services...). Tab mới sẽ được **merge** vào.
> - `name="./subTitle"` — Giá trị sẽ được lưu vào property `subTitle` trực tiếp trên node `jcr:content` của page.
> - `cq:showOnCreate="{Boolean}true"` — Trường này xuất hiện trong wizard "Create Page".
> - Có thể thêm nhiều field types: `select`, `checkbox`, `pathfield`, `datepicker`, v.v. bằng cách thay `sling:resourceType`.

---

## PHẦN 2 — Template Definition (`ui.content`)

### 2.1. Template Node chính

**File:** `ui.content/src/main/content/jcr_root/conf/mysite/settings/wcm/templates/custom-page/.content.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
    jcr:primaryType="cq:Template">
    <jcr:content
        cq:lastModified="{Date}2026-06-13T12:00:00.000+07:00"
        cq:lastModifiedBy="admin"
        cq:templateType="/conf/mysite/settings/wcm/template-types/page"
        jcr:description="Custom Page Template with additional properties (Sub Title, Sub Description, Name)"
        jcr:primaryType="cq:PageContent"
        jcr:title="Custom Page Template"
        status="enabled"/>
</jcr:root>
```

> **Giải thích:**
> - `jcr:primaryType="cq:Template"` — Khai báo node là một Editable Template.
> - `cq:templateType` — Trỏ đến Template Type dùng làm nền tảng (ở dự án này là `/conf/mysite/settings/wcm/template-types/page`).
> - `status="enabled"` — Template có thể được sử dụng để tạo page. Dùng `"disabled"` để ẩn.
> - `jcr:title` và `jcr:description` — Hiển thị trong wizard "Create Page".

---

### 2.2. Template Structure (Bố cục cố định)

Định nghĩa các component **bắt buộc / không thể xóa** của template (header, footer, vùng editable).

**File:** `ui.content/src/main/content/jcr_root/conf/mysite/settings/wcm/templates/custom-page/structure/.content.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="cq:Page">
    <jcr:content
        cq:deviceGroups="[mobile/groups/responsive]"
        cq:template="/conf/mysite/settings/wcm/templates/custom-page"
        jcr:primaryType="cq:PageContent"
        sling:resourceType="mysite/components/custom-page">
        <root
            jcr:primaryType="nt:unstructured"
            sling:resourceType="mysite/components/container"
            layout="responsiveGrid">
            <!-- Header (Experience Fragment, không editable) -->
            <experiencefragment-header
                jcr:primaryType="nt:unstructured"
                sling:resourceType="mysite/components/experiencefragment"
                fragmentVariationPath="/content/experience-fragments/mysite/language-masters/en/site/header/master"/>
            <!-- Vùng nội dung chính (editable) -->
            <container
                jcr:primaryType="nt:unstructured"
                sling:resourceType="mysite/components/container"
                layout="responsiveGrid">
                <!-- Title cố định, tác giả có thể chỉnh sửa nội dung -->
                <title
                    jcr:primaryType="nt:unstructured"
                    sling:resourceType="mysite/components/title"
                    editable="{Boolean}true"/>
                <!-- Container editable cho tác giả thêm component -->
                <container
                    jcr:primaryType="nt:unstructured"
                    sling:resourceType="mysite/components/container"
                    editable="{Boolean}true"
                    layout="responsiveGrid"/>
            </container>
            <!-- Footer (Experience Fragment, không editable) -->
            <experiencefragment-footer
                jcr:primaryType="nt:unstructured"
                sling:resourceType="mysite/components/experiencefragment"
                fragmentVariationPath="/content/experience-fragments/mysite/language-masters/en/site/footer/master"/>
        </root>
        <!-- Responsive Breakpoints -->
        <cq:responsive jcr:primaryType="nt:unstructured">
            <breakpoints jcr:primaryType="nt:unstructured">
                <phone
                    jcr:primaryType="nt:unstructured"
                    title="Smaller Screen"
                    width="{Long}768"/>
                <tablet
                    jcr:primaryType="nt:unstructured"
                    title="Tablet"
                    width="{Long}1200"/>
            </breakpoints>
        </cq:responsive>
    </jcr:content>
</jcr:root>
```

> **Giải thích:**
> - `sling:resourceType="mysite/components/custom-page"` — Liên kết template với component đã tạo ở Phần 1.
> - `experiencefragment-header` / `experiencefragment-footer` — Không có `editable="{Boolean}true"` → Template Author không thể xóa, Content Author không thể sửa.
> - `editable="{Boolean}true"` — Đánh dấu component/container có thể được Template Author bật/tắt, Content Author thêm nội dung.
> - `cq:responsive/breakpoints` — Định nghĩa breakpoint cho Responsive Grid (layout editor).

---

### 2.3. Initial Content (Nội dung khởi tạo)

Nội dung được **copy** vào trang mới khi tác giả tạo page từ template này. Khác với Structure, nội dung này có thể bị xóa.

**File:** `ui.content/src/main/content/jcr_root/conf/mysite/settings/wcm/templates/custom-page/initial/.content.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="cq:Page">
    <jcr:content
        cq:template="/conf/mysite/settings/wcm/templates/custom-page"
        jcr:primaryType="cq:PageContent"
        sling:resourceType="mysite/components/custom-page">
        <root
            jcr:primaryType="nt:unstructured"
            sling:resourceType="mysite/components/container"
            layout="responsiveGrid">
            <container
                jcr:primaryType="nt:unstructured"
                sling:resourceType="mysite/components/container"
                layout="responsiveGrid">
                <title
                    jcr:primaryType="nt:unstructured"
                    sling:resourceType="mysite/components/title"/>
                <container
                    jcr:primaryType="nt:unstructured"
                    sling:resourceType="mysite/components/container"
                    layout="responsiveGrid"/>
            </container>
        </root>
    </jcr:content>
</jcr:root>
```

> **Lưu ý:** `initial` không có header/footer vì đây là nội dung trang (không phải structure). Header/footer đã được kế thừa từ `structure`.

---

### 2.4. Policy Mappings cho Template

Ánh xạ từng component trong structure tới policy cụ thể đã định nghĩa trong policies.xml.

**File:** `ui.content/src/main/content/jcr_root/conf/mysite/settings/wcm/templates/custom-page/policies/.content.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="cq:Page">
    <jcr:content
        cq:lastModified="{Date}2026-06-13T12:00:00.000+07:00"
        cq:lastModifiedBy="admin"
        cq:policy="mysite/components/page/policy"
        jcr:primaryType="nt:unstructured"
        sling:resourceType="wcm/core/components/policies/mappings">
        <root
            cq:policy="mysite/components/container/policy_1574694950110"
            jcr:primaryType="nt:unstructured"
            sling:resourceType="wcm/core/components/policies/mapping">
            <experiencefragment-header
                cq:policy="mysite/components/experiencefragment/policy_header"
                jcr:primaryType="nt:unstructured"
                sling:resourceType="wcm/core/components/policies/mapping"/>
            <experiencefragment-footer
                cq:policy="mysite/components/experiencefragment/policy_footer"
                jcr:primaryType="nt:unstructured"
                sling:resourceType="wcm/core/components/policies/mapping"/>
            <container
                cq:policy="mysite/components/container/policy_649128221558427"
                jcr:primaryType="nt:unstructured"
                sling:resourceType="wcm/core/components/policies/mapping">
                <title
                    cq:policy="mysite/components/title/policy_641475696923109"
                    jcr:primaryType="nt:unstructured"
                    sling:resourceType="wcm/core/components/policies/mapping"/>
                <container
                    cq:policy="mysite/components/container/policy_1574695586800"
                    jcr:primaryType="nt:unstructured"
                    sling:resourceType="wcm/core/components/policies/mapping">
                    <mysite jcr:primaryType="nt:unstructured">
                        <components jcr:primaryType="nt:unstructured">
                            <title
                                cq:policy="mysite/components/title/policy_641528232375303"
                                jcr:primaryType="nt:unstructured"
                                sling:resourceType="wcm/core/components/policies/mapping"/>
                            <text
                                cq:policy="mysite/components/text/policy_641562756958017"
                                jcr:primaryType="nt:unstructured"
                                sling:resourceType="wcm/core/components/policies/mapping"/>
                            <image
                                cq:policy="mysite/components/image/policy_651483963895698"
                                jcr:primaryType="nt:unstructured"
                                sling:resourceType="wcm/core/components/policies/mapping"/>
                            <teaser
                                cq:policy="mysite/components/teaser/policy_1575031387650"
                                jcr:primaryType="nt:unstructured"
                                sling:resourceType="wcm/core/components/policies/mapping"/>
                            <download
                                cq:policy="mysite/components/download/policy_1575032193319"
                                jcr:primaryType="nt:unstructured"
                                sling:resourceType="wcm/core/components/policies/mapping"/>
                        </components>
                    </mysite>
                </container>
            </container>
        </root>
    </jcr:content>
</jcr:root>
```

> **Giải thích:**
> - `cq:policy` — Đường dẫn tương đối đến policy node trong `policies/.content.xml` (xem Phần 3).
> - Hierarchy của policy mappings phải **khớp với hierarchy node trong `structure`**.
> - `cq:policy="mysite/components/page/policy"` trên `jcr:content` — Gán policy cho page component (clientlibs).

---

## PHẦN 3 — Global Policies

Định nghĩa tập trung tất cả policy cho mọi component trong site. Template sẽ tham chiếu đến các policy này.

**File:** `ui.content/src/main/content/jcr_root/conf/mysite/settings/wcm/policies/.content.xml`

File này chứa toàn bộ policy definition theo cấu trúc:

```
/mysite/components/
    ├── page/
    │   └── policy          → Page clientlibs (mysite.dependencies, mysite.site)
    ├── container/
    │   ├── policy_1574694950110   → Page Root (cho phép group: My Site - Content + Structure)
    │   ├── policy_1574695586800   → Page Content (cho phép group: My Site - Content)
    │   ├── policy_649128221558427 → Page Main (<main> element)
    │   └── policy_1575040440977   → XF Root
    ├── title/
    │   ├── policy_641475696923109 → Page Title (chỉ H1, no link)
    │   └── policy_641528232375303 → Content Title (H2–H6)
    ├── text/
    │   └── policy_641562756958017 → Content Text (RTE config)
    ├── image/
    │   └── policy_651483963895698 → Content Image (widths, lazy load, crop ratios)
    ├── teaser/
    │   └── policy_1575031387650   → Content Teaser (H3 title)
    ├── download/
    │   └── policy_1575032193319   → Content Download
    └── experiencefragment/
        ├── policy_header          → Page Header (<header> element)
        └── policy_footer          → Page Footer (<footer> element)
```

> **Lưu ý:** Khi tạo template mới có component policy chưa tồn tại, cần thêm policy node vào file này trước khi tham chiếu trong `templates/custom-page/policies/.content.xml`.

---

## PHẦN 4 — Client Libraries (`ui.apps`)

### 4.1. clientlib-base (mysite.base)

Clientlib cốt lõi được load trong `<head>` của mọi page qua `customheaderlibs.html`.

**File:** `ui.apps/src/main/content/jcr_root/apps/mysite/clientlibs/clientlib-base/.content.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
    jcr:primaryType="cq:ClientLibraryFolder"
    allowProxy="{Boolean}true"
    categories="[mysite.base]"
    embed="[core.wcm.components.accordion.v1,
            core.wcm.components.tabs.v1,
            core.wcm.components.carousel.v1,
            core.wcm.components.image.v3,
            core.wcm.components.breadcrumb.v2,
            core.wcm.components.search.v2,
            core.wcm.components.form.text.v2,
            core.wcm.components.pdfviewer.v1,
            core.wcm.components.form.container.v2,
            core.wcm.components.text.v2,
            core.wcm.components.embed.v1,
            core.wcm.components.commons.site.link,
            mysite.grid]"/>
```

### 4.2. Page Policy Clientlibs

Page clientlibs (`mysite.dependencies`, `mysite.site`) được load qua policy của page component:

**Trong** `ui.content/.../policies/.content.xml`:
```xml
<page jcr:primaryType="nt:unstructured">
    <policy
        jcr:title="Generic Page"
        sling:resourceType="wcm/core/components/policy/policy"
        clientlibs="[mysite.dependencies,mysite.site]"
        clientlibsJsHead="mysite.dependencies">
        <jcr:content jcr:primaryType="nt:unstructured"/>
    </policy>
</page>
```

---

## PHẦN 5 — Sling Model Java (nếu cần logic)

Nếu template cần xử lý dữ liệu (ví dụ: SEO JSON-LD), tạo Sling Model trong `core` module.

**File:** `core/src/main/java/com/mysite/core/models/schema/ArticleSchemaModel.java`

```java
package com.mysite.core.models.schema;

import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import javax.annotation.PostConstruct;

@Model(adaptables = SlingHttpServletRequest.class)
public class ArticleSchemaModel {

    @ScriptVariable
    private Page currentPage;

    private String jsonLd;

    @PostConstruct
    protected void init() {
        // Đọc properties từ page: subTitle, subDescription, customName
        String title = currentPage.getTitle();
        String subTitle = currentPage.getProperties().get("subTitle", String.class);
        
        jsonLd = String.format(
            "{\"@context\":\"https://schema.org\",\"@type\":\"Article\",\"headline\":\"%s\",\"description\":\"%s\"}",
            title != null ? title : "",
            subTitle != null ? subTitle : ""
        );
    }

    public String getJsonLd() {
        return jsonLd;
    }
}
```

---

## PHẦN 6 — Checklist & Thứ tự triển khai

> [!IMPORTANT]
> Thực hiện theo đúng thứ tự dưới đây để tránh lỗi dependency.

### Bước 1 — `ui.apps`: Tạo Component

- [ ] Tạo thư mục: `ui.apps/.../components/custom-page/`
- [ ] Tạo: `.content.xml` (Component Definition)
- [ ] Tạo: `customheaderlibs.html` (include base + thêm script tùy chỉnh)
- [ ] Tạo thư mục: `_cq_dialog/`
- [ ] Tạo: `_cq_dialog/.content.xml` (thêm tab dialog tùy chỉnh)

### Bước 2 — `ui.content`: Thêm Policies (nếu cần mới)

- [ ] Chỉnh sửa: `ui.content/.../policies/.content.xml`
- [ ] Thêm policy mới cho các component cần cấu hình đặc biệt

### Bước 3 — `ui.content`: Tạo Template

- [ ] Tạo thư mục: `ui.content/.../templates/custom-page/`
- [ ] Tạo: `.content.xml` (Template metadata, status, templateType)
- [ ] Tạo thư mục: `structure/`
- [ ] Tạo: `structure/.content.xml` (layout trang: header, content zones, footer)
- [ ] Tạo thư mục: `initial/`
- [ ] Tạo: `initial/.content.xml` (nội dung mặc định khi tạo page)
- [ ] Tạo thư mục: `policies/`
- [ ] Tạo: `policies/.content.xml` (ánh xạ component → policy)

### Bước 4 — `core`: Tạo Sling Model (nếu cần)

- [ ] Tạo Java class trong `core/.../models/`
- [ ] Cập nhật `customheaderlibs.html` để gọi model

### Bước 5 — Deploy & Kiểm tra

```bash
# Build và deploy toàn bộ project
mvn clean install -PautoInstallSinglePackage

# Hoặc chỉ deploy ui.apps + ui.content
mvn clean install -PautoInstallPackage -pl ui.apps,ui.content
```

- [ ] Vào AEM → Sites → Create Page → Kiểm tra template "Custom Page Template" xuất hiện
- [ ] Tạo thử 1 page, kiểm tra layout và dialog
- [ ] Verify JSON-LD xuất hiện trong `<head>` (nếu có schema model)
- [ ] Kiểm tra Template Editor tại: `/editor.html/conf/mysite/settings/wcm/templates/custom-page/structure`

---

## Phụ lục: Cấu trúc file đầy đủ

```
mysite/
├── ui.apps/src/main/content/jcr_root/apps/mysite/
│   ├── clientlibs/
│   │   └── clientlib-base/
│   │       └── .content.xml                    ← categories="[mysite.base]"
│   └── components/
│       ├── page/
│       │   ├── .content.xml                    ← base page, resourceSuperType=core/wcm/...
│       │   ├── customheaderlibs.html           ← load mysite.base + ContextHub
│       │   └── customfooterlibs.html           ← load mysite.base JS async
│       └── custom-page/
│           ├── .content.xml                    ← [NEW] resourceSuperType=mysite/components/page
│           ├── customheaderlibs.html           ← [NEW] include parent + inject JSON-LD
│           └── _cq_dialog/
│               └── .content.xml                ← [NEW] thêm tab "Custom Properties"
│
└── ui.content/src/main/content/jcr_root/
    └── conf/mysite/settings/wcm/
        ├── template-types/
        │   └── page/
        │       └── .content.xml                ← Template Type (nền tảng)
        ├── policies/
        │   └── .content.xml                    ← Tất cả policies của site
        └── templates/
            └── custom-page/                    ← [NEW] Custom Page Template
                ├── .content.xml                ← [NEW] Template metadata
                ├── structure/
                │   └── .content.xml            ← [NEW] Bố cục cố định
                ├── initial/
                │   └── .content.xml            ← [NEW] Nội dung khởi tạo
                └── policies/
                    └── .content.xml            ← [NEW] Ánh xạ policies
```
