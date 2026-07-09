# Recursive Mode

**DNSao** supports recursive resolution directly from root hints. This mode is available and usable, but it is not yet a fully complete Internet-grade recursive resolver.

## What it is

In recursive mode, **DNSao** does not depend only on external upstream resolvers. Instead, it can resolve names iteratively by starting from root hints, following delegations, resolving nameserver addresses when glue is missing, and caching intermediate recursive steps.

## Enabling

Recursive mode is activated by **omitting the `resolver.upstreams` block** from your `application.yml`. When no upstreams are configured, DNSao automatically starts in recursive mode.

Minimal recursive configuration:

```yaml
server:
  port: 53
  webPort: 8044

cache:
  enabled: true
  maxCacheEntries: 1000

misc:
  timeout: 3
  dnssec: "simple"
# No resolver.upstreams block — this enables recursive mode
```

You may optionally set `resolver.rootHintsUrl` to override the default root hints source (`https://www.internic.net/domain/named.root`).

### Validating that recursive mode is active

At startup, look for this line in the logs:

```
Mode: recursive (no upstreams configured)
```

A warning will also appear to remind about the open resolver risk:

```
Recursive mode enabled; do not expose publicly (open resolver risk).
```

On the dashboard (`http://&lt;host&gt;:&lt;webPort&gt;/`), the "Recursion" card and timeline series will show queries being resolved recursively. If the server is in forward mode instead, the "Recursion" card will remain at zero.

## What is working

- iterative resolution from root hints
- fuller delegation walk across intermediate ancestor zones
- in-bailiwick glue handling
- out-of-bailiwick glue rejection
- sibling-domain glue rejection under stricter bailiwick rules
- no-glue nameserver resolution through helper lookups
- CNAME following and loop protection
- TCP fallback when UDP steps are truncated
- nameserver failover and limited racing between candidates
- intermediate recursive caching, including deeper delegated zones
- wall-clock and helper-resolution budgets to avoid unbounded resolution loops

## What is still missing

- DNSSEC validation
- negative caching with more complete SOA-based negative TTL handling
- DNAME support
- stronger referral and answer acceptance rules
- better handling of broken or lame nameservers

## Operational notes

- keep cache enabled when using recursive mode; recursive performance depends on it
- do not expose recursive mode publicly without network controls; open recursive resolvers are dangerous
- recursive mode still has implementation gaps, so the current target is usability rather than full protocol completeness
- outbound DNS reachability and root-hints bootstrap must work correctly for recursive mode to resolve names

<div style="margin-bottom: 60px;"></div>
