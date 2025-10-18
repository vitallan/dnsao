---
title: Home
---

# DNSao

<p align="center">
  <img src="/dnsao/assets/logo.svg" alt="DNSao Logo" width="200">
</p>
<p align="center">
<strong>DNSao</strong> is a high-performance multi-upstream DNS forwarder
</p>

---

- **DNS Sinkhole**: blocks domains based on lists, acting as a [DNS sinkhole](https://en.wikipedia.org/wiki/DNS_sinkhole){:target="_blank"}, preventing access to unwanted content, invasive advertising, and hindering trackers
- **DNS over TLS Support**: performs queries to upstream servers using traditional UDP or [DoT](https://en.wikipedia.org/wiki/DNS_over_TLS){:target="_blank"}, ensuring greater privacy in resolutions
- **Parallel Querying to Multiple Upstreams**: sends the same query to multiple upstream servers in parallel and returns the fastest response, reducing browsing latency
- **High-Performance Cache**: stores responses respecting their original TTL, includes negative caching, and a rewarm (pre-heating) mechanism to keep frequently used entries always available
- **DNSSEC-Aware Policies**: requests DNSSEC data from upstream resolvers and applies configurable policies (off/simple/rigid) based on the AD flag
- **YAML Configuration**: all server configuration is centralized in a single .yaml file, easy to version and replicate across multiple instances for high availability
- **Local DNS Mapping**: allows defining local domain resolutions for specific IPs — ideal for homelabs, self-hosting, and internal networks
- **Metrics Dashboard**: provides a metrics dashboard to monitor operation and performance
- **Low Resource Usage**: runs comfortably with 256 MB of RAM, even on older hardware or compact devices
- **Free and Open Source**: free software, openly maintained [on GitHub](https://github.com/vitallan/dnsao), allowing unrestricted auditing and use
- **Stateless Runtime**: does not rely on databases or peripheral systems, enabling fast cold starts
- **Low Number of Dependencies**: only 5 — dnsjava, logback, javalin, minimal-json, and snakeyaml

---

## Purpose

Other DNS software acts as a [DNS Sinkhole](https://docs.pi-hole.net/){:target="_blank"}, or supports multiple [DoT upstreams](https://github.com/getdnsapi/stubby){:target="_blank"}, or performs [recursive DNS resolution](https://nlnetlabs.nl/projects/unbound/about/){:target="_blank"}, but it’s always necessary to combine solutions to achieve a satisfactory level of privacy or speed. **DNSao** exists to be the only DNS tool your network needs.

---

All source code is available on the project’s [GitHub](https://github.com/vitallan/dnsao), including the latest releases and development documentation.

[Learn how to install and use!](installation.md){ .md-button .md-button--primary }
{: style="text-align:center" }

## Screenshots

### Query Summary

<p align="center">
  <img src="/dnsao/assets/screenshot-summary.png" alt="summary">
</p>

### Graph with queries timeline 

<p align="center">
  <img src="/dnsao/assets/screenshot-timeline.png" alt="timeline graph">
</p>

### Upstream distribution

<p align="center">
  <img src="/dnsao/assets/screenshot-upstream-distribution.png" alt="upstream distribution">
</p>

[Learn how to install and use!](installation.md){ .md-button .md-button--primary }
{: style="text-align:center" }

<div style="margin-bottom: 60px;"></div>
