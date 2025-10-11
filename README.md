<p align="center">
  <img src="" alt="DNSao" width="150" />
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
</p>


**DNSao** is a multi-upstream DNS forwarder written in Java — featuring caching, DNS-over-TLS (DoT) support, and a built-in dashboard for real-time metrics.

> DNSao is currently under active development

---

## Overview

DNSao is designed to serve as a lightweight, configurable, and privacy-oriented DNS forwarder.  

It handles both UDP and TCP queries, and supports modern encrypted upstreams (DoT), while maintaining a flexible caching system and a simple web interface for live metrics.

### Current Features

- Answers both UDP and TCP for local DNS queries
- Multiple upstream DNS resolvers (UDP/DoT)
- TTL-aware caching system with asynchronous rewarm
- Query logging (optional)
- Real-time metrics dashboard built with Bulma + Chart.js
- Pluggable blocklists and local mappings/overrides
- Configuration via a single `application.yml`
- Systemd-ready installation and lightweight footprint

---

More info can be found at the documentation.

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

## Configuration

All app configuration options are defined in a single `application.yml`.

More detailed info can be found at the documentation.

---

## Dashboard

DNSao provides a built-in **metrics dashboard** that displays:

* Query volume
* Cache hit/miss rate
* Average response time
* Upstream distribution

Access it via the admin port configured in your `application.yml` (e.g. `http://localhost:8044`).

---

## Contributing

Contributions are welcome!
Open issues, suggest improvements, or submit pull requests directly on [GitHub](https://github.com/vitallan/dnsao).

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

* [Official Documentation](https://github.com/vitallan/dnsao)
* [GitHub Repository](https://github.com/vitallan/dnsao)

---

Licensed under the MIT License © 2025 Allan Vital