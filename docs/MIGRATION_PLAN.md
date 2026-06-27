# AEM Webpack → Vite + Vue 3 Migration Guide

Hướng dẫn migrate dự án AEM Maven Archetype từ **Webpack** sang **Vite + Vue 3**.
Đã test thành công với AEM 6.5.24 on-premise.

---

## Phiên bản đã hoạt động

### Node.js (qua `frontend-maven-plugin`)

| Công cụ | Cũ (Webpack) | Mới (Vite + Vue 3) |
|---------|-------------|-------------------|
| Node.js | `v16.17.0` | **`v22.14.0`** |
| npm | `8.15.0` | **`10.9.2`** |

### npm packages

| Package | Version | Vai trò |
|---------|---------|---------|
| `vite` | `7.3.6` | Build tool |
| `vue` | `3.5.39` | Vue 3 runtime |
| `@vitejs/plugin-vue` | `6.0.7` | Vue SFC compiler cho Vite |
| `@aem-vite/vite-aem-plugin` | `5.1.1` | Proxy AEM + ClientLib integration |
| `@aem-vite/import-rewriter` | `9.1.2` | Rewrite ES import path cho AEM |
| `vite-tsconfig-paths` | `5.1.4` | Resolve TypeScript path aliases |
| `typescript` | `4.8.2` | Giữ nguyên từ webpack |
| `sass` | `1.101.0` | Dart Sass compiler (Vite native) |
| `aem-clientlib-generator` | `1.8.0` | Tạo ClientLib `.content.xml` + `js.txt`/`css.txt` |
| `aemsync` | `4.0.1` | Sync file vào AEM (dev) |
| `chokidar-cli` | `3.0.0` | Watch `dist/` |
| `npm-run-all` | `4.1.5` | Parallel dev tasks |
| `eslint` | `8.4.1` | Lint (chạy độc lập) |

---

## Bước 1: Upgrade Node.js

**File: `pom.xml` (root)**

```xml
<frontend-maven-plugin.version>1.12.0</frontend-maven-plugin.version>
...
<configuration>
  <nodeVersion>v22.14.0</nodeVersion>   <!-- ← v16.17.0 → v22.14.0 -->
  <npmVersion>10.9.2</npmVersion>       <!-- ← 8.15.0 → 10.9.2 -->
</configuration>
```

Sau khi sửa, xóa `ui.frontend/node/` và `ui.frontend/node_modules/` để plugin tải Node mới.

```bash
rm -rf ui.frontend/node ui.frontend/node_modules
```

---

## Bước 2: Cập nhật `package.json`

**File: `ui.frontend/package.json`**

```json
{
  "scripts": {
    "dev": "vite build --mode development && clientlib --verbose",
    "prod": "vite build && clientlib --verbose",
    "start": "vite serve",
    "sync": "aemsync -d -p ../ui.apps/src/main/content",
    "chokidar": "chokidar -c \"clientlib\" ./dist",
    "aemsyncro": "aemsync -w ../ui.apps/src/main/content",
    "watch": "npm-run-all --parallel start chokidar aemsyncro"
  },
  "dependencies": {
    "vue": "^3.5.39"
  },
  "devDependencies": {
    "@aem-vite/import-rewriter": "^9.1.2",
    "@aem-vite/vite-aem-plugin": "^5.1.1",
    "@typescript-eslint/eslint-plugin": "^5.7.0",
    "@typescript-eslint/parser": "^5.7.0",
    "@vitejs/plugin-vue": "^6.0.7",
    "aem-clientlib-generator": "^1.8.0",
    "aemsync": "^4.0.1",
    "browserslist": "^4.2.1",
    "chokidar-cli": "^3.0.0",
    "eslint": "^8.4.1",
    "npm-run-all": "^4.1.5",
    "sass": "^1.45.0",
    "typescript": "^4.8.2",
    "vite": "^7.0.0",
    "vite-tsconfig-paths": "^5.0.0"
  }
}
```

