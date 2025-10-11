# Backlog

**DNSao** is still in the early stages of development, but to record the next ideas and planned steps for the tool, here’s what’s on the list:

* [Metrics](#metrics-improvement)
* [Javalin](#remove-javalin)
* [Recursive DNS Resolution](#recursive-resolution)
* [DNSSEC](#dnssec)
* [Move Away from MkDocs](#move-away-from-mkdocs)

## Metrics Improvement

The **DNSao** metrics dashboard is still very simple and doesn’t yet include all the information needed for full operation solely through the administrative interface. In scenarios where query logging is enabled, it would be useful to view queries directly from the admin panel. 

Global counters also lose relevance over time and should be replaced with temporal ones (for example, last 24 hours). The granularity of statistics could also improve, as well as showing metrics per client.

## Remove Javalin

UDP and TCP servers are created and managed directly within **DNSao**, but the web server was built using **Javalin**. There were several reasons for this choice, mainly to avoid spending time implementing an HTTP server in the early stages of the project.

Although **Javalin** is an excellent project and fits well with lightweight runtime goals, it makes more sense to have an internal HTTP server dedicated to serving metrics.

## Recursive Resolution

Similar to [Unbound](https://nlnetlabs.nl/projects/unbound/about/){:target="_blank"}, this feature would make **DNSao** a DNS resolver, not just a forwarder.
With this capability, DNS queries wouldn’t be forwarded to an “upstream” — they would be resolved directly by **DNSao** through root servers.

## DNSSEC

With recursive resolution and support for [DNSSEC validation](https://en.wikipedia.org/wiki/Domain_Name_System_Security_Extensions){:target="_blank"}, **DNSao** would become a complete DNS server, ensuring request integrity and validating all keys across the hierarchical resolution chain.
This would allow users to choose whichever DNS setup best fits their environment — but always using **DNSao** as the core component.

## Move Away from MkDocs

The worst tool for building a static site.

<div style="margin-bottom: 60px;"></div>
