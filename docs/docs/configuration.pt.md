# Configuração

O **DNSao** requer dois arquivos de configuração:

* [Configuração YML](#configuracao-yml)
* [Logback XML](#logback-xml)

Você pode encontrar exemplos de configuração [no projeto do GitHub]({{sample_conf_url}}), mas abaixo há exemplos e referências.

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
  keep:
    - "url1.com"
    - "url2.com"

misc:
  timeout: 3
  refreshLists: false
  serveExpired: false
  serveExpiredMax: 86400
  dnssec: "simple"

resolver:
  tlsPoolSize: 5
  multiplier: 3
  dnssec: "simple"
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

  allowLists:
    - "https://raw.githubusercontent.com/Allow/hosts/master/hosts"

  blocklists:
    - "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
```

### server

```yaml
server:
  port: 53 
  udpThreadPool: 10
  tcpThreadPool: 3
  httpThreadPool: 10
  webPort: 8044
```

A propriedade **server** define as propriedades de alto nível da aplicação.

| Propriedade       | Descrição                                                                                                           |
| ----------------- | ------------------------------------------------------------------------------------------------------------------- |
| **port**          | porta em que a aplicação escutará chamadas UDP e TCP, conforme os padrões de DNS. O padrão para servidores DNS é 53 |
| **udpThreadPool** | quantas *threads* estarão disponíveis para o protocolo UDP, no **server**. O valor padrão é 10                      |
| **tcpThreadPool** | quantas *threads* estarão disponíveis para o protocolo TCP, no **server**. O valor padrão é 3                       |
| **httpThreadPool** | quantas *threads* estarão disponíveis para o protocolo HTTP, no **server**. O valod padrão é 10 |
| **webPort**       | porta onde o dashboard de métricas ficará disponível. O padrão é 8044                                               |

Para queries http, o endpoint é **http://serverIp:webPort/dns-query**, seguindo os padrões de servidor dns via HTTP. Note que a resposta será em HTTP aberto, não em HTTPS. Essa é uma decisão consciente para evitar o manuseio de certificados TLS, de forma que o usuário possa usar os próprios certificados. Caso https seja desejado, é recomendado usar um proxy reverso que possibilite a comunicação remota a ocorrer via HTTPS (como traeffic ou nginx) e fazer o proxy reverso interno para **DNSao**.

Na interface web **http://serverIp:webPort/** você pode observar as métricas do servidor e buscar queries individualmente. O gráfico contém uma janela das últimas 24 horas dividida em segmentos de 10 minutos.

Os segmentos também são usados para conter os eventos de query buscáveis no endpoint */query*, porém, um máximo de 5000 queries será mantida em um dado segmento. Os totais e contadores terão o número correto, mas as queries não serão buscáveis na tabela de query.

### cache

```yaml
cache:
  enabled: true
  maxCacheEntries: 1000
  rewarm: true
  maxRewarmCount: 5
  keep:
    - "url1.com"
    - "url2.com"
```

A propriedade **cache** define o comportamento do cache da aplicação. O cache é o principal componente responsável por acelerar as queries de DNS.

| Propriedade         | Descrição                                                                                                                                                                                                                                                                                                                                                        |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **enabled**         | indica se o cache será configurado. É possível definir como *false* para desabilitar o cache - **não recomendado**, pois isso degradará significativamente a performance e quebrará a lógica de DNS Query/TTL. Pode ser útil para troubleshooting. O padrão é **true**                                                                                           |
| **maxCacheEntries** | número máximo de entradas permitidas no cache. 1000 é um bom número para redes domésticas e cabe dentro do uso de memória recomendado (conforme observado na [instalação](installation.md)). Se aumentar este número, lembre-se de aumentar também o limite de memória da JVM.                                                                                   |
| **rewarm**          | habilita o mecanismo de “cache rewarm”: quando uma entrada de cache está perto do fim do seu TTL, uma tentativa de atualização é feita automaticamente. O padrão é **true**                                                                                                                                                                                      |
| **maxRewarmCount**  | quantas vezes o **DNSao** fará *rewarm* da entrada antes de removê-la da memória. Se chegar uma query para um domínio no cache “warm”, essa entrada é promovida para o cache “hot” e o contador de *rewarm* é reiniciado. Isso garante que domínios acessados com frequência permaneçam disponíveis, melhorando a performance da resolução DNS. O padrão é **5** |
| **keep**            | uma lista de urls para realizar um precache antes de servidor iniciar e também sempre manter em memória. Essas urls sempre serão mantidas quentes mesmo após atingirem o limite estabelecido em **maxRewarmCount**. O objetivo é manter essas entradas sempre disponíveis em cache                                                                               |
### misc 

```yaml
misc:
  timeout: 3
  refreshLists: false
  serveExpired: false
  serveExpiredMax: 86400
  dnssec: "simple"
```

A propriedade **misc** define mecanismos de funcionamento geral do servidor.

| Property | Description |
|---------|------------|
| **timeout** | timeout global em segundos para as queries upstream |
| **refreshLists** | true/false, default é false. Quando habilitado o servidor irá refazer o download das listas de *allow* e *block* periodicamente para atualizar seus valores. Se muitas listas forem usadas, isso pode causar um aumento no consumo de memória. Aloque mais memória caso deseje habilitar a função |
| **serveExpired** | true/false, default é false. Aderindo a dns rfc8767, quando habilitado, **DNSao** servirá entradas que já expiraram quando nenhuma consulta upstream resultar em uma resposta definitiva (timeout, SERVFAIL ou REFUSED) para maximizar disponibilidade DNS  |
| **serveExpiredMax** | default é 86400 (um dia). Quando **serveExpired** está habilitado, esse é o tempo máximo de segundos que uma entrada do cache local resultará em sucesso antes da entrada ser considerada expirada e removida |
| **dnssec** | Define o funcionamento geral sobre DNSSEC do servidor. Mais detalhes na tabela abaixo. O valor default é **simple** |

#### dnssec

A propriedade **dnssec** define o comportamento de **DNSao** sobre as flags e validação *DNSSEC* , e tem o valor default de **simple**. Os valores válidos são **off, simple e rigid**. **DNSao** não executa a validação crypt para as flags e chaves, mas confia na resposta dos servidores upstream para definir seu comportamento.

| Level | Description |
| ----- | ----------- |
| **off** | a requisição do cliente será enviada para os upstreams sem manipulação das flags DNSSEC. **DNSao** não irá validar se a resposta tem validação DNSSEC antes de retornar ao cliente |
| **simple** | a requisição do cliente será enviada para os upstreams adicionando a flag DNSSEC. **DNSao** irá responder ao cliente transmitindo as flags DNSSEC respondidas pelo upstream, validadas ou não, mas não bloqueará as respostas não validadas. A query também terá um *padding* para o próximo tamanho multiplo de 128 bytes para permitir ofuscação extra, seguindo dns rfc7830 |
| **rigid** | a requisição do cliente será enviada para os upstreams adicionando a flag DNSSEC. **DNSao** só responderá ao cliente se a resposta possuir a flag DNSSEC disponível e válida. Caso contrário, será respondido um **SERVFAIL**. A query também terá *pading*, mas para o tamanho máximo permitido no pacote que será enviado ao upstream para máxima ofuscação. *Atenção: isso irá bloquear vários domínios, visto que muitos deles não possuem DNSSEC habilitado* |

Independente da resposta do upstream, **DNSao** não habilita a flag *AD* na resposta, pois não executa as validações dos hashs internamente (necessário pela dns rfc4035 3.2.3)

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

A propriedade **resolver** define os *upstreams* que serão consultados. Você deve especificar as configs respectivas para cada protocolo ("dot", "doh" ou "udp"). Estas são as propriedades de alto nível:

| Propriedade     | Descrição                                                                                                                                                                                                                                                                                                                      |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **tlsPoolSize** | tamanho máximo do *pool* de conexões DOT por *upstream*. Usar *pool* melhora a performance, já que o *handshake* TLS é custoso, mas aumentá-lo demais não trará necessariamente mais velocidade — uma única conexão pode servir múltiplas requisições e conexões stale são descartadas pelo *upstream*                     |
| **multiplier**  | para quantos *upstreams* cada query será enviada. O **DNSao** usa a resposta mais rápida e descarta as demais. Há um *trade-off* entre velocidade e privacidade: quanto mais *upstreams* por requisição, mais servidores verão suas queries. Se privacidade é o principal objetivo, defina *multiplier* como 1 e use *upstreams* DOT ou DOH. |

Estas são as propriedades internas dentro de **upstreams**:

| Propriedade     | Descrição                                                                                                                                                                                                                                |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **upstreams**   | a lista de *upstreams* que serão consultados                                                                                                                                                                                             |
| **ip**          | IP do servidor *upstream* a ser usado                                                                                                                                                                                                    |
| **port**        | Porta do servidor *upstream* a ser usada. Para UDP, a porta comum é 53; para DOT, a porta padrão é 853                                                                                                                                   |
| **protocol**    | protocolos suportados: **udp** e **dot**                                                                                                                                                                                                 |
| **tlsAuthName** | ao usar o protocolo **dot**, é necessário também definir **tlsAuthName** para validação do servidor remoto. Esse nome é verificado na inicialização e, se a verificação de autoridade falhar, o *upstream* é descartado e não será usado |
| **host** | ao usar o protocolo **doh**, é necessário definir a propriedade **host**, para onde as queries serão enviadas via https |
| **path** | ao usar o protocolo **doh**, é necessário definir a propriedade **path**, que será incluida ao final de **host**. Seu valor default é **/dns-query**  |

Exemplos das configurações possíveis podem ser encontradas na pasta [config-samples no github](https://github.com/vitallan/dnsao/tree/main/config-samples). Diferentes tipos de upstream podem ser usados ao mesmo tempo contanto que as propriedades necessárias para cada protocolo estejam presentes.

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

Um caso de uso comum é usar [StevenBlack](https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts), uma blocklist famosa e atualizada regularmente. Os links são baixados quando **DNSao** inicia e são atualizados a cada hora.

Também é possível configurar **allowLists**: possuem o mesmo comportamento de download e releitura que as **blockLists**, mas servem como um indicativo de domínios que você **não** quer bloquear mesmo se estiverem presentes nas **blockLists**. É útil para evitar falsos-positivo, e cenários onde algum item de alguma blocklist que é de seu interesse. 

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


<div style="margin-bottom: 60px;"></div>
