<p align="center">
  <img src="https://github.com/vitallan/dnsao/blob/main/docs/docs/assets/logo.svg?raw=true" width="150" alt="DNSao logo">
</p>

<h1 align="center">DNSao</h1>

<p align="center">
  <a href="https://www.java.com/">
    <img src="https://img.shields.io/badge/Java-17+-red.svg?style=for-the-badge" alt="Java">
  </a>
  <a href="https://maven.apache.org/">
    <img src="https://img.shields.io/badge/Build%20with-Maven-blue?style=for-the-badge&logo=apachemaven"/>
  </a>
  <a href="https://opensource.org/licenses/MIT">
    <img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge" alt="License">
  </a>
  <a href="https://vitallan.github.io/dnsao/">
    <img src="https://img.shields.io/badge/Documentation-DNSao-green?style=for-the-badge&logo=readthedocs" alt="License">
  </a>
</p>


**DNSao** is a multi-upstream DNS forwarder written in Java, featuring caching, privacy-oriented DNS querying support (DoT and DoH), and a built-in dashboard for real-time metrics.

> DNSao is currently under active development

---

## Overview

DNSao is designed to serve as a lightweight, configurable, and privacy-oriented DNS forwarder.  

It handles UDP, TCP and HTTP queries, and supports modern encrypted upstreams (DoT/DoH), while maintaining a robust caching system and a simple web interface for live metrics.

### Current Features

- Answers UDP, TCP and HTTP for local DNS queries
- Multiple upstream DNS resolvers (UDP/DoT/DoH)
- TTL-aware caching system with asynchronous rewarm
- DNSSEC aware with configurable policies
- Query logging (optional)
- Real-time metrics dashboard built with Bulma + Chart.js
- Pluggable blocklists and local mappings/overrides
- Configuration via a single `application.yml`
- Systemd-ready installation and lightweight footprint

---

More info can be found at the [documentation](https://vitallan.github.io/dnsao/).

## Building from Source

You can build DNSao using **Maven** and **Java 17 or later**.

```bash
git clone https://github.com/vitallan/dnsao.git
cd dnsao
mvn clean package -DskipTests
````

The resulting JAR will be located in:

```
target/dnsao-<version>.jar
```

To run it manually:

```bash
java -jar target/dnsao-<version>.jar \
  -Dconfig=/etc/dnsao/application.yml \
  -Dlogback.configurationFile=/etc/dnsao/logback.xml
```

or, using maven itself:

```bash
mvn exec:java
```
---

**Important**: the main branch is the current development branch, so it should not be considered stable. When wanting stable builds, always go for the "prod" tag. 

New releases and docker images are generated based on commit/push of a new tag in the format v0.0.0. The "latest" docker tag always points to the "prod" source tag and is built nightly.

## Configuration

All app configuration options are defined in a single `application.yml`.

More detailed info can be found at the [documentation](https://vitallan.github.io/dnsao/configuration/).

---

## Dashboard

DNSao provides a built-in **metrics dashboard** that displays:

* Query volume
* Searchable queries resolved
* Cache hit/miss rate
* Average response time
* Upstream distribution

Access it via the admin port configured in your `application.yml` (e.g. `http://localhost:8044`).

---

## Contributing

Contributions are welcome, but mostly for bug reporting, suggest improvements and general feedback. The server is fairly stable now, but some code is still being created, and a lot of inner interfaces might change without notice. 

Issues are the recommended form of contact [GitHub](https://github.com/vitallan/dnsao/issues).

---

## Credits

DNSao makes use of open-source libraries, including:

* [dnsjava](https://github.com/dnsjava/dnsjava) — DNS protocol implementation
* [Javalin](https://javalin.io) — lightweight web server
* [SnakeYAML](https://bitbucket.org/asomov/snakeyaml) — YAML configuration
* [Minimal-json](https://github.com/ralfstx/minimal-json) - JSON manipulation
* [Logback](https://logback.qos.ch) — logging framework

---

## Links

* [Official Documentation](https://vitallan.github.io/dnsao/)
* [My personal site](https://allanvital.com)

---

Licensed under the MIT License © 2025 Allan Vital
