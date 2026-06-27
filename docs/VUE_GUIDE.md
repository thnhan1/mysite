# Vue 3 Component Development Guide for AEM + Vite

Hướng dẫn viết Vue 3 component trong module `ui.frontend`, tích hợp với AEM HTL.
Pattern: **HTL render markup gốc → Vue mount vào element có sẵn qua `data-cmp-is`**.

---

## Kiến trúc tổng quan

```
┌──────────────────────────────────────────────────────────┐
│ AEM Page                                                  │
│                                                          │
│  ┌─ customheaderlibs.html ────────────────────────────┐  │
│  │  <link rel="stylesheet" href="...site.[hash].css">  │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  <div data-cmp-is="ms-demo" data-name="Nguyen Van A">  │
│    <noscript>Nguyen Van A</noscript>  ← SSR fallback   │
│  </div>                                                  │
│                                                          │
│  ┌─ customfooterlibs.html ────────────────────────────┐  │
│  │  <script src="...site.[hash].js"></script>          │  │
│  │  → main.ts import.meta.glob '**/*.mount.ts'        │  │
│  │  → demo.mount.ts: createApp(Demo, {name})          │  │
│  │  → Demo.vue mounts vào [data-cmp-is="ms-demo"]     │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

---

## Cấu trúc thư mục cho Vue component

```
ui.frontend/src/main/webpack/              ← Đổi thành src/main/vite sau khi rename
├── site/
│   ├── main.ts                            ← Entry: import.meta.glob auto-discovery
│   ├── main.scss
│   └── ...
├── components/
│   ├── demo/                              ← Mỗi Vue component 1 thư mục riêng
│   │   ├── demo.vue                       ← Vue SFC (template + script + style)
│   │   └── demo.mount.ts                  ← createApp + mount vào DOM element
│   ├── productlist/
│   │   ├── productlist.vue
│   │   └── productlist.mount.ts
│   ├── _accordion.scss                    ← SCSS-only component (không Vue)
│   └── ...
├── composables/                           ← Shared Vue composables
│   ├── useDomObserver.ts                  ← Watch DOM changes (cho AEM authoring)
│   └── useAemModel.ts                     ← Gọi Sling Model Exporter
├── assets/
│   ├── fonts/
│   └── images/
└── vue-shims.d.ts                         ← TypeScript declaration cho .vue files
```

---

## Pattern 1: Component đơn giản với data attributes

Dữ liệu từ Sling Model → HTL render vào `data-*` attributes → Vue component đọc qua props.

### 1a. HTL template

```html
<!-- helloworld.html -->
<div class="ms-demo" data-cmp-is="ms-demo" data-name="${properties.name}">
    <noscript>${properties.name}</noscript>
</div>
```

### 1b. Vue SFC

```vue
<!-- demo.vue -->
<template>
    <div class="ms-demo__content">
        xin chào {{ name }}
    </div>
</template>

<script>
export default {
    name: 'MsDemo',
    props: {
        name: {
            type: String,
            default: 'Nguyen Van A',
        },
    },
    mounted() {
        console.log('[ms-demo] mounted with name:', this.name);
    },
};
</script>

<style scoped>
.ms-demo__content {
    padding: 1rem;
    border: 2px solid #42b883;
    border-radius: 6px;
}
</style>
```

### 1c. Mount script

```ts
// demo.mount.ts
import { createApp } from 'vue';
import Demo from './demo.vue';

export function mountDemo() {
  const elements = document.querySelectorAll('[data-cmp-is="ms-demo"]');

  elements.forEach((el) => {
    const root = el as HTMLElement;

    // Đọc props từ data-* attributes (do HTL render)
    const name = root.dataset.name || 'Nguyen Van A';

    const app = createApp(Demo, { name });
    app.mount(root);
  });
}

// Auto-mount khi script load + DOM sẵn sàng
if (document.readyState !== 'loading') {
  mountDemo();
} else {
  document.addEventListener('DOMContentLoaded', mountDemo);
}
```

---

## Pattern 2: Component với JSON data (dữ liệu phức tạp)

Khi dữ liệu là object/array → dùng JSON script tag thay vì data attributes.

### 2a. HTL template

```html
<div data-cmp-is="productlist">
    <script type="application/json" data-cmp-hook-productlist="data">
        ${productListModel.json @ context='unsafe'}
    </script>
