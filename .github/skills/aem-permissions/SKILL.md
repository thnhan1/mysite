---
name: aem-permissions
description: Use to debug AEM 6.5 author users, groups, and ACL/permission problems, including "user cannot read/edit/activate", group membership and inheritance, deny-overrides-allow evaluation, rep:policy ACEs and restrictions (rep:glob), Closed User Groups, service/system users, repoinit, and effective-permission diagnosis with the Security console, CRXDE, JMX, and JCR APIs.
license: Internal project guidance
compatibility: Requires AEM 6.5 on-premise (Jackrabbit Oak). NOT for AEM as a Cloud Service. Many actions need an admin or a user with jcr:readAccessControl on the inspected paths.
metadata:
  version: "1.0"
  aem_version: "6.5"
---

# AEM 6.5 Users, Groups, And ACL Debugging

Use this skill for production-support and review work when an AEM 6.5 author user "cannot do something" and the cause is users, groups, membership, or access control (ACLs/permissions). It targets read-only diagnosis first, then the smallest safe correction.

## Scope

- Runtime: AEM 6.5 on-premise, Jackrabbit Oak authorization. Not AEM as a Cloud Service.
- Authorizables: users (`rep:User`) under `/home/users`, groups (`rep:Group`) under `/home/groups`, system users under `/home/users/system`.
- Access control: resource/path-based ACLs stored as `rep:ACL` policy nodes (`rep:policy`) with `rep:GrantACE` / `rep:DenyACE` children; principal-based ACLs where used.
- Privileges and restrictions: `jcr:read`, `rep:write`, `jcr:all`, `crx:replicate`, `jcr:readAccessControl`/`jcr:modifyAccessControl`, plus restrictions such as `rep:glob`, `rep:ntNames`, `rep:itemNames`, `rep:prefixes`.
- Closed User Groups (`rep:cugPolicy`), service users and the Sling Service User Mapper, and `repoinit` provisioning.

## When To Use This Skill

- "User can view but cannot edit / activate / delete a page or asset."
- "User sees a 403, blank console, or missing tab/action in author."
- "Group permission change did not take effect" or "permissions differ between two users."
- "New service user / system user cannot read or write its path."
- Reviewing an ACL change before it ships in `ui.apps`/`ui.config` or via `repoinit`.

For replication/activation failures that are not permission-related, use `../aem-replication/SKILL.md`. For Dispatcher-level 403s on publish, use `../dispatcher/SKILL.md`. For general component/Sling work, stay in `../aem-65-onprem/SKILL.md`.

## Core Model (read this first)

1. A user's effective permissions are the aggregate of all ACEs that match **any** principal the user has: the user itself, every group it belongs to (declared and inherited), and the built-in `everyone` group.
2. Evaluation is per repository path. ACEs on a node take precedence over ACEs inherited from ancestors. At the same node, a matching **deny overrides a matching allow** for the same privilege and restriction scope.
3. `rep:DenyACE` for `everyone` is the most common cause of "nobody can access X". A deny high in the tree (for example on `/content`) can shadow an allow deeper down depending on level and restrictions.
4. Restrictions narrow an ACE. An allow with `rep:glob=*/jcr:content*` does not grant the node itself. A missing or surprising `rep:glob` is a frequent false "permission is set but does not work" cause.
5. `admin` and members of `administrators` effectively have full access; do not test "does the ACL work" as `admin`.

See [references/permission-evaluation.md](./references/permission-evaluation.md) for the full evaluation rules, privilege aggregation, and restriction semantics.

## Debugging Workflow

1. **Reproduce as the real user, not admin.** Confirm the exact path, action (read/edit/replicate/delete), and the failing URL or operation.
2. **Identify the principals.** In the Security console open the user and list its groups; expand to inherited membership. Note `everyone` always applies. See [references/tools-and-queries.md](./references/tools-and-queries.md) for console paths and JCR queries to resolve membership.
3. **Read effective permissions for the path.** Use the user's **Permissions** view in the Security console and test the specific path. This shows the net allow/deny per privilege and is the fastest source of truth.
4. **Find the deciding ACE.** Walk the path from the target node up to `/`, inspecting each `rep:policy` in CRXDE Lite. Look for `rep:DenyACE` matching the user/groups/`everyone`, then for the missing/over-narrow allow (check `rep:glob` and other restrictions).
5. **Check for CUG.** If the subtree has a `rep:cugPolicy`, only listed principals get read access regardless of other allows. Verify membership against the CUG principals.
6. **Check authorizable state.** Confirm the user is not disabled (`rep:disabled`), is the principal you think it is (`rep:principalName`), and for service users that the Service User Mapper binds the bundle/sub-service to the right system user.
7. **Confirm the source of truth.** Decide whether the ACL is authored in the repo (`rep:policy` in a content package), set by `repoinit`, or set manually in the running instance. Manual-only changes are lost on redeploy if the package re-imports that path.
8. **Apply the smallest fix** (see Remediation), then re-test as the real user and clear any cached permission state by re-login.

## Remediation Patterns

- Prefer adding a **specific allow** at the right node over removing a broad deny, unless the deny itself is the bug.
- Grant to a **group**, not to individual users. Add the user to the group instead of creating per-user ACEs.
- Use the **narrowest privilege**: `jcr:read` for view, `rep:write` for edit, `crx:replicate` for activation/deactivation, `jcr:modifyAccessControl` only for users who manage ACLs.
- Use restrictions intentionally. To cover a page and its content, grant on the page node without a `rep:glob`, or pair node-level and `*/jcr:content*` grants as needed.
- Make the change reproducible: codify it in `repoinit` (preferred for service users and structural ACLs) or in the appropriate content package, not only in the running author. See [references/tools-and-queries.md](./references/tools-and-queries.md) for a `repoinit` example.
- For service users, register the system user and ACLs via `repoinit` and bind via a Service User Mapper amendment; never use admin sessions.

## Guardrails

- Diagnose read-only first. Do not edit `rep:policy` nodes, group membership, or `repoinit` on a running production author without confirming the change set and rollback.
- Never grant `jcr:all` or broad `/content` allows to fix a narrow problem.
- Do not add ACEs directly under `/libs`. Overlay or target `/apps`, `/conf`, `/content`, `/home` as appropriate.
- Changing ACLs on `/home/users` or `/home/groups` can lock users out of their own profiles; be deliberate.
- Remember Dispatcher and publish: an author permission fix does not change a publish-tier or Dispatcher 403. Verify which tier returns the error.
- After membership or ACL changes, the affected user must re-login for a fresh permission evaluation.

## Review Checklist

- The failing action maps to a concrete privilege (read / `rep:write` / `crx:replicate` / ACL-management).
- Effective permissions were read for the **actual user**, not `admin`.
- The deciding ACE (deny or missing/over-narrow allow) was located on a specific node, with its restrictions understood.
- The fix grants to a group with the narrowest privilege and correct restriction scope.
- CUG, disabled state, and service-user mapping were ruled out where relevant.
- The change is codified (repoinit or content package) and will survive redeploy.
- Re-tested as the real user; publish/Dispatcher impact considered separately.
