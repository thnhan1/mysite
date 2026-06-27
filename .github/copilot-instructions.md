# AEM 6.5 On-Premise Copilot Instructions

This repository is an AEM 6.5 on-premise **multi-project monorepo**. It contains several independent AEM project-archetype builds (each its own set of `core`, `ui.apps`, `ui.config`, `ui.frontend`, `ui.content`, `dispatcher`, and `all` modules), not a single one-level project. Treat each sub-project as a traditional AEM 6.5 codebase that uses `com.adobe.aem:uber-jar`, Java 11 unless the POM says otherwise, Maven packages, FileVault content packages, editable templates, HTL, Sling Models, OSGi DS, and Dispatcher configuration.

**AMS in this repository means Application Management Service** — the team that maintains and supports the running on-premise AEM system. Day-to-day work is heavily production-support oriented: reading source code, reproducing and debugging production defects, inspecting Sling/`error.log`/request logs, reviewing ACLs and permissions, and shipping safe fixes. AMS does **not** mean Adobe's hosted "Adobe Managed Services" offering, and this is not an Adobe-hosted environment. When unsure, favor read-only diagnosis (logs, JMX, repository inspection) before changing a running system.

Before acting, identify which sub-project a task belongs to. Each archetype project typically has its own Maven `groupId`/`artifactId`, its own `/apps/<app-root>` (and `/conf/<app-root>`), its own clientlib categories, and its own Dispatcher/runmode config. Do not assume a change in one sub-project applies to the others, and do not mix resource types, packages, or clientlib categories across sub-projects.

Do not default to AEM as a Cloud Service, Edge Delivery Services, RDE, `aem-sdk-api`, Cloud Service-only deployment rules, or Sling Distribution replacements unless the user explicitly asks for migration or AEMaaCS guidance. This project uses classic AEM replication agents and on-premise operational patterns.

Use the project skills in `.github/skills` when relevant:

- `.github/skills/aem-65-onprem` for AEM components, templates, Sling Models, OSGi, HTL, Vue integration, security, testing, and on-premise operational trade-offs.
- `.github/skills/dispatcher` for Dispatcher config, filters, cache, invalidation, security, and runtime triage.
- `.github/skills/aem-workflow` for Granite Workflow models, launchers, process steps, JMX/debugging, and Sling Job diagnostics.
- `.github/skills/aem-replication` for replication agents, activation/deactivation, Replication API usage, dispatcher flush, and queue troubleshooting.
- `.github/skills/aem-permissions` for debugging author users, groups, membership, and ACLs/permissions: effective-permission diagnosis, deny ACEs, `rep:glob` restrictions, CUG, and service-user access.

If an imported Adobe skill mentions Dispatcher MCP tools that are not available in the current Copilot host, do not invent tool results. Use repository inspection and local commands where possible, and clearly state which MCP validation was unavailable.

## Repository Layout

This is a multi-project monorepo. There is usually a root aggregator `pom.xml`, and one folder per AEM archetype project. Inspect the root POM `<modules>` and each sub-project POM before building, since module names, app roots, and reactor membership differ per sub-project.

- Root `pom.xml`: top-level aggregator/parent. Lists the sub-projects and/or shared dependency management. Read it first to discover the active sub-projects.
- `<project>/`: one AEM archetype project. Each contains its own module set below and its own `<app-root>`.

Within each AEM archetype sub-project, the common modules are:

- `core`: Java bundle for Sling Models, OSGi services, servlets, filters, listeners, schedulers, and JUnit/AEM Mocks tests.
- `ui.apps`: `/apps/<app-root>` content package for components, dialogs, templates, policies, clientlibs, and HTL. Infer `<app-root>` from that sub-project's FileVault filters and existing paths.
- `ui.apps.structure`: repository structure package defining allowed roots.
- `ui.config`: runmode OSGi configs (commonly under `/apps/<app-root>/osgiconfig`) when the sub-project follows the AEM archetype pattern.
- `ui.content`: authored content and sample/design content for that sub-project.
- `ui.frontend`: Webpack/TypeScript frontend build that generates AEM clientlibs via `aem-clientlib-generator`. Each sub-project may have its own frontend build and lockfile.
- `dispatcher`: Dispatcher and Apache HTTPD config package. A sub-project may own its own dispatcher module, or dispatcher config may be shared at the repo level — check the POMs. Confirm whether it is part of the active Maven reactor.
- `all`: aggregate package embedding that sub-project's built artifacts.
- `it.tests` / `ui.tests`: integration and UI tests when present.