</div>
```

### 2b. Sling Model (Java)

```java
@Model(adaptables = Resource.class)
public class ProductListModel {
    public String getJson() {
        List<Product> products = getProducts();
        return new Gson().toJson(Map.of("products", products));
    }
}
```

### 2c. Mount script

```ts
// productlist.mount.ts
import { createApp } from 'vue';
import ProductList from './productlist.vue';

export function mountProductList() {
  document.querySelectorAll('[data-cmp-is="productlist"]').forEach((el) => {
    const dataScript = el.querySelector('[data-cmp-hook-productlist="data"]');
    const props = dataScript ? JSON.parse(dataScript.textContent || '{}') : {};

    createApp(ProductList, props).mount(el as HTMLElement);
  });
}

if (document.readyState !== 'loading') {
  mountProductList();
} else {
  document.addEventListener('DOMContentLoaded', mountProductList);
}
```

---

## Pattern 3: Component tự fetch data từ AEM API

Vue component gọi Sling Model Exporter hoặc custom servlet khi mount.

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue';

interface Product {
  id: string;
  name: string;
  price: number;
}

const products = ref<Product[]>([]);
const loading = ref(true);

onMounted(async () => {
  try {
    const res = await fetch('/content/mysite/en/products.model.json');
    products.value = (await res.json()).products;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div v-if="loading">Loading...</div>
  <ul v-else>
    <li v-for="p in products" :key="p.id">{{ p.name }} — ${{ p.price }}</li>
  </ul>
</template>
```

---

## Auto-discovery: cách `main.ts` tìm và load Vue components

```ts
// main.ts
import './main.scss';

// Tự động import tất cả file .mount.ts trong components/
// Mỗi file .mount.ts tự gọi mount khi import (DOMContentLoaded check)
const vueMounts = import.meta.glob('../components/**/*.mount.ts', { eager: true });

// Legacy JS (IIFE pattern, không phải Vue)
const componentModules = import.meta.glob('../components/**/*.js', { eager: true });

// SCSS components
const componentStyles = import.meta.glob('../components/**/_*.scss', { eager: true });
const pageStyles = import.meta.glob('./styles/*.scss', { eager: true });
```

> `{ eager: true }` = import và execute ngay (giống static import).
> Các file `.mount.ts` tự chạy logic mount khi import.

---

## Hỗ trợ AEM Authoring (MutationObserver)

Khi author kéo component mới vào page, Vue cần mount cho element mới.
Dùng composable `useDomObserver`:

```ts
// composables/useDomObserver.ts
import { onMounted, onUnmounted } from 'vue';

export function useDomObserver(
  selector: string,
  onElementAdded: (el: HTMLElement) => void
) {
  let observer: MutationObserver | null = null;

  onMounted(() => {
    observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        mutation.addedNodes.forEach((node) => {
          if (node instanceof HTMLElement) {
            if (node.matches?.(selector)) onElementAdded(node);
            node.querySelectorAll?.(selector).forEach(onElementAdded);
          }
        });
      });
    });
    observer.observe(document.body, { subtree: true, childList: true });
  });

  onUnmounted(() => observer?.disconnect());
}
```

---

## Checklist tạo Vue component mới

1. [ ] Tạo folder: `ui.frontend/src/.../components/<tên>/`
2. [ ] Tạo `<tên>.vue` — SFC với `<template>` + `<script>` + `<style scoped>`
3. [ ] Tạo `<tên>.mount.ts` — `createApp()` + `mount()` + DOMContentLoaded check
4. [ ] HTL render: `<div data-cmp-is="<tên>" data-xxx="..."><noscript>fallback</noscript></div>`
5. [ ] (Optional) Sling Model trả về JSON nếu dữ liệu phức tạp
6. [ ] `npm run prod` build test
7. [ ] Deploy + kiểm tra trên AEM page

---

## Workflow phát triển

```bash
cd ui.frontend

# Dev: Vite dev server + HMR + sync AEM
npm run watch

# Build production
npm run prod

# Deploy lên AEM Author
cd ..
mvn clean install -PautoInstallPackage -pl ui.apps -DskipTests
```

---

## Migration: từ IIFE JS → Vue component

| Cũ (IIFE) | Mới (Vue) |
|-----------|-----------|
| Manual DOM manipulation | Reactive template |
| `querySelector` + `innerHTML` | `{{ variable }}` binding |
| MutationObserver viết tay | Composable `useDomObserver` |
| Global CSS class | `<style scoped>` |
| ~40 dòng code | ~20 dòng code |
