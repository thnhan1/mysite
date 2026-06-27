# Tools, Queries, And APIs For Permission Debugging (AEM 6.5)

Concrete tools, JCR queries, logging, and code for the `aem-permissions` skill. Replace `<author>` with the author host:port (for example `localhost:4502`). Run read-only inspection first.

## Consoles And UIs

- **Security console (users):** `/security/users.html` — list users, open a user, view its groups and the **Permissions** tab.
- **Security console (groups):** `/security/groups.html` — group members and nested membership.
- **Effective permissions / test a path:** open the user in the Security console, switch to **Permissions**, navigate to or enter the path, and read the net allow/deny per privilege. This is the fastest source of truth.
- **Classic User Admin (legacy):** `/useradmin` — includes a per-user **Test** action to check a path/privilege.
- **CRXDE Lite:** `/crx/de` — inspect `rep:policy` nodes along the path and the authorizable nodes under `/home/users` and `/home/groups`. Requires `jcr:readAccessControl` on the inspected paths.
- **User profile / authorizable node:** under `/home/users/<hashed>/<id>`; check `rep:principalName`, `rep:disabled`, `rep:authorizableId`, and `profile`.

## Inspecting ACLs Directly

- In CRXDE Lite, select the target node and open its `rep:policy` child to read ordered `rep:GrantACE` / `rep:DenyACE` entries, their `rep:principalName`, `rep:privileges`, and restrictions (`rep:glob`, etc.).
- Walk from the failing node up to `/`, checking each level's `rep:policy`. The deciding entry is usually the closest matching deny, or a missing/over-narrow allow.
- Reading `rep:policy` itself requires `jcr:readAccessControl`; if you cannot see policies, you lack that privilege (or are not admin).

## JCR / SQL2 Queries

Resolve a user node by id:

```sql
SELECT * FROM [rep:User] AS u WHERE u.[rep:authorizableId] = 'jdoe'
```

Find groups (to inspect `rep:members`):

```sql
SELECT * FROM [rep:Group] AS g WHERE g.[rep:authorizableId] = 'content-authors'
```

Find disabled users:

```sql
SELECT * FROM [rep:User] AS u WHERE u.[rep:disabled] IS NOT NULL
```

Find access control entries for a principal (entries reference the principal by name):

```sql
SELECT * FROM [rep:ACE] AS ace WHERE ace.[rep:principalName] = 'content-authors'
```

Find Closed User Group policies:

```sql
SELECT * FROM [rep:CugPolicy] AS cug
```

Run queries from `/crx/de` (Tools > Query) or the Query Performance tool. Prefer indexed property constraints and a path scope where possible.

## Logging

Enable temporary DEBUG loggers (Felix Console > Sling > Log Support, or an OSGi `org.apache.sling.commons.log.LogManager.factory.config`) and reproduce as the real user:

- `org.apache.jackrabbit.oak.security.authorization.permission` — permission evaluation decisions.
- `org.apache.jackrabbit.oak.security.authorization` — access control management.
- `org.apache.jackrabbit.oak.security.user` — user/group management.

Write to a dedicated log file and lower the level back to INFO/WARN after capturing the failure. On a busy author these loggers are noisy; scope the reproduction tightly.

## API Checks (servlet / JMX / Groovy console where available)

Check an effective permission for the current session:

```java
Session session = resourceResolver.adaptTo(Session.class);
// JCR actions: "read", "add_node", "set_property", "remove"
boolean canEdit = session.hasPermission("/content/site/en", Session.ACTION_SET_PROPERTY);

AccessControlManager acm = session.getAccessControlManager();
Privilege[] write = new Privilege[] { acm.privilegeFromName("rep:write") };
boolean hasWrite = acm.hasPrivileges("/content/site/en", write);
```

Resolve a user's groups programmatically:

```java
UserManager userManager = ((JackrabbitSession) session).getUserManager();
Authorizable user = userManager.getAuthorizable("jdoe");
Iterator<Group> groups = user.memberOf(); // transitive (inherited) membership
```

Obtain a service resolver for a service user (never an admin session):

```java
Map<String, Object> auth = Collections.singletonMap(
    ResourceResolverFactory.SUBSERVICE, "my-subservice");
try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(auth)) {
    // operate as the mapped system user
}
```

## Persisting A Fix With repoinit

Prefer `repoinit` for system users and structural ACLs so the change survives redeploy. Example OSGi `repoinit` script (via `org.apache.sling.jcr.repoinit.RepositoryInitializer` config in `ui.config`):

```
create service user my-service-user

create path /content/site

set ACL for my-service-user
    allow jcr:read on /content/site
    allow rep:write on /content/site restriction(rep:glob, "*/jcr:content*")
end
```

Bind the service user with a Service User Mapper amendment:

```
org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended-myapp
  user.mapping=["com.example.myapp:my-subservice=my-service-user"]
```

For author-only authoring ACLs, granting to a group (and adding users to the group) in a content package or repoinit is preferable to per-user ACEs.

## Quick curl Inspection (read-only)

Read a user's groups via the authorizable JSON (needs read access to `/home`):

```
curl -u <admin> "http://<author>/home/users/.../<id>.json"
```

Note: `rep:policy` nodes are protected; expose them through CRXDE/Security console rather than relying on plain JSON, and never script ACL changes against production without an approved change and rollback.
