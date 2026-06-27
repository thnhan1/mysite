
Based on the AEM replication skill analysis, here's a comprehensive diagnosis of your Content Fragment replication permission issue.

Root Cause Analysis

When you replicate a Content Fragment in AEM 6.5, the system checks permissions on more paths than just the DAM asset and its configuration folder. The error you're seeing (/dam/cf/vietnam/dining, /conf/settings/mysite/cf/dining) suggests JCR permission checks are failing. Here are the likely missing permissions:

---
1. Replication Queue Paths — Most Commonly Missed

Even if a user has crx:replicate on the content, they also need write access to the replication infrastructure to queue the replication job:

┌────────────────────────────────┬────────────────────────────────────────┬──────────────────────────────────────┐
│              Path              │           Required Privilege           │               Purpose                │
├────────────────────────────────┼────────────────────────────────────────┼──────────────────────────────────────┤
│ /var/replication/data          │ jcr:read, jcr:write, jcr:addChildNodes │ Queueing replication jobs            │
├────────────────────────────────┼────────────────────────────────────────┼──────────────────────────────────────┤
│ /etc/replication/agents.author │ jcr:read                               │ Discovering/using replication agents │
└────────────────────────────────┴────────────────────────────────────────┴──────────────────────────────────────┘

This is the #1 most overlooked permission for non-admin replication users. Without write on /var/replication/data, the user can't create the replication queue entry.

---
2. Content Fragment Model Paths

The path /conf/settings/mysite/cf/dining in your error is not the standard CF model path. In AEM 6.5, CF models follow this pattern:

/conf/<config-name>/settings/dam/cfm/models/<model-name>

So your dining CF model is likely at:
/conf/settings/mysite/dam/cfm/models/dining

Or possibly under global:
/conf/global/settings/dam/cfm/models/...

When AEM replicates a CF, it validates the fragment against its model. If the user lacks jcr:read on the model node (or any ancestor), it fails. Check and grant read on:

- /conf/settings/mysite/dam/cfm/models/dining (and all ancestor paths)
- /conf/global/settings/dam/cfm (if the model inherits from global)

---
3. Intermediate Path Read Access (JCR Traversal)

AEM's permission model requires jcr:read on every intermediate node in a path — not just the leaves. Verify the user's group has at least jcr:read on ALL of these:

/content
/content/dam
/content/dam/cf
/content/dam/cf/vietnam
/conf
/conf/settings
/conf/settings/mysite
/conf/settings/mysite/cf              ← check this one specifically
/conf/settings/mysite/dam
/conf/settings/mysite/dam/cfm

If any ancestor denies read, the entire path is blocked.

---
4. Content Fragment Sub-Nodes

A CF in JCR is not a single node — it's a tree:

/content/dam/cf/vietnam/dining
  ├── jcr:content
  │   ├── data
  │   │   └── master      ← actual CF variation data
  │   └── metadata
  └── renditions (if any)

The user needs crx:replicate + jcr:read on the entire subtree, not just the parent node. Ensure ACLs are not restricted to only the top-level node.

---
5. Referenced Content

If your dining CF references other assets, fragments, or content (images, other CFs, tags), the replication process validates permissions on those referenced paths too. The user needs at least jcr:read on all referenced content.

---
Recommended Fix — Permission Checklist

Apply these to the user's group via Tools → Security → Permissions:

┌─────┬─────────────────────────────────────────────┬────────────────────────────────────────┬──────────────────────┐
│  #  │                    Path                     │               Privileges               │        Reason        │
├─────┼─────────────────────────────────────────────┼────────────────────────────────────────┼──────────────────────┤
│ 1   │ /content/dam/cf/vietnam/dining              │ jcr:read, jcr:write, crx:replicate     │ Already done ✓       │
├─────┼─────────────────────────────────────────────┼────────────────────────────────────────┼──────────────────────┤
│ 2   │ /conf/settings/mysite                       │ jcr:read, crx:replicate                │ Already done ✓       │
├─────┼─────────────────────────────────────────────┼────────────────────────────────────────┼──────────────────────┤
│ 3   │ /var/replication/data                       │ jcr:read, jcr:write, jcr:addChildNodes │ Likely missing       │
├─────┼─────────────────────────────────────────────┼────────────────────────────────────────┼──────────────────────┤
│ 4   │ /etc/replication/agents.author              │ jcr:read                               │ Likely missing       │
├─────┼─────────────────────────────────────────────┼────────────────────────────────────────┼──────────────────────┤
│ 5   │ /conf/settings/mysite/dam/cfm/models        │ jcr:read                               │ CF model read        │
├─────┼─────────────────────────────────────────────┼────────────────────────────────────────┼──────────────────────┤
│ 6   │ /conf/global/settings/dam/cfm               │ jcr:read                               │ Global CF model read │
├─────┼─────────────────────────────────────────────┼────────────────────────────────────────┼──────────────────────┤
│ 7   │ All intermediate ancestors (see list above) │ jcr:read                               │ JCR traversal        │
└─────┴─────────────────────────────────────────────┴────────────────────────────────────────┴──────────────────────┘

---
Quick Diagnostic: Replicate via curl

Test with the user's credentials to see the exact error:

curl -u <username>:<password> -X POST \
  http://localhost:4502/bin/replicate.json \
  -F "cmd=Activate" \
  -F "path=/content/dam/cf/vietnam/dining"

Check the JSON response and crx-quickstart/logs/error.log for the full stack trace — it will show exactly which path the permission check failed on.

---
Additional Check: Agent User Id

Open your replication agent at /etc/replication/agents.author/publish → Transport tab → Agent User Id. This service user (not the author user) is what actually writes to Publish. Verify this service user has jcr:read, jcr:write, crx:replicate on the Publish instance for the target paths. If Agent User Id is blank, it falls back to the admin session which usually works — but if a restricted service user is configured, that could be the bottleneck.

---
The most likely culprit is #3 — missing write permissions on /var/replication/data. This is the classic "user can read the content but can't queue replication" scenario. Check that first, then work through items #4–#7.

✻ Cooked for 1m 11s

※ recap: You're diagnosing why an AEM author can't publish a Content Fragment at `/content/dam/cf/vietnam/dining` despite having replicate permissions there. The most likely fix is granting `jcr:read`, `jcr:write`, `jcr:addChildNodes` on `/var/replication/data` to the user's group. (disable recaps in /config)