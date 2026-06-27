---
applyTo: "core/**/*.java,core/src/test/**/*.java,ui.apps/src/main/content/jcr_root/apps/**/*.html,ui.apps/src/main/content/jcr_root/apps/**/*.xml,ui.config/src/main/content/**/*.json,**/pom.xml"
---

# AEM 6.5 Java, HTL, And Config

Target AEM 6.5 on-premise, service pack AEM 6.5.24. Do not suggest AEMaaCS-only APIs, `aem-sdk-api`, RDE, or immutable Cloud Service deployment patterns unless migration is explicitly requested.

## Sling Models

- Keep `resourceType` aligned with the component path under `/apps/<app-root>`.
- Infer `<app-root>` from `ui.apps/src/main/content/META-INF/vault/filter.xml` and existing component paths.
- Adapt from `SlingHttpServletRequest` when request context, exporter behavior, selectors, or `wcmmode` matters; adapt from `Resource` only for resource-only models.

## JSON / Exporter Output

- Implement `ComponentExporter` when the component is consumed by frontend code.
- Keep output stable, null-safe, and cacheable.
- Return empty strings or empty collections instead of exposing nullable internals to HTL or Vue.

## HTL

- Keep markup semantic and author-friendly.
- Use explicit context options for URLs, attributes, HTML, JS, and JSON.
- Keep empty-state placeholders behind `wcmmode.edit`.
- Include clientlibs through existing clientlib templates and categories, never hardcoded script paths.

## OSGi

- Prefer DS annotations, typed `@ObjectClassDefinition` configs, service users, and small focused services.
- Avoid `loginAdministrative`, broad JCR writes, long synchronous servlet operations, and unbounded queries.

## Tests

- Use JUnit 5 and the AEM Mocks patterns already present under `core/src/test`.
- Add focused tests when changing Sling Models, servlets, services, filters, or workflow/replication code.