**Đã xóa (webpack-only):** `webpack`, `webpack-cli`, `webpack-dev-server`, `webpack-merge`, `ts-loader`, `tsconfig-paths-webpack-plugin`, `css-loader`, `style-loader`, `sass-loader`, `postcss-loader`, `mini-css-extract-plugin`, `css-minimizer-webpack-plugin`, `cssnano`, `terser-webpack-plugin`, `clean-webpack-plugin`, `copy-webpack-plugin`, `html-webpack-plugin`, `glob-import-loader`, `autoprefixer`, `postcss`, `source-map-loader`, `eslint-webpack-plugin`, `@babel/core`, `@babel/plugin-proposal-class-properties`, `@babel/plugin-proposal-object-rest-spread`

---

## Bước 3: Tạo `vite.config.ts`

**File mới: `ui.frontend/vite.config.ts`**

```ts
import { defineConfig } from 'vite';
import path from 'path';
import { fileURLToPath } from 'url';
import vue from '@vitejs/plugin-vue';
import tsconfigPaths from 'vite-tsconfig-paths';
import { viteForAem } from '@aem-vite/vite-aem-plugin';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// ============================================================
// Path constants — sửa 1 lần ở đây cho mọi reference
// ============================================================
const SRC_DIR = 'src/main/webpack';     // ← Đổi thành 'src/main/vite' sau khi rename folder
const SITE_DIR_NAME = 'site';
const CLIENTLIB_DIR = 'clientlib-site';
const ASSETS_DIR = 'assets';

const SITE_PATH = path.resolve(__dirname, SRC_DIR, SITE_DIR_NAME);
const ENTRY_TS = `${SRC_DIR}/${SITE_DIR_NAME}/main.ts`;
const VARIABLES_PATH = path.resolve(SITE_PATH, '_variables').replace(/\\/g, '/');

export default defineConfig(({ command, mode }) => ({
  base: command === 'build'
    ? '/etc.clientlibs/mysite/clientlibs/'
    : '/',

  publicDir: command === 'build'
    ? false
    : `${SRC_DIR}/${ASSETS_DIR}`,

  build: {
    reportCompressedSize: false,
    manifest: false,
    minify: mode === 'development' ? false : 'terser',
    outDir: 'dist',
    sourcemap: command === 'serve' ? 'inline' : false,
    cssMinify: mode !== 'development',
    rollupOptions: {
      input: {
        site: ENTRY_TS,
      },
      output: {
        entryFileNames: `${CLIENTLIB_DIR}/[name].[hash].js`,
        chunkFileNames: `${CLIENTLIB_DIR}/resources/chunks/[name].[hash].js`,
        assetFileNames: (assetInfo) => {
          if (assetInfo.name?.endsWith('.css')) {
            return `${CLIENTLIB_DIR}/[name].[hash][extname]`;
          }
          return `${CLIENTLIB_DIR}/resources/[name].[hash][extname]`;
        },
      },
    },
  },

  css: {
    preprocessorOptions: {
      scss: {
        includePaths: [SITE_PATH],
        additionalData: `@import '${VARIABLES_PATH}';\n`,
        silenceDeprecations: ['global-builtin', 'import', 'color-functions'],
      },
    },
  },

  plugins: [
    vue(),
    tsconfigPaths(),
    viteForAem({
      contentPaths: ['mysite/en'],
      publicPath: '/etc.clientlibs/mysite/clientlibs/clientlib-site',
      aem: { host: 'localhost', port: 4502 },
    }),
  ],

  server: {
    origin: 'http://localhost:3000',
    port: 3000,
    strictPort: false,
  },
}));
```

---

## Bước 4: Cập nhật entry point `main.ts`

**File: `ui.frontend/src/main/webpack/site/main.ts`**

