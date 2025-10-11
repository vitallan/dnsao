# Configuração

O **DNSao** requer dois arquivos de configuração:

* [Logback XML](#logback-xml)
* [Configuração YML](#configuracao-yml)

Você pode encontrar exemplos de configuração [no projeto do GitHub]({{sample_conf_url}}), mas abaixo há exemplos e referências.

## Logback XML

O Logback é um padrão bem conhecido na comunidade Java, e o arquivo lido pelo **DNSao** deve seguir o formato do Logback. Em caso de dúvida, siga [a documentação](https://logback.qos.ch/documentation.html).

Abaixo está um exemplo com logs no nível INFO.

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

O **DNSao** possui três loggers principais: **DNS, CACHE e INFRA**. Se você prefere uma configuração mais orientada à privacidade, basta mudar o nível do *root* no XML acima para “WARN” — queries e outros eventos esperados não serão registrados. Para troubleshooting, análise ou preferência pessoal, você pode configurar cada log individualmente:

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

## Configuração YML

Este é o único arquivo de configuração da aplicação. Nele você define quais portas serão usadas, quais recursos estarão habilitados, quais *upstreams* serão utilizados, mapeamentos locais e quais blocklists de DNS serão aplicadas. Abaixo está um exemplo completo:

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

A propriedade **server** define as propriedades de alto nível da aplicação.

| Propriedade       | Descrição                                                                                                           |
| ----------------- | ------------------------------------------------------------------------------------------------------------------- |
| **port**          | porta em que a aplicação escutará chamadas UDP e TCP, conforme os padrões de DNS. O padrão para servidores DNS é 53 |
| **udpThreadPool** | quantas *threads* estarão disponíveis para o protocolo UDP, no **server**. O valor padrão é 10                      |
| **tcpThreadPool** | quantas *threads* estarão disponíveis para o protocolo TCP, no **server**. O valor padrão é 3                       |
| **webPort**       | porta onde o dashboard de métricas ficará disponível. O padrão é 8044                                               |

### cache

```yaml
cache:
  enabled: true
  maxCacheEntries: 1000
  rewarm: true
  maxRewarmCount: 5
```

A propriedade **cache** define o comportamento do cache da aplicação. O cache é o principal componente responsável por acelerar as queries de DNS.

| Propriedade         | Descrição                                                                                                                                                                                                                                                                                                                                                        |
| ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **enabled**         | indica se o cache será configurado. É possível definir como *false* para desabilitar o cache — **não recomendado**, pois isso degradará significativamente a performance e quebrará a lógica de DNS Query/TTL. Pode ser útil para troubleshooting. O padrão é **true**                                                                                           |
| **maxCacheEntries** | número máximo de entradas permitidas no cache. 1000 é um bom número para redes domésticas e cabe dentro do uso de memória recomendado (conforme observado na [instalação](installation.md)). Se aumentar este número, lembre-se de aumentar também o limite de memória da JVM.                                                                                   |
| **rewarm**          | habilita o mecanismo de “cache rewarm”: quando uma entrada de cache está perto do fim do seu TTL, uma tentativa de atualização é feita automaticamente. O padrão é **true**                                                                                                                                                                                      |
| **maxRewarmCount**  | quantas vezes o **DNSao** fará *rewarm* da entrada antes de removê-la da memória. Se chegar uma query para um domínio no cache “warm”, essa entrada é promovida para o cache “hot” e o contador de *rewarm* é reiniciado. Isso garante que domínios acessados com frequência permaneçam disponíveis, melhorando a performance da resolução DNS. O padrão é **5** |

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

A propriedade **resolver** define os *upstreams* que serão consultados. Você deve especificar IP, porta e protocolo (“dot” ou “udp”). Estas são as propriedades de alto nível:

| Propriedade     | Descrição                                                                                                                                                                                                                                                                                                                      |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **tlsPoolSize** | tamanho máximo do *pool* de conexões DOT por *upstream*. Usar *pool* melhora a performance, já que o *handshake* TLS é custoso, mas aumentá-lo demais não trará necessariamente mais velocidade — uma única conexão pode servir múltiplas requisições e conexões stale são descartadas pelo *upstream*                     |
| **multiplier**  | oara quantos *upstreams* cada query será enviada. O **DNSao** usa a resposta mais rápida e descarta as demais. Há um *trade-off* entre velocidade e privacidade: quanto mais *upstreams* por requisição, mais servidores verão suas queries. Se privacidade é o principal objetivo, defina *multiplier* como 1 e use *upstreams* DOT. |

Estas são as propriedades internas dentro de **upstreams**:

| Propriedade     | Descrição                                                                                                                                                                                                                                |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **upstreams**   | a lista de *upstreams* que serão consultados                                                                                                                                                                                             |
| **ip**          | IP do servidor *upstream* a ser usado                                                                                                                                                                                                    |
| **port**        | Porta do servidor *upstream* a ser usada. Para UDP, a porta comum é 53; para DOT, a porta padrão é 853                                                                                                                                   |
| **protocol**    | protocolos suportados: **udp** e **dot**                                                                                                                                                                                                 |
| **tlsAuthName** | ao usar o protocolo **dot**, é necessário também definir **tlsAuthName** para validação do servidor remoto. Esse nome é verificado na inicialização e, se a verificação de autoridade falhar, o *upstream* é descartado e não será usado |

Também é possível definir **localMappings**: entradas de DNS que serão resolvidas diretamente pelo **DNSao**.

| Propriedade | Descrição                                                                     |
| ----------- | ----------------------------------------------------------------------------- |
| **domain**  | o domínio a ser mapeado                                                       |
| **ip**      | o IPv4 que será enviado como resposta. Apenas IPv4 está disponível no momento |

Você também pode configurar **blocklists**: são URLs remotas que o **DNSao** fará download e parse para bloquear certos domínios. Qualquer domínio mapeado nessas listas será respondido com o IP **0.0.0.0**, seguindo o padrão de *DNS Sink Hole*. As listas suportadas podem estar no formato *hosts*:

```txt
87.123.55.32 domain.to.be.blocked       # o IP é ignorado, todos os domínios irão para 0.0.0.0
```

ou lista simples:

```txt
domain1.to.be.blocked
domain2.to.be.blocked
domain3.to.be.blocked
```

Um caso de uso comum é usar [StevenBlack](https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts), uma blocklist famosa e atualizada regularmente.

<div style="margin-bottom: 60px;"></div>
