---
applyTo: "dispatcher/**"
---

# Dispatcher Instructions

This is an on-premise Dispatcher module. Use AEM 6.5 Dispatcher behavior, Apache HTTPD config, classic replication flush agents, and cache invalidation concepts. Do not use AEMaaCS Dispatcher SDK or Cloud Manager-only assumptions unless the user asks for that explicitly.

## Checksum-Protected Files

- `dispatcher/pom.xml` enforces checksums on many immutable/baseline files.
- Before editing a Dispatcher file, check whether it is checksum-protected.
- Do not change protected files or update checksums unless the task explicitly requires that baseline-governed change and the risk is called out.

## Filters

- Prefer least-privilege filters: allow only the method, extension, path, selectors, suffix, and client headers required.
- Keep deny-by-default behavior intact.
- Avoid broad `/url` allow rules when `/path`, `/extension`, `/selectors`, or `/suffix` can express the rule.

## Cache

- Reason about `/statfileslevel`, `/invalidate`, `/rules`, `/ignoreUrlParams`, TTL headers, and authenticated content.
- Confirm whether activation/dispatcher flush will actually invalidate the path.
- Keep author and publish farms separate.

## Validation

- Run `mvn clean package` from `dispatcher/` when Dispatcher config changes.
- If Adobe Dispatcher MCP tools are unavailable, state that limitation and provide static validation evidence from the repository.
