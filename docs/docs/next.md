# Backlog

**DNSao** is still evolving, and this page tracks the next larger changes planned for the project.

* [Javalin](#remove-javalin)
* [Recursive Mode](#recursive-mode)
* [Move Away from MkDocs](#move-away-from-mkdocs)

These are long-term goals and backlog items.

## Remove Javalin

UDP and TCP servers are created and managed directly within **DNSao**, but the web server was built using **Javalin**. There were several reasons for this choice, mainly to avoid spending time implementing an HTTP server in the early stages of the project.

Although **Javalin** is an excellent project and fits well with lightweight runtime goals, it makes more sense to have an internal HTTP server dedicated to serving metrics.

## Recursive Mode

Recursive mode is already available in **DNSao** and is currently usable, especially for homelab-style scenarios. The items below track what is still missing to make it more complete.

### Negative caching

NXDOMAIN and NODATA answers should be cached more completely, including SOA-based negative TTL handling.

### DNAME support

Recursive alias handling already supports CNAME, but DNAME still needs to be implemented.

### Stronger referral and answer acceptance rules

Recursive mode already handles glue, no-glue referrals and stricter bailiwick rules, but more complete referral/answer acceptance rules are still needed.

### Better broken or lame nameserver handling

Recursive mode still needs stronger handling for lame, empty or otherwise broken nameservers, including improved retry and suppression behavior.

## Move Away from MkDocs

The worst tool for building a static site.

<div style="margin-bottom: 60px;"></div>
