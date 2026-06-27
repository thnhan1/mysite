---
name: create-basic-page-template
description: Creates a new editable page template with a custom page component, including additional page properties (e.g., sub title, sub description, name text field). This skill handles the full workflow: custom page component definition, page component dialog for extra properties, template node, structure, initial content, and policy mappings.
license: Internal project guidance
compatibility: AEM 6.5 on-premise, AEM uber-jar, Java 11. NOT for AEM as a Cloud Service.
metadata:
  version: "1.0"
  aem_version: "6.5"
---

# Create Basic Page Template Skill

Use this skill when asked to **create a new page template** in this AEM 6.5 on-premise multi-project monorepo. It covers creating a custom page component (with extended dialog for extra properties), the editable template under `/conf/<app-root>/settings/wcm/templates/`, and the associated structure, initial content, and policy files.

## Prerequisites

- The app root (e.g., `/apps/mysite`) must already have a base page component (e.g., `mysite/components/page` extending `core/wcm/components/page/v3/page`).
- The `/conf/<app-root>/settings/wcm/templates/` directory must exist in `ui.content` (via the content package filter).
- The `/conf/<app-root>/settings/wcm/template-types/page` template type must already exist.
- The project must have a pre-existing template (e.g., `page-content`) to copy structure/policy patterns from.

## Workflow

1. **Identify the sub-project and app root.**  
   Read the root `pom.xml` and the `ui.apps` `filter.xml` to find the active sub-project and its app root.

2. **Create a custom page component** in `ui.apps`.  
   The new component extends the existing page component so it inherits HTL scripts (`customheaderlibs.html`, `customfooterlibs.html`) and any base behavior.

   ```
   ui.apps/src/main/content/jcr_root/apps/<app-root>/components/<custom-page>/
   ├── .content.xml          # Component definition
   └── _cq_dialog/            # Escaped name for cq:dialog
       └── .content.xml       # Dialog with extra page properties tab
   ```

   **Component `.content.xml`** — set `sling:resourceSuperType` to the existing page component:
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
             xmlns:cq="http://www.day.com/jcr/cq/1.0"
             xmlns:jcr="http://www.jcp.org/jcr/1.0"
       jcr:primaryType="cq:Component"
       jcr:title="<Custom Page Display Name>"
       sling:resourceSuperType="<app-root>/components/page"
       componentGroup=".hidden"/>
   ```

   **Dialog `_cq_dialog/.content.xml`** — use `sling:resourceSuperType` on the dialog node to inherit the Core Components page dialog tabs, then define a new custom tab alongside inherited tabs. The tab MUST follow the Core Components nesting (`fixedcolumns` > `column` (container) > `items` > fields), and every field that should appear in the **Create Page wizard** MUST have `cq:showOnCreate="{Boolean}true"` (see Guardrails):
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
       <content jcr:primaryType="nt:unstructured"
                sling:resourceType="granite/ui/components/coral/foundation/container">
           <items jcr:primaryType="nt:unstructured">
               <tabs jcr:primaryType="nt:unstructured"
                     sling:resourceType="granite/ui/components/coral/foundation/tabs">
                   <items jcr:primaryType="nt:unstructured">
                       <custom jcr:primaryType="nt:unstructured"
                               jcr:title="Custom Properties"
                               sling:resourceType="granite/ui/components/coral/foundation/fixedcolumns">
                           <items jcr:primaryType="nt:unstructured">
                               <column jcr:primaryType="nt:unstructured"
                                       sling:resourceType="granite/ui/components/coral/foundation/container">
                                   <items jcr:primaryType="nt:unstructured">
                                       <subTitle jcr:primaryType="nt:unstructured"
                                                 sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                                                 cq:showOnCreate="{Boolean}true"
                                                 fieldLabel="Sub Title"
                                                 name="./subTitle"
                                                 emptyText="Enter sub title"/>
                                       <!-- Add more fields here, each with cq:showOnCreate if it should show in the create wizard -->
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

   Common form field types:
   - Text field: `granite/ui/components/coral/foundation/form/textfield`
   - Textarea: `granite/ui/components/coral/foundation/form/textarea`
   - Path field: `granite/ui/components/coral/foundation/form/pathbrowser`
   - Checkbox: `granite/ui/components/coral/foundation/form/checkbox`
   - Select: `granite/ui/components/coral/foundation/form/select`

   Each field stores its value under the page's `jcr:content` node via `name="./<propertyName>"`.

3. **Create the editable template** in `ui.content`.

   ```
   ui.content/src/main/content/jcr_root/conf/<app-root>/settings/wcm/templates/<template-name>/
   ├── .content.xml          # Template metadata
   ├── structure/             # Locked structure (layout, fixed components)
   │   └── .content.xml
   ├── initial/               # Initial content for newly created pages
   │   └── .content.xml
   ├── policies/              # Policy mapping for components
   │   └── .content.xml
   └── thumbnail.png          # Optional: template thumbnail
   ```

   **Template `.content.xml`** — reference the template type and enable the template:
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0"
             xmlns:jcr="http://www.jcp.org/jcr/1.0"
       jcr:primaryType="cq:Template">
       <jcr:content
           cq:templateType="/conf/<app-root>/settings/wcm/template-types/page"
           jcr:description="<Description>"
           jcr:primaryType="cq:PageContent"
           jcr:title="<Template Title>"
           status="enabled"/>
   </jcr:root>
   ```

   **Structure `.content.xml`** — use the custom page component's `sling:resourceType`:
   ```xml
   <jcr:content
       sling:resourceType="<app-root>/components/<custom-page>"
       ...>
   ```

   **Initial `.content.xml`** — MUST use the same `sling:resourceType="<app-root>/components/<custom-page>"` on its `jcr:content`, with the default editable containers. A new page copies its `jcr:content` (incl. `sling:resourceType`) from `initial`, and the page-properties dialog is resolved from that resource type — so if `initial` points at the base page component, the custom tab will NOT appear.

   **Policies `.content.xml`** — copy and adapt from an existing template (e.g., `page-content/policies/.content.xml`). Key policies to keep:
   - Page policy: `<app-root>/components/page/policy` (clientlib includes)
   - Root container policy: `<app-root>/components/container/policy_<id>` (allowed components)
   - Content container policy: `<app-root>/components/container/policy_<id>` (allowed content components)

