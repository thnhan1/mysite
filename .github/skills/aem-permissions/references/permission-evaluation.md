# Permission Evaluation In AEM 6.5 (Jackrabbit Oak)

Reference for how Oak computes a user's effective permissions, used by the `aem-permissions` skill.

## Principals That Apply To A User

Effective permissions aggregate ACEs matching **any** principal in the user's principal set:

- The user's own principal (`rep:principalName` on the `rep:User` node).
- Every group the user is a declared member of.
- Every group inherited transitively (group-in-group membership).
- The built-in `everyone` group, which matches all authenticated and anonymous principals.

`administrators` group members and the `admin` user bypass ACL evaluation and have full access. Never validate an ACL by testing as `admin`.

## How ACEs Are Stored

- An access control list on a node is a child node `rep:policy` of type `rep:ACL`.
- Each entry is an ordered child of type `rep:GrantACE` (allow) or `rep:DenyACE` (deny).
- Each entry has:
  - `rep:principalName` — the principal the entry applies to.
  - `rep:privileges` — multi-value list of privileges (see below).
  - Optional restriction properties (see Restrictions).

## Evaluation Rules

1. **Path-based, top-down with descendant precedence.** Permissions are computed per target path. ACEs on a node take precedence over ACEs inherited from ancestor nodes.
2. **Deny overrides allow at the same scope.** For the same path, principal set, privilege, and matching restriction, a deny wins over an allow.
3. **Most specific entry decides.** A grant/deny on the target node or a closer ancestor overrides a conflicting entry on a more distant ancestor.
4. **Aggregation across principals.** If any applicable principal is granted a privilege at the deciding scope and none denies it there, the user has it.
5. **No implicit grant.** Without a matching allow, the action is denied. AEM ships default ACLs (for example for `everyone`, `contributor`, `content-authors`) that you must account for.

A practical consequence: a `rep:DenyACE` for `everyone` placed high in the tree is the classic cause of "no one can access this subtree", because `everyone` matches every principal.

## Privileges (common)

- `jcr:read` — read nodes and properties (view).
- `rep:write` — aggregate of `jcr:modifyProperties`, `jcr:addChildNodes`, `jcr:removeNode`, `jcr:removeChildNodes`, `jcr:nodeTypeManagement` (edit/create/delete content).
- `jcr:modifyProperties`, `jcr:addChildNodes`, `jcr:removeChildNodes`, `jcr:removeNode` — granular write parts.
- `crx:replicate` — activate/deactivate (replication) of the path.
- `jcr:readAccessControl` — read `rep:policy` nodes (needed even to inspect ACLs).
- `jcr:modifyAccessControl` — change ACLs.
- `jcr:versionManagement`, `jcr:lockManagement`, `jcr:nodeTypeManagement` — versioning, locking, node types.
- `jcr:all` — everything. Avoid granting this to fix narrow problems.

Map the failing user action to the smallest privilege: view -> `jcr:read`; edit/create/delete -> `rep:write`; publish -> `crx:replicate`.

## Restrictions

Restrictions narrow when an ACE applies. Misused restrictions are a frequent "permission set but not working" cause.

- `rep:glob` — path-pattern restriction relative to the ACL node.
  - `` (empty) — the access-controlled node only.
  - `*` — the node and all descendants.
  - `*/jcr:content*` — the `jcr:content` subtree but **not** the node itself.
  - A leading `/segment` matches specific descendant paths.
- `rep:ntNames` — restrict to nodes of given node types.
- `rep:itemNames` — restrict to named items.
- `rep:prefixes` — restrict by namespace prefix.

When an allow "does not work", check whether `rep:glob` excludes the exact node being accessed (for example a grant only on `*/jcr:content*` will not allow reading or moving the page node itself).

## Closed User Groups (CUG)

- A CUG is represented by a `rep:cugPolicy` node listing allowed principals (`rep:principals`).
- When CUG authorization is enabled and a subtree has a `rep:cugPolicy`, **read access is restricted to the listed principals**, on top of normal ACL evaluation.
- Symptom: a user has a normal `jcr:read` allow but still cannot read a CUG-protected subtree because they are not in the CUG principal list.
- CUG affects read access; other privileges still follow standard ACEs.

## Service And System Users

- System users live under `/home/users/system` and have no password (cannot log in interactively).
- Code obtains a service resolver via the Sling Service User Mapper (bundle + sub-service -> system user); it must not use admin sessions.
- A service-user permission failure is usually either a missing ACL grant on its target path or a missing/incorrect Service User Mapper amendment.

## Reasoning Order (cheat sheet)

1. Which principals does the user actually have (including inherited groups and `everyone`)?
2. What is the net effective permission for the privilege at the exact path?
3. Which ACE decides it — a deny, or a missing/over-narrow allow?
4. Is a CUG, disabled flag, or service-user mapping involved?
5. Where does the ACL come from (repoinit / package / manual) so the fix persists?
