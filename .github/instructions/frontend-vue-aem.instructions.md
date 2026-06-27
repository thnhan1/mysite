---
applyTo: "ui.frontend/**,ui.apps/src/main/content/jcr_root/apps/**/clientlibs/**,ui.apps/src/main/content/jcr_root/apps/**/components/**/*.html"
---

# AEM Frontend And Vue 2.7

The current frontend is Webpack/TypeScript with generated AEM clientlibs. Vue is the target integration stack, but `ui.frontend/package.json` does not yet include Vue dependencies. If adding Vue, use Vue 2.7.x and update both `package.json` and `package-lock.json` through npm.

## Integration Pattern

- Prefer Vue component islands inside AEM-rendered HTL over a full embedded SPA unless the user asks for SPA behavior.
- Mount each island on a stable root element with a data attribute, component resource type, and serialized model data.
- Make mounts idempotent so author-mode refreshes or repeated clientlib execution do not double-mount.

## Hydration

- Hydrate from Sling Model JSON, a safe `application/json` script tag, or escaped `data-*` attributes.
- Keep server-rendered values and client defaults consistent to avoid hydration mismatches.
- Avoid client-only date, locale, or URL normalization when the same value is rendered by HTL.

## Authoring Modes

- Handle AEM authoring modes explicitly.
- In edit mode, preserve placeholders, dialog behavior, overlays, and drag/drop areas.
- Do not let Vue replace authorable child markup unless the component is intentionally non-container.

## Build Flow

- Keep generated assets in the existing clientlib flow: `ui.frontend` builds into `dist`, then `aem-clientlib-generator` writes under `ui.apps/src/main/content/jcr_root/apps/<app-root>/clientlibs`.
- Do not hand-edit generated clientlib output unless the task is specifically about generated artifacts.
