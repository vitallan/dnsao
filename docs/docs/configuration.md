# Configuration

**DNSao** requires two configuration files:

* [Logback XML](#logback-xml)
* [YML configuration](#configuration-yml)

You can find configuration examples [in the GitHub project]({{sample_conf_url}}), but below are examples and references.

## Logback XML

Logback is a well-known standard in the Java community, and the file read by **DNSao** must follow the logback format. When in doubt, just follow [the documentation](https://logback.qos.ch/documentation.html).

Below is an example of logs set at the INFO level.

```xml
<configuration>

    <property name="LOG_DIR" value="/var/log/dnsao"/>
    <property name="LOG_FILE" value="${LOG_DIR}/dnsao.log"/>
    <property name="LOG_PATTERN" value="[%d{HH:mm:ss.SSS}] %-5level [%replace(%thread){'com.allanvital.dnsao.Main.main','main'}] [%logger] %msg%n" />

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_FILE}</file>
        <append>true</append>
        <prudent>false</prudent>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_DIR}/dnsao.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>5GB</totalSizeCap>
            <cleanHistoryOnStart>true</cleanHistoryOnStart>
            <maxFileSize>100MB</maxFileSize>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <appender name="ASYNC_CONSOLE" class="ch.qos.logback.classic.AsyncAppender">
        <queueSize>2048</queueSize>
        <discardingThreshold>90</discardingThreshold>
        <neverBlock>true</neverBlock>
        <appender-ref ref="CONSOLE"/>
    </appender>

    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <queueSize>8192</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <neverBlock>false</neverBlock>
        <appender-ref ref="FILE"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="ASYNC_CONSOLE"/>
        <appender-ref ref="ASYNC_FILE"/>
    </root>

    <logger name="org.xbill.DNS" level="OFF"/>
    <logger name="io.javalin" level="OFF"/>
    <logger name="org.eclipse.jetty" level="OFF"/>
</configuration>

```

**DNSao** has three main loggers: **DNS, CACHE, and INFRA**. If you prefer a more privacy-oriented setup, just change the root level of the XML above to "WARN" — queries and other expected events will not be logged.
For troubleshooting, analysis, or personal preference, you can set each log individually:

```xml
<logger name="DNS" level="INFO" additivity="false">
    <appender-ref ref="ASYNC_CONSOLE" />
    <appender-ref ref="ASYNC_FILE"/>
</logger>
<logger name="CACHE" level="OFF" additivity="false">
    <appender-ref ref="ASYNC_CONSOLE" />
    <appender-ref ref="ASYNC_FILE"/>
</logger>
<logger name="INFRA" level="DEBUG" additivity="false">
    <appender-ref ref="ASYNC_CONSOLE" />
    <appender-ref ref="ASYNC_FILE"/>
</logger>
```

## Configuration YML

This is the only configuration file for the application. Here you can define which ports will be used, which features are enabled, which upstreams will be used, local mappings, and which DNS blocklists will be applied. Below is a complete example:

```yaml
server:
  port: 53 
  udpThreadPool: 10
  tcpThreadPool: 3
  webPort: 8044

cache:
  enabled: true
  maxCacheEntries: 1000
  rewarm: true
  maxRewarmCount: 5

resolver:
  tlsPoolSize: 5
  multiplier: 3
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

  blocklists:
    - "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"

```

### server

```yaml
server:
  port: 53 
  udpThreadPool: 10
  tcpThreadPool: 3
  webPort: 8044
```

The **server** property defines the application’s top-level properties.

| Property | Description |
|---------|------------|
| **port** | the port where the application will listen for UDP and TCP calls, as per DNS standards. Default for DNS servers is 53 |
| **udpThreadPool** | how many threads will be available for UDP protocol, as the **server**. The default value is 10 |
| **tcpThreadPool** | how many threads will be available for TCP protocol, as the **server**. The default value is 3 |
| **webPort** | the port where the metrics dashboard will be available. Default is 8044 |

### cache

```yaml
cache:
  enabled: true
  maxCacheEntries: 1000
  rewarm: true
  maxRewarmCount: 5
```

The **cache** property defines the application’s cache behavior. The cache is the main component responsible for speeding up DNS queries.

| Property | Description |
|---------|------------|
| **enabled** | whether or not the cache will be setup. You can set to false to disable caching — this is **not recommended**, as it will significantly worsen performance and break DNS query/TTL logic. It may be useful for troubleshooting. Default is **true** |
| **maxCacheEntries** | the maximum number of entries allowed in the cache. 1000 is a good number for home networks and fits within the recommended memory usage (as noted in [installation](installation.md)). If you increase this number, remember to also increase the JVM memory limit. |
| **rewarm** | enable the "cache rewarm" mechanism: when a cache entry is near the end of its TTL, a refresh attempt is made automatically. Default is **true** |
| **maxRewarmCount** | how many times **DNSao** will rewarm the cache entry before removing it from memory. If a query arrives for a domain in the “warm” cache, such entry is promoted to “hot” cache and its rewarm counter resets. This ensures that frequently accessed domains stay available, improving DNS resolution performance. Default is **5** |

### resolver

```yaml
resolver:
  tlsPoolSize: 5
  multiplier: 3
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

  blocklists:
    - "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
```

The **resolver** property defines the upstreams to be queried. You must specify the IP, port, and protocol (“dot” or “udp”). These are the top level properties:

| Property | Description |
|---------|------------|
| **tlsPoolSize** | the maximum pool size for DOT connections per upstream. Using a pool improves performance since the TLS handshake is costly, but increasing it excessively won’t necessarily improve speed — a single connection can serve multiple requests and stale connections are discarded by the upstream |
| **multiplier** | how many upstreams each query will be sent to. **DNSao** uses the fastest response and discards the others. There’s a trade-off between speed and privacy: the more upstreams queried per request, the more servers will see your DNS queries. If privacy is the main goal, set the multiplier to 1 and use a DOT upstream. |

These are the inner properties for the "upstreams" property.

| Property | Description |
|---------|------------|
| **upstreams** | the list of upstreams to be queried against |
| **ip** | IP of the upstream server to be used |
| **port** | Port of the upsream server to be used. For UDP the common port is 53, for DOT, the default port is 853 | 
| **protocol** | supported protocols are **udp** and **dot** |
| **tlsAuthName** | when using **dot** protocol, it is necessary to also set the **tlsAuthName** for remote server validation. This name is confirmed during startup and if the authority verification fails, the upstream is discarded and not used |

It is also possible to set **localMappings**: dns entries that will be resolved directly by **DNSao**.

| Property | Description |
|---------|------------|
| **domain** | the domain to be mapped |
| **ip** | the ipv4 that will be sent as response. Only ipv4 is available at the moment |

You can also set **blocklists**: these are remote URLs that **DNSao** will download and parse to block certain domains. Any domain mapped in these lists will be answered with the **0.0.0.0** ip, following the DNS Sink Hole standards. Lists supported can be in hosts format:

```txt
87.123.55.32 domain.to.be.blocked       # the ip is ignored, all domains will go to 0.0.0.0
```

or simple list:

```txt
domain1.to.be.blocked
domain2.to.be.blocked
domain3.to.be.blocked
```

A common usecase is to use [StevenBlack](https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts), which is a regularly updated famous block list.

<div style="margin-bottom: 60px;"></div>