App roots, package names, and clientlib categories are per sub-project. Always resolve them from the specific sub-project you are editing, never from a sibling.

## Build And Validation

Scope builds to the sub-project you are working on. Run commands from that sub-project's directory (or target it with `mvn -f <project>/pom.xml` / `-pl`) rather than assuming a single root reactor. The root aggregator build only works if every sub-project is wired into it.

- Build one sub-project's full reactor: `mvn clean install` from that `<project>/` directory.
- Build everything (only if the root POM aggregates all sub-projects): `mvn clean install` from the repo root.
- Deploy a sub-project's all package to local author: `mvn clean install -PautoInstallSinglePackage` from that `<project>/`.
- Deploy to local publish: `mvn clean install -PautoInstallSinglePackagePublish`.
- Deploy only that sub-project's bundle to author: `mvn clean install -PautoInstallBundle`.
- Core tests for a sub-project: `mvn -pl core test` from that `<project>/` (or `mvn -pl <project>/core test` from root).
- Frontend build: from that sub-project's `ui.frontend`, run `npm ci` then `npm run prod` (`npm run dev` for dev).
- Dispatcher package build: run `mvn clean package` from the relevant `dispatcher/` module if it exists and is not part of the active reactor.

Each sub-project may pin different Node/npm and dependency versions. Prefer that sub-project's lockfile and the Node/npm versions configured in its frontend Maven plugin. Do not upgrade Node, npm, Webpack, TypeScript, Vue, or Core Components unless the task requires it, and do not assume versions are shared across sub-projects.

### Build & Deploy Shortcuts

The team uses these fast build/deploy shortcuts, defined as PowerShell functions in `scripts/aem-build.ps1` (dot-source it: `. .\scripts\aem-build.ps1`). They skip tests and Javadoc for speed and deploy to the **local author** on the default port. The `-pl` shortcuts require running from a sub-project directory whose reactor contains those modules; in this monorepo, `cd` into the relevant `<project>/` first. When asked to run one of these, prefer running the underlying `mvn` command directly rather than relying on the shell function being loaded.

| Shortcut | Purpose | Command |
|---|---|---|
| `mcore` | Build + deploy only the `core` OSGi bundle to author | `mvn -T 1C clean install -Dmaven.test.skip -DskipTests -Dmaven.javadoc.skip=true -PautoInstallBundle -pl core` |
| `mapps` | Build + deploy the `ui.apps` package to author | `mvn -T 1C clean install -Dmaven.test.skip -DskipTests -Dmaven.javadoc.skip=true -PautoInstallPackage -pl ui.apps` |
| `mcontent` | Build + deploy the `ui.content` package to author | `mvn -T 1C clean install -Dmaven.test.skip -DskipTests -Dmaven.javadoc.skip=true -PautoInstallPackage -pl ui.content` |
| `mconfig` | Build + deploy the `ui.config` package to author | `mvn -T 1C clean install -Dmaven.test.skip -DskipTests -Dmaven.javadoc.skip=true -PautoInstallPackage -pl ui.config` |
| `mall` | Build + deploy the aggregate single package to author | `mvn -T 1C clean install -PautoInstallSinglePackage -Dmaven.test.skip=true -Dmaven.javadoc.skip=true` |
| `mapp` | Build frontend (prod) then deploy `ui.apps` | `cd ui.frontend; npm run prod; cd ..` then run `mapps` |

These are local-author convenience builds: they skip tests, so run the proper test build (`mvn clean install` / `mvn -pl core test`) before committing or releasing. They target a single sub-project; pick the sub-project that owns the path you changed.

## AEM Engineering Rules

- Keep resource types exact and scoped to the correct sub-project. If a component lives under `/apps/<app-root>/components/content/foo`, the Sling Model `resourceType` must match that sub-project's component path unless the component uses inheritance intentionally. Never point a resource type at another sub-project's app root.
- Prefer Sling Models for server data shaping and HTL for markup. Avoid business logic in HTL.
- Use OSGi DS annotations for new services and use service users with `ResourceResolverFactory.getServiceResourceResolver()`. Do not use administrative sessions.
- Keep content-package filters scoped. Code/config belongs in `ui.apps` or `ui.config`; authored content belongs in `ui.content`.
- For publish-safe behavior, account for Dispatcher caching, replication/flush, CSRF, CORS, Referrer Filter, permissions, and runmodes.
- For Vue work, prefer component islands mounted inside AEM-rendered HTL. Use Sling Model JSON or safe inline JSON as the hydration source and handle author/edit mode explicitly.
