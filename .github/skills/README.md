# Copilot Skills For AEM 6.5 On-Premise

This directory contains project-level GitHub Copilot agent skills for AEM 6.5 on-premise projects maintained by an Application Management Service (AMS) team. For repo overview and sharing instructions, see the root `../../README.md`.

## Skills

- `aem-65-onprem` — project-authored entry point: components, templates, HTL, Sling Models, OSGi, Vue, tests, security.
- `aem-permissions` — project-authored: debug author users, groups, membership, and ACLs/permissions.
- `dispatcher` — imported from Adobe.
- `aem-workflow` — imported from Adobe.
- `aem-replication` — imported from Adobe.

## Imported Adobe Skills Source

- Repository: `https://github.com/adobe/skills`
- Source commit: `1ab6833f1edfc87db31020cbe3f3b8b47f8e11fd`
- Source path: `plugins/aem/6.5-lts/skills`
- License: Apache-2.0, copied as `ADOBE-SKILLS-LICENSE.txt`

Included from Adobe:

- `dispatcher`
- `aem-workflow`
- `aem-replication`

Excluded intentionally:

- `plugins/aem/cloud-service/**`
- `plugins/aem/edge-delivery-services/**`
- `plugins/aem/project-management/**`
- `app-builder`
- `creative-cloud`
- `ensure-agents-md`

This project should not load AEM as a Cloud Service, RDE, or Edge Delivery Services skills for normal work. If migration to AEMaaCS is requested later, add a separate migration-specific skill or install the relevant upstream skill in a branch.

## Sharing

To reuse this pack in another AEM 6.5 on-premise repository, copy the whole `.github/` folder. See the root `../../README.md` for the full sharing checklist.