```ts
import './main.scss';

// Vue component mount scripts — mỗi file tự mount Vue app vào DOM
const vueMounts = import.meta.glob('../components/**/*.mount.ts', { eager: true });

// Legacy JS components (IIFE pattern, không phải Vue)
const componentModules = import.meta.glob('../components/**/*.js', { eager: true });

// SCSS component + page styles
const componentStyles = import.meta.glob('../components/**/_*.scss', { eager: true });
const pageStyles = import.meta.glob('./styles/*.scss', { eager: true });
```

**File: `ui.frontend/src/main/webpack/site/main.scss`**

```scss
@import 'variables';
@import 'base';
// Component và style SCSS được load qua import.meta.glob trong main.ts
```

---

## Bước 5: Cập nhật `clientlib.config.js`

**File: `ui.frontend/clientlib.config.js`**

```js
const libsBaseConfig = {
  allowProxy: true,
  serializationFormat: 'xml',
  cssProcessor: ['default:none', 'min:none'],
  jsProcessor: ['default:none', 'min:none'],
};

module.exports = {
  context: BUILD_DIR,
  clientLibRoot: CLIENTLIB_DIR,
  libs: [
    {
      ...libsBaseConfig,
      name: 'clientlib-dependencies',
      categories: ['mysite.dependencies'],
      assets: {
        js: { cwd: 'clientlib-dependencies', files: ['**/*.js'], flatten: false },
        css: { cwd: 'clientlib-dependencies', files: ['**/*.css'], flatten: false },
      },
    },
    {
      ...libsBaseConfig,
      name: 'clientlib-site',
      categories: ['mysite.site'],
      dependencies: ['mysite.dependencies'],
      assets: {
        js: { cwd: 'clientlib-site', files: ['**/*.js'], flatten: false },
        css: { cwd: 'clientlib-site', files: ['**/*.css'], flatten: false },
        resources: {
          cwd: 'clientlib-site',
          files: ['**/*.*'],
          flatten: false,
          ignore: ['**/*.js', '**/*.css'],
        },
      },
    },
  ],
};
```

> Không cần `esModule` hay `customProperties` — template chuẩn AEM không cần.

---

## Bước 6: Cập nhật HTL Templates

Tất cả đều dùng template **có sẵn trong AEM**:
`/libs/granite/sightly/templates/clientlib.html`

**File: `ui.apps/.../components/page/customheaderlibs.html`**

```html
<sly data-sly-use.clientlib="/libs/granite/sightly/templates/clientlib.html">
    <sly data-sly-call="${clientlib.css @ categories='mysite.base'}"/>
    <sly data-sly-call="${clientlib.css @ categories='mysite.site'}"/>
</sly>
...
```

**File: `ui.apps/.../components/page/customfooterlibs.html`**

```html
<sly data-sly-use.clientlib="/libs/granite/sightly/templates/clientlib.html">
    <sly data-sly-call="${clientlib.js @ categories='mysite.base', async=true}"/>
    <sly data-sly-call="${clientlib.js @ categories='mysite.site'}"/>
</sly>
```

> **Không dùng `esModule=true`** — template chuẩn `/libs/...` không hỗ trợ tham số này.
> `<script>` tag thường vẫn chạy Vue bình thường.

**File: `ui.content/.../conf/mysite/settings/wcm/policies/.content.xml`**

Gỡ `mysite.site` khỏi page policy (đã load trong customheaderlibs/customfooterlibs):

```xml
<!-- Trước: -->
clientlibs="[mysite.dependencies,mysite.site]"
<!-- Sau: -->
clientlibs="[mysite.dependencies]"
```

---

## Bước 7: Cập nhật ClientLib `.content.xml` (ui.apps)

**File: `ui.apps/.../clientlibs/clientlib-site/.content.xml`**

```xml
<jcr:root ...
    categories="[mysite.site]"
    dependencies="[mysite.dependencies]"
    cssProcessor="[default:none,min:none]"
    jsProcessor="[default:none,min:none]"
    allowProxy="{Boolean}true"/>
```