4. **Verify the content package filters** cover the new paths:
   - `ui.apps` filter must include `/apps/<app-root>/components` (or the specific component path).
   - `ui.content` filter must include `/conf/<app-root>` with `mode="merge"`.

5. **Build and deploy** with `autoInstallSinglePackage` (installs the `all` package, which embeds both `ui.apps` and `ui.content`). To skip the frontend/npm build (safe when only `ui.apps`/`ui.content` changed) on Windows PowerShell, quote each `-D` so pwsh passes it as one token:
   ```
   mvn clean install -PautoInstallSinglePackage "-Dskip.installnodenpm=true" "-Dskip.npm=true"
   ```
   The `frontend-maven-plugin` (eirslett) honors `skip.installnodenpm` / `skip.npm`.

## Guardrails

- Use **FileVault underscore escaping** for JCR names with colons: `cq:dialog` → `_cq_dialog/`.
- Always set `sling:resourceSuperType` on the custom page component to the project's base page component (not directly to Core Components) so HTL clientlib includes are inherited.
- The dialog `sling:resourceSuperType` should point to the **Core Components page dialog** (`core/wcm/components/page/v3/page/cq:dialog`) to inherit all standard tabs.
- New tabs in the dialog are merged alongside inherited tabs — you only need to define the additional tab, but the path/node names MUST match the Core Components dialog exactly: `content` (container) > `items` > `tabs` (tabs) > `items` > `<yourTab>`.
- **`cq:showOnCreate="{Boolean}true"` is REQUIRED** on each field that must appear in the **Create Page wizard**. Without it the custom tab still renders during creation but shows **no fields** (they only appear in Page Properties of an already-created page). This was the #1 source of "tab is empty" confusion.
- The page-properties dialog is resolved from the **page component's `cq:dialog`** (via the page's `jcr:content/sling:resourceType`), NOT from the template's `structure/jcr:content/cq:dialog`. Always put the dialog on the custom page component.
- **FileVault trap:** never leave empty mirror folders (e.g., `_cq_dialog/content/items/tabs/items/custom/`) next to the inline `_cq_dialog/.content.xml`. Those empty, `.content.xml`-less folders import as empty nodes and clobber the inline definition. Keep exactly one file: `_cq_dialog/.content.xml`.
- Use the Core Components tab nesting `fixedcolumns` > `column` (container) > `items` > fields. A bare `container` for the tab body will not render fields reliably.
- Set `sling:resourceType="cq/gui/components/authoring/dialog"` on the dialog root node.
- Do **not** copy `customheaderlibs.html` or `customfooterlibs.html` into the custom component; they are inherited via `sling:resourceSuperType`.
- Prefer reusing existing container and component policies from the reference template rather than creating new policy IDs.
- Keep the template `status` set to `"enabled"` so it appears in the page creation wizard.
- Template structure defines the **locked layout**; initial content defines the **default editable content** for new pages.

## Review Checklist

- Custom page component exists under the correct app root.
- Dialog `sling:resourceSuperType` correctly points to Core Components page dialog.
- Custom tab dialog fields use `name="./<property>"` format.
- Each create-wizard field has `cq:showOnCreate="{Boolean}true"`.
- Custom tab uses `fixedcolumns` > `column` > `items` > fields nesting.
- `_cq_dialog` contains ONLY `.content.xml` (no stray empty subfolders).
- Template `cq:templateType` points to the correct template type.
- Template structure AND initial `sling:resourceType` both point to the custom page component.
- Policy mappings reference existing policies from the reference template.
- Content package filters cover the new files.
- Build succeeds without errors.
