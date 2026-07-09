# Configuration

**DNSao** requires a single configuration file:

* [YML configuration](#configuration-yml)

You can find configuration examples [in the GitHub project]({{sample_conf_url}}), but below are examples and references.

## Configuration YML

This is the only configuration file for the application. Here you can define which ports will be used, which features are enabled, which upstreams will be used, local mappings, and which DNS blocklists will be applied. Below is a complete example:

```yaml
server:
  port: 53 
  udpThreadPool: 10
  tcpThreadPool: 3
  statsDbPath: "/etc/dnsao/stats.db"
  webPort: 8044

cache:
  enabled: true
  maxCacheEntries: 1000
  rewarm: true
  maxRewarmCount: 5
  keep:
    - "url1.com"
    - "url2.com"

misc:
  timeout: 3
  queryLog: true
  refreshLists: false
  serveExpired: false
  serveExpiredMax: 86400
  dnssec: "simple"

resolver:
  tlsPoolSize: 5
  multiplier: 3
  upstreamThreadPoolSize: 64
  upstreamQueueSize: 640
  upstreams:
    - ip: "1.1.1.1"
      port: 853
      protocol: "dot"
      tlsAuthName: "cloudflare-dns.com"
    - ip: "1.0.0.1"
      port: 853
      protocol: "dot"
      tlsAuthName: "cloudflare-dns.com"
    - ip: "1.1.1.1"
      port: 53
      protocol: "udp"
    - ip: "1.0.0.1"
      port: 53
      protocol: "udp"
    - host: "dns.quad9.net"
      port: 443
      protocol: "doh"
      path: "/dns-query"
    - ip: "149.112.112.112"
      port: 853
      protocol: "dot"
      tlsAuthName: "dns.quad9.net"
    - ip: "9.9.9.9"
      port: 853
      protocol: "dot"
      tlsAuthName: "dns.quad9.net"

  localMappings:
    - domain: "ma-cool.domain.com"
      ip: "192.168.150.150"
    - domain: "ma-cool-2.domain.com"
      ip: "192.168.150.151"

lists:
  blockLists:
    steven: "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
    bets: "https://raw.githubusercontent.com/zangadoprojets/pi-hole-blocklist/refs/heads/main/Bets.txt"
  allowLists:
    allowList1: "http://url.of.allow.lists.com"

groups:
  group1:
    members:
      - "192.168.68.55"
      - "192.168.68.40"
    allows:
      - allowList1
    blocks:
      - steven
  group2:
    members:
      - "192.168.68.10"
  
listeners:
  http:
    - "http://host:port/listener"
```

---

### server

```yaml
server:
  port: 53 
  udpThreadPool: 10
  tcpThreadPool: 3
  httpThreadPool: 10
  statsDbPath: "/etc/dnsao/stats.db"
  webPort: 8044
```

The **server** property defines the application’s top-level properties.

| Property | Description |
|---------|------------|
| **port** | the port where the application will listen for UDP and TCP calls, as per DNS standards. Default for DNS servers is 53 |
| **udpThreadPool** | how many threads will be available for UDP protocol, as the **server**. The default value is 10 |
| **tcpThreadPool** | how many threads will be available for TCP protocol, as the **server**. The default value is 3 |
| **httpThreadPool** | how many threads will be available for HTTP protocol, as the **server**. The default value is 10 |
| **webPort** | the port where the metrics dashboard will be available. Default is 8044 |
| **statsDbPath** | path to a SQLite database file for metrics and query history. When unset/blank, **DNSao** defaults to `{tmpdir}/dnsao.db` (the OS temp directory). The parent directory must already exist and be writable (DNSao will not create directories) |
| **useMemoryStorage** | true/false, default is false. When true, forces in-memory stats storage instead of SQLite. Useful for ephemeral environments or when you want to avoid disk writes |

For HTTP queries, the endpoint will be **http://serverIp:webPort/dns-query**, following dns standards. Note that the answer will be in HTTP, not HTTPS. This is a concious decision to avoid manual handling of tls certificates to favor the users possibility to use their own certificates. If https is desired, it is recommended to use a reverse proxy that enables the remote communication to happen through https (like traeffic or nginx) and then reverse proxy internally to **DNSao**.

In the web interface **http://serverIp:webPort/** you can take a look at the server metrics and search the individual queries. The graph contains a rolling window of 24 hours divided in 10 minutes segments. 

**DNSao** persists queries and metrics to a local SQLite file by default. The buckets hold query events searchable in the */query* endpoint — with disk persistence there is no event cap, unlike the legacy in-memory mode which was limited to 5,000 events per segment. Writes are batched every 500ms; if the DB can't keep up, DNSao will drop the oldest buffered query events to avoid unbounded memory growth.

### cache

```yaml
cache:
  enabled: true
  maxCacheEntries: 1000
  rewarm: true
  maxRewarmCount: 5
  alwaysRewarmTopEntries: 0
  keep:
    - "url1.com"
    - "url2.com"
```

The **cache** property defines the application’s cache behavior. The cache is the main component responsible for speeding up DNS queries.

| Property | Description                                                                                                                                                                                                                                                                                                                         |
|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **enabled** | whether or not the cache will be setup. You can set to false to disable caching — this is **not recommended**, as it will significantly worsen performance and break DNS query/TTL logic. It may be useful for troubleshooting. Default is **true**                                                                                 |
| **maxCacheEntries** | the maximum number of entries allowed in the cache. 1000 is a good number for home networks and fits within the recommended memory usage (as noted in [installation](installation.md)). If you increase this number, remember to also increase the JVM memory limit.                                                                |
| **rewarm** | enable the "cache rewarm" mechanism: when a cache entry is near the end of its TTL, a refresh attempt is made automatically. Default is **true**                                                                                                                                                                                    |
| **maxRewarmCount** | how many times **DNSao** will rewarm the cache entry before removing it from memory. If a query arrives for a domain in the “warm” cache, such entry is promoted to “hot” cache and its rewarm counter resets. This ensures that frequently accessed domains stay available, improving DNS resolution performance. Default is **5** |
| **alwaysRewarmTopEntries** | the number of most-frequently-accessed cache entries that will always be rewarmed, bypassing **maxRewarmCount**. Unlike **keep**, these are not preloaded — they earn their spot through actual client queries. The top N non-keep entries by access recency are promoted automatically. Default is **0** (disabled). Clamped to **maxCacheEntries** |
| **keep** | a list of URLs to both precache and keep always warm. These urls will always trigger the rewarm mechanism and will not be enforced by the **maxRewarmCount** limit. **DNSao** will attempt to always keep those cached |

### misc 

```yaml
misc:
  timeout: 3
  queryLog: true
  refreshLists: false
  blockingEnabled: true
  serveExpired: false
  serveExpiredMax: 86400
  dnssec: "simple"
```

The **misc** property defines general purposes functions in **DNSao**.

| Property | Description |
|---------|------------|
| **timeout** | global timeout in seconds when querying upstream servers |
| **queryLog** | true/false, default is true. Controls whether individual query events are logged and stored. When false: query details are suppressed from the DNS log, sensitive fields (domain, client, type, answer) are stripped from events notified to subscribers, dashboard counters still work normally, but the query history table stays empty |
| **refreshLists** | true/false, default is false. When enabled, **DNSao** will periodically redownload the block and allow list to refresh the entries. If a lot of lists are used, this might overload the available memory. Allocate more memory if this is desired |
| **blockingEnabled** | true/false, default is true. When set to false, the block/allow unit is entirely disabled and all queries pass through without any list check. Useful for temporarily disabling filtering without removing list configurations |
| **serveExpired** | true/false, default is false. Adhering to dns rfc8767, when enabled, **DNSao** will serve data that is already expired when no available upstream resulted in a definitive answer (null, SERVFAIL and REFUSED) to maximixe dns availability |
| **serveExpiredMax** | default is 86400 (one day). When **serveExpired** is enable this is the maximum time in seconds that a local query will result in a cache hit before the entry is considered expired and removed |
| **dnssec** | Defines the general dnssec behavior of the server. More info in the bellow table. Default value is **simple**  |

#### dnssec

The **dnssec** property defines **DNSao** behavior about *DNSSEC* flags, validation and query padding, and has the default value as **simple**. The valid values are **off**, **simple** and **rigid**. **DNSao** does not execute the crypt validation of dnssec flags, but rely on the upstream answer to define it's behavior.

| Level | Description |
| ----- | ----------- |
| **off** | the request will be sent to the upstreams without adding specific DNSSEC flags. **DNSao** does not validate if the answer is DNSSEC validated |
| **simple** | the request will be sent to the upstreams adding DNSSEC flag. **DNSao** then replies to client, either validated or not, but does not block unvalidated ones. The query will also be padded to the nearest 128 byte size, to allow for extra obfuscation, following dns rfc7830 |
| **rigid** | the request will be sent to the upstreams adding DNSSEC flag. **DNSao** only replies to client if the DNSSEC is valid, otherwise, replies as **SERVFAIL**. The query will be padded to the full size of the package sent upstream, to allow for maximum obfuscation. *This will block several domains, since a lot of then does not have DNSSEC enabled* |

Regardless of the upstream answer, **DNSao** does not set the *AD* flag in the answer, since it does not execute the hash validations internally (follows dns rfc4035 3.2.3).

### resolver

```yaml
resolver:
  tlsPoolSize: 5
  multiplier: 3
  upstreamThreadPoolSize: 64
  upstreamQueueSize: 640
  upstreams:
    - ip: "1.1.1.1"
      port: 853
      protocol: "dot"
      tlsAuthName: "cloudflare-dns.com"
    - ip: "1.0.0.1"
      port: 853
      protocol: "dot"
      tlsAuthName: "cloudflare-dns.com"
    - ip: "1.1.1.1"
      port: 53
      protocol: "udp"
    - ip: "1.0.0.1"
      port: 53
      protocol: "udp"
    - ip: "149.112.112.112"
      port: 853
      protocol: "dot"
      tlsAuthName: "dns.quad9.net"
    - ip: "9.9.9.9"
      port: 853
      protocol: "dot"
      tlsAuthName: "dns.quad9.net"

  localMappings:
    - domain: "ma-cool.domain.com"
      ip: "192.168.150.150"
    - domain: "ma-cool-2.domain.com"
      ip: "192.168.150.151"

```

The **resolver** property defines the upstreams to be queried. You must specify the necessary configs for each protocol ("dot", "doh" or "udp"). These are the top level properties:

| Property | Description |
|---------|------------|
| **tlsPoolSize** | the maximum pool size for DOT connections per upstream. Using a pool improves performance since the TLS handshake is costly, but increasing it excessively won’t necessarily improve speed — a single connection can serve multiple requests and stale connections are discarded by the upstream |
| **multiplier** | how many upstreams each query will be sent to. **DNSao** uses the fastest response and discards the others. There’s a trade-off between speed and privacy: the more upstreams queried per request, the more servers will see your DNS queries. If privacy is the main goal, set the multiplier to 1 and use DOT or DOH upstreams. |
| **upstreamThreadPoolSize** | size of the shared thread pool used to execute upstream calls. Default is **64** |
| **upstreamQueueSize** | size of the bounded queue for upstream tasks. Default is **640** (64 * 10) |

#### Upstream Execution Internals

Upstream calls are executed via a shared `ThreadPoolExecutor` (`UpstreamThreadPoolExecutor`) configured with a fixed number of threads (`resolver.upstreamThreadPoolSize`) and a bounded queue (`resolver.upstreamQueueSize`).

When the pool and queue are saturated, **DNSao** uses `CallerRunsPolicy` as backpressure: the caller thread executes the upstream call inline, slowing down request handling instead of spawning new threads or growing memory unbounded.

The `resolver.multiplier` controls how many upstream tasks a single query may schedule; under saturation, tasks may queue or run inline due to backpressure.

These are the inner properties for the "upstreams" property.

| Property | Description |
|---------|------------|
| **upstreams** | the list of upstreams to be queried against |
| **ip** | IP of the upstream server to be used |
| **port** | Port of the upsream server to be used. For UDP the common port is 53, for DOT, the default port is 853 and for DOH, the default port is 443 | 
| **protocol** | supported protocols are **udp**, **dot** and **doh** |
| **tlsAuthName** | when using **dot** protocol, it is necessary to also set the **tlsAuthName** for remote server validation. This name is confirmed during startup and if the authority verification fails, the upstream is discarded and not used |
| **host** | when using **doh** protocol, it is necessary to set the **host** property, where the queries will be sent over https |
| **path** | when using **doh** protocol, it is necessary to set the **path** property, which will be appended at the end of the **host** property. It defaults to **/dns-query** |

You can find examples of the configurations on the [config-samples in the github repo](https://github.com/vitallan/dnsao/tree/main/config-samples). Upstreams of different types can be used at the same type, as long as the necessary properties for each protocol are fullfiled.

It is also possible to set **localMappings**: dns entries that will be resolved directly by **DNSao**.

| Property | Description |
|---------|------------|
| **domain** | the domain to be mapped |
| **ip** | the ipv4 that will be sent as response. Only ipv4 is available at the moment |

### lists

```yaml
lists:
  blockLists:
    steven: "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
    bets: "https://raw.githubusercontent.com/zangadoprojets/pi-hole-blocklist/refs/heads/main/Bets.txt"
  allowLists:
    allowList1: "http://url.of.allow.lists.com"
```

This config is optional, but can be used to set domain lists to be blocked. The expected parameter is a URL where **DNSao** can download such list. Any domain mapped in these lists will be answered with the **0.0.0.0** ip, following the DNS Sink Hole standards.

Lists supported can be in hosts format:

```txt
87.123.55.32 domain.to.be.blocked       # the ip is ignored, all domains will go to 0.0.0.0
```

or simple list:

```txt
domain1.to.be.blocked
domain2.to.be.blocked
domain3.to.be.blocked
```

A common usecase is to use [StevenBlack](https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts), which is a regularly updated famous block list. The links are downloaded at server start and are updated every 12 hour, if the **refreshLists** property is set as **true**.

It is also possible to configure **allowLists**: the download and reload behavior is similar to **blockLists**, but work as a list of domains that you do **not** want to block even when present in one of the **blockLists**. It is useful to allow access in scenarios where one blocklist blocks a domain that is actually useful to you.

Both **blockLists** and **allowLists** demand a name for each list (in the above example, the names are steven, bets and allowList1).

Names must be unique among both blockLists and allowLists to work effectively.

### groups

```yaml
groups:
  group1:
    members:
      - "192.168.68.55"
      - "192.168.68.40"
    allows:
      - allowList1
    blocks:
      - steven
  group2:
    members:
      - "192.168.68.10"
```

This config is also optional but can be used to selectively block or allow domains based on the client. That way, domains can be blocked for some devices, but not for all network.

In the above example, the group named **group1** will have two members (ips ending in 55 and 40), and will only block the domains on the "steven" list and allows only the "allowList1" list.

The group named **group2** will have a single member, and will not block or allow any specific list.

All clients not defined in a group will enter the default **MAIN** group.

The **MAIN** group can optionally be defined manually in YAML. When explicitly defined, its `members`, `allows`, and `blocks` are preserved as-is. When `main` is **absent** from the config, **DNSao** creates it automatically as a catchall using all blockLists and allowLists defined in the `lists` section.

### listeners

```yaml
listeners:
  http:
    - "http://host:port/listener"
```

Optional config that can be used to set external http listeners to receive the dns queries as POST requests. After each query is received and processed, **DNSao** will execute a POST request to that URL with the following body format:

```json
{
  "requestTime" : "2025-11-08 00:00:00.000",
  "queryResolvedBy" : "UPSTREAM",
  "client" : "192.168.66.123",
  "type" : "A",
  "domain" : "example.com",
  "answer" : "10.10.10.10",
  "source" : "9.9.9.9",
  "elapsedTimeInMs" : "1000"
}
```

**Source** field shows which upstream answered the query, in case the query was resolved by an upstream, otherwise it is null.

## Logging

Logging is configured via the `log` section in `application.yml`. **DNSao** uses `java.util.logging` (JUL) with three named loggers: **DNS**, **CACHE**, and **INFRA**.

### Configuration

```yaml
log:
  rootLevel: WARN
  dns: DEBUG
  cache: DEBUG
  infra: DEBUG
  # Optional file logging (printed to console if absent):
  file:
    path: "/var/log/dnsao/dnsao-%g.log"
    maxSize: 10485760
    maxFiles: 5
```

### Loggers

- **DNS** — DNS query resolution, server lifecycle
- **CACHE** — cache operations (hits, misses, rewarm)
- **INFRA** — infrastructure events (list loading, upstream connections)

### Levels

| Config value | JUL level | Description |
|---|---|---|
| `TRACE` | FINER | Fine-grained diagnostic details |
| `DEBUG` | FINE | General debug information |
| `INFO` | INFO | Normal operational messages |
| `WARN` | WARNING | Unexpected but recoverable situations |
| `ERROR` | SEVERE | Severe failures |
| `OFF` | OFF | Suppresses all messages |

### File logging

When `file.path` is set, DNSao writes logs asynchronously to a rolling file. The pattern supports `%g` as a generation index (e.g., `dnsao-0.log`, `dnsao-1.log`).

### Console output format

```
[HH:mm:ss.SSS] LEVEL [thread] [LOGGER] message
```

<div style="margin-bottom: 60px;"></div>