> Không cần `esModule` nếu dùng template chuẩn `/libs/...`.

---

## Bước 8: Dọn dẹp

Xóa các file webpack cũ:

```
ui.frontend/webpack.common.js       ← XÓA
ui.frontend/webpack.dev.js          ← XÓA
ui.frontend/webpack.prod.js         ← XÓA
ui.frontend/.babelrc                ← XÓA
ui.frontend/assembly.xml            ← XÓA
ui.frontend/src/main/webpack/static/index.html  ← XÓA
```

Xóa `maven-assembly-plugin` trong `ui.frontend/pom.xml` (đã xóa `assembly.xml`).

---

## Bước 9: Cài đặt + Build

```bash
cd ui.frontend
npm install
npm run prod
```

Expected output:
```
dist/clientlib-site/site.[hash].js    # JS bundle (~60 KB với Vue runtime)
dist/clientlib-site/site.[hash].css   # CSS bundle (~7 KB)
```

---

## Bước 10: Deploy lên AEM

```bash
mvn clean install -PautoInstallPackage -pl ui.apps -DskipTests
```

Sau đó mở page, check:
- [ ] Console không có lỗi 404
- [ ] `<script src="...site.[hash].js">` tồn tại trong HTML
- [ ] Vue component render đúng
- [ ] `npm run watch` → HMR hoạt động

---

## Cấu trúc thư mục sau migrate

```
ui.frontend/src/main/webpack/        ← Đổi thành src/main/vite/ sau khi rename
├── site/
│   ├── main.ts                      ← Entry point
│   ├── main.scss
│   ├── _variables.scss
│   ├── _base.scss
│   └── styles/
├── components/
│   ├── helloworld/                  ← Vue component (mỗi component 1 folder)
│   │   ├── helloworld.vue
│   │   └── helloworld.mount.ts
│   ├── _accordion.scss              ← SCSS-only component
│   └── ...
├── assets/                          ← fonts, images (đã rename từ resources/)
├── composables/                     ← Shared Vue composables
└── vue-shims.d.ts                   ← TypeScript type declaration cho .vue
```

---

## Đổi tên thư mục `webpack` → `vite`

Chỉ cần sửa **4 references** trong toàn bộ project:

| # | File | Sửa |
|---|------|-----|
| 1 | `vite.config.ts` | `SRC_DIR = 'src/main/vite'` |
| 2 | `vite.config.ts` | `ASSETS_DIR = 'assets'` (nếu đã rename resources) |
| 3 | `vite.config.ts` | `ENTRY_TS` tự động cập nhật từ `SRC_DIR` |
| 4 | `package.json` | `"main": "src/main/vite/site/main.ts"` |

Sau đó rename folder:

```bash
cd ui.frontend
mv src/main/webpack src/main/vite
mv src/main/vite/resources src/main/vite/assets
```

---

## Các vấn đề đã gặp & cách fix

| # | Lỗi | Fix |
|---|-----|-----|
| 1 | `ERR_REQUIRE_ESM` — Node 16 quá cũ | Upgrade lên `v22.14.0` |
| 2 | SCSS `Undefined variable` | `additionalData` inject `_variables.scss` absolute path |
| 3 | `Can't find stylesheet to import` | Dùng `includePaths` + forward-slash path |
| 4 | Generator nest sai `js/resources/js/` | Output flat từ Vite: `[name].[hash].js` |
| 5 | Template `/apps/aem-vite/...` không tồn tại | Dùng `/libs/granite/sightly/templates/clientlib.html` (có sẵn) |
| 6 | Dart Sass deprecation | `silenceDeprecations: ['global-builtin', 'import', 'color-functions']` |
| 7 | `esModule=true` gây lỗi với template chuẩn | Bỏ `esModule` — template chuẩn không hỗ trợ |
