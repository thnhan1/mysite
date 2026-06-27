---
name: aem-65-onprem
description: Use for AEM 6.5 on-premise development and production support in this repository, including components, editable templates, HTL, Sling Models, OSGi services, clientlibs, Vue 2.7 integration, security configs, testing, query/index review, and package structure.
license: Internal project guidance
compatibility: Requires AEM 6.5 on-premise with the uber-jar dependency and Java 11. NOT for AEM as a Cloud Service.
metadata:
  version: "1.0"
  aem_version: "6.5"
---

# AEM 6.5 On-Premise Project Skill

Use this skill for day-to-day implementation, review, and production-support work on AEM 6.5 on-premise projects when the task is about AEM components, templates, HTL, Sling Models, OSGi services, configuration, clientlibs, Vue integration, or AEM operational trade-offs. In this repository, AMS refers to the Application Management Service team maintaining the running system, not Adobe's hosted offering.

## Scope

- Runtime: AEM 6.5 on-premise, not AEM as a Cloud Service.
- Java: Java 11, OSGi DS annotations, AEM `uber-jar` dependency.
- Frontend: Webpack/TypeScript clientlib generation. Vue target is Vue 2.7.x.
- Content packages: FileVault modules `ui.apps`, `ui.config`, `ui.content`, and aggregate `all`.
- Operations: classic replication agents, Dispatcher cache/flush, direct logs/JMX/Felix Console where available.
- Project conventions: infer the app root from package filters and component paths, usually `/apps/<app-root>`.

## Routing

- Use `../dispatcher/SKILL.md` when the request is about Dispatcher filters, cache, vhosts, invalidation, security headers, runtime cache behavior, or Dispatcher incidents.
- Use `../aem-workflow/SKILL.md` when the request is about Granite Workflow models, launchers, process steps, participant choosers, workflow failures, purge, or Sling Job workflow diagnostics.
- Use `../aem-replication/SKILL.md` when the request is about replication agents, activation/deactivation, dispatcher flush agents, Replication API, or stuck replication queues.
- Use `../aem-permissions/SKILL.md` when the request is about debugging users, groups, group membership, or ACLs/permissions on author ("user cannot read/edit/activate", deny ACEs, `rep:glob` restrictions, CUG, service-user access).
- Stay in this skill for components, Sling Models, HTL, OSGi services, Vue integration, tests, package filters, and security configuration.

## Implementation Workflow

1. Identify the affected AEM layer: Java bundle, HTL/component content, OSGi config, frontend clientlib, Dispatcher, workflow, replication, or content package filter.
2. Check existing local patterns before adding new abstractions. Match naming, package paths, component paths, clientlib categories, and test style.
3. For new components, define the component node, dialog, optional policy fields, HTL, Sling Model, clientlib hook, and unit tests together.
4. For Vue islands, define the HTL mount point and Sling Model JSON contract before writing the Vue code.
5. Validate with the narrowest useful command first, then broader Maven/frontend builds when risk is higher.

## Guardrails

- Do not replace classic replication with Sling Distribution unless the task is an AEMaaCS migration.
- Do not use `aem-sdk-api` or Cloud Service-only APIs in this codebase.
- Do not write to `/libs`; overlay or configure under `/apps`, `/conf`, or OSGi config as appropriate.
- Do not use administrative sessions. Use service users and scoped permissions.
- Avoid unbounded JCR queries. Prefer indexed predicates, path constraints, and explainable query plans.
- Keep author-mode behavior intact: placeholders, overlays, dialog persistence, and container authoring must continue to work.
- Keep Dispatcher cacheability in mind for any JSON endpoint, selector, servlet, or clientlib URL.

## Review Checklist

- Resource type matches the actual component path.
- HTL escapes values with correct context.
- Sling Model getters are null-safe and stable for HTL/Vue.
- OSGi configs are runmode-scoped under `ui.config`.
- FileVault filters include only intended repository roots.
- Frontend changes preserve the clientlib generation flow.
- Tests cover changed Java behavior and critical serialization contracts.
- Operational impact is clear: cache, replication, security filters, logs, and rollback.
