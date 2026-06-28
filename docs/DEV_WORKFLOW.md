# Local Dev Workflow — Build & Deploy tối ưu

Hướng dẫn build + deploy nhanh khi dev local, tránh chạy `npm ci` + `npm run prod`
mỗi lần `mvn clean install` (tiết kiệm 20-30s mỗi build).

---

## Nguyên lý

`frontend-maven-plugin` mặc định chạy 2 step mỗi lần Maven build:

| Step | Thời gian | Khi nào cần |
|------|-----------|------------|
| `npm ci` | ~15s | Chỉ khi `node_modules` thay đổi (thêm/xóa package) |
| `npm run prod` | ~3s | Chỉ khi sửa frontend (JS/SCSS/Vue) |

→ **90% thời gian dev là sửa Java hoặc HTL**, không cần build lại frontend.

**Giải pháp:** Property `<skip.frontend>` — mặc định `false`, set `true` để bỏ qua
toàn bộ frontend build.

---

## Các lệnh shortcut theo loại thay đổi

### 1. Sửa Java (Core bundle, Sling Model, Servlet)

```bash
# Build + deploy core bundle lên Author (nhanh nhất)
mvn -T 1C clean install -Dskip.frontend=true -PautoInstallBundle -pl core
```
~5-8s

### 2. Sửa HTL / Dialog / Component XML / ClientLib

```bash
# Build + deploy ui.apps package lên Author
mvn -T 1C clean install -Dskip.frontend=true -PautoInstallPackage -pl ui.apps
```
~5-10s

### 3. Sửa Frontend (JS / SCSS / Vue) — KHÔNG cần Maven

```bash
# Cách A: Dev server + HMR (nhanh nhất, thấy kết quả ngay)
cd ui.frontend && npm run start

# Cách B: Build + đồng bộ vào ui.apps
cd ui.frontend && npm run dev

# Cách C: Full watch mode
cd ui.frontend && npm run watch
```
~1-2s cho HMR, không cần deploy

### 4. Sửa cả Java + HTL + Frontend (full build)

```bash
# Full build + deploy lên Author
mvn clean install -PautoInstallSinglePackage -DskipTests

# Hoặc nếu không có autoInstallSinglePackage ở root:
mvn -T 1C clean install -DskipTests
mvn -T 1C clean install -PautoInstallPackage -pl ui.apps -DskipTests
```
~30-60s

### 5. Deploy lên Publish

```bash
# Sau khi test OK trên Author
mvn -T 1C clean install -Dskip.frontend=true -PautoInstallPackagePublish -pl ui.apps
```

### 6. Build tất cả nhưng skip test

```bash
mvn -T 1C clean install -DskipTests -Dmaven.javadoc.skip=true
```

---

## Bảng cheat sheet

| Tôi đang sửa... | Lệnh | Time |
|-----------------|------|------|
| Java class | `mvn -T 1C clean install -Dskip.frontend=true -PautoInstallBundle -pl core` | ~5s |
| HTL / dialog | `mvn -T 1C clean install -Dskip.frontend=true -PautoInstallPackage -pl ui.apps` | ~5s |
| SCSS / Vue / JS | `npm run start` (HMR, không cần Maven) | < 1s |
| Full project | `mvn clean install -PautoInstallSinglePackage -DskipTests` | ~30s |

---

## `npm run` scripts trong `ui.frontend/`

| Lệnh | Làm gì | Dùng khi |
|------|--------|---------|
| `npm run start` | Vite dev server (port 3000) | Dev frontend với HMR |
| `npm run dev` | Build dev + copy vào `ui.apps/clientlibs/` | Test frontend trên AEM (không HMR) |
| `npm run prod` | Build production + copy vào `ui.apps/clientlibs/` | Trước khi commit/deploy |
| `npm run watch` | `start` + `chokidar` + `aemsync` đồng thời | Dev toàn diện |

## Maven profiles trong root `pom.xml`

| Profile | Tác dụng |
|---------|---------|
| `autoInstallBundle` | Deploy `core` bundle lên Author (port 4502) |
| `autoInstallPackage` | Deploy content package lên Author |
| `autoInstallPackagePublish` | Deploy content package lên Publish (port 4503) |

---

## Config đã thêm

### `pom.xml` (root) — thêm 2 chỗ

```xml
<properties>
    <skip.frontend>false</skip.frontend>   <!-- ← thêm property -->
</properties>

<execution>
    <id>npm ci</id>
    ...
    <configuration>
        <skip>${skip.frontend}</skip>       <!-- ← thêm skip -->
        <arguments>ci</arguments>
    </configuration>
</execution>
```

### `ui.frontend/pom.xml` — thêm 1 chỗ

```xml
<execution>
    <id>npm run prod</id>
    ...
    <configuration>
        <skip>${skip.frontend}</skip>       <!-- ← thêm skip -->
        <arguments>run prod</arguments>
    </configuration>
</execution>
```

---

## Khi nào cần chạy full frontend build

Chỉ cần chạy `npm run prod` (hoặc Maven không có `-Dskip.frontend=true`) khi:

- Sửa file trong `ui.frontend/src/` (JS, TS, SCSS, Vue)
- Thêm/xóa npm package
- Commit code (để CI build đúng)

KHÔNG cần khi:

- Sửa Java, HTL, XML, dialog, policy
- Sửa `ui.content` hoặc `ui.config`
- Chạy test

---

## Workflow hàng ngày điển hình

```bash
# Sáng: start môi trường
cd ui.frontend && npm run start &    # Vite dev server chạy nền

# Sửa Java:
mvn -T 1C clean install -Dskip.frontend=true -PautoInstallBundle -pl core

# Sửa HTL/dialog:
mvn -T 1C clean install -Dskip.frontend=true -PautoInstallPackage -pl ui.apps

# Sửa SCSS/Vue: chỉ cần save file → HMR cập nhật ngay

# Cuối ngày: full build
mvn clean install -PautoInstallSinglePackage -DskipTests
```
