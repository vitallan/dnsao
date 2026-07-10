# Configuração

O **DNSao** requer um único arquivo de configuração:

* [Configuração YML](#configuracao-yml)

Você pode encontrar exemplos de configuração [no projeto do GitHub]({{sample_conf_url}}), mas abaixo há exemplos e referências.

## Configuração YML

Este é o único arquivo de configuração da aplicação. Nele você define quais portas serão usadas, quais recursos estarão habilitados, quais *upstreams* serão utilizados, mapeamentos locais e quais blocklists de DNS serão aplicadas. Abaixo está um exemplo completo:

```yaml
server:
  port: 53 
  udpThreadPool: 10
  tcpThreadPool: 3
  statsDbPath: "/etc/dnsao/stats.db"
  webPort: 8044
  authPass: ""

cache:
  enabled: true
  maxCacheEntries: 1000
  rewarm: true
  maxRewarmCount: 5
  alwaysRewarmTopEntries: 0
  rewarmWorkerPoolSize: 3
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

### server

```yaml
server:
  port: 53 
  udpThreadPool: 10
  tcpThreadPool: 3
  httpThreadPool: 10
  statsDbPath: "/etc/dnsao/stats.db"
  webPort: 8044
  authPass: ""
```

A propriedade **server** define as propriedades de alto nível da aplicação.

| Propriedade       | Descrição                                                                                                           |
| ----------------- | ------------------------------------------------------------------------------------------------------------------- |
| **port**          | porta em que a aplicação escutará chamadas UDP e TCP, conforme os padrões de DNS. O padrão para servidores DNS é 53 |
| **udpThreadPool** | quantas *threads* estarão disponíveis para o protocolo UDP, no **server**. O valor padrão é 10                      |
| **tcpThreadPool** | quantas *threads* estarão disponíveis para o protocolo TCP, no **server**. O valor padrão é 3                       |
| **httpThreadPool** | quantas *threads* estarão disponíveis para o protocolo HTTP, no **server**. O valod padrão é 10 |
| **webPort**       | porta onde o dashboard de métricas ficará disponível. O padrão é 8044                                               |
| **statsDbPath**   | caminho para um arquivo SQLite de métricas e histórico de queries. Quando não definido/vazio, o **DNSao** usa `{tmpdir}/dnsao.db` (o diretório temporário do SO). O diretório pai precisa existir e ter permissão de escrita (o DNSao não cria diretórios) |
| **useMemoryStorage** | true/false, padrão é false. Quando true, força o armazenamento das métricas em memória em vez de SQLite. Útil para ambientes efêmeros ou quando você quer evitar escrita em disco |
| **authPass** | senha opcional para proteger o dashboard web e as APIs JSON. Quando vazia (padrão), o dashboard fica aberto para qualquer um. Quando definida, os usuários devem informar esta senha em uma página de login antes de acessar o dashboard. A senha é enviada em texto puro via HTTP — use apenas em redes confiáveis ou atrás de um proxy reverso com HTTPS. O padrão é **""** (sem autenticação) |

Quando **authPass** está definida, acessar o dashboard em **http://serverIp:webPort/** redirecionará para uma página de login. Os endpoints de API (`/stats`, `/queries`, `/api/*`) também exigem autenticação e retornam **401 Unauthorized** quando não autenticados. O endpoint DNS-over-HTTPS (`/dns-query`) é sempre público, independentemente desta configuração.

Para queries http, o endpoint é **http://serverIp:webPort/dns-query**, seguindo os padrões de servidor dns via HTTP. Note que a resposta será em HTTP aberto, não em HTTPS. Essa é uma decisão consciente para evitar o manuseio de certificados TLS, de forma que o usuário possa usar os próprios certificados. Caso https seja desejado, é recomendado usar um proxy reverso que possibilite a comunicação remota a ocorrer via HTTPS (como traeffic ou nginx) e fazer o proxy reverso interno para **DNSao**.

Na interface web **http://serverIp:webPort/** você pode observar as métricas do servidor e buscar queries individualmente. O gráfico contém uma janela das últimas 24 horas dividida em segmentos de 10 minutos.

O **DNSao** persiste queries e métricas em um arquivo SQLite local por padrão. Os segmentos contêm eventos de query buscáveis no endpoint */query* — com persistência em disco não há limite de eventos, diferente do modo legado em memória que limitava em 5.000 eventos por segmento. As escritas são agrupadas a cada 500ms; se o banco não acompanhar, o DNSao descartará os eventos mais antigos do buffer para evitar crescimento ilimitado de memória.

### cache

```yaml
cache:
  enabled: true
  maxCacheEntries: 1000
  rewarm: true
  maxRewarmCount: 5
  alwaysRewarmTopEntries: 0
  rewarmWorkerPoolSize: 3
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
| **alwaysRewarmTopEntries** | o número de entradas mais frequentemente acessadas que serão sempre reaquecidas, ignorando o **maxRewarmCount**. Diferente de **keep**, estas não são pré-carregadas — elas conquistam seu lugar através de consultas reais dos clientes. As N entradas (não-keep) mais recentes por acesso são promovidas automaticamente. Padrão é **0** (desabilitado). Limitado a **maxCacheEntries** |
| **rewarmWorkerPoolSize** | o número de threads no pool de workers de rewarm. Um pool maior permite que mais entradas sejam reaquecidas em paralelo, reduzindo a chance de entradas expiradas sob alta carga de consultas. O padrão é **3**. O mínimo é **1** |
| **keep**            | uma lista de urls para realizar um precache antes de servidor iniciar e também sempre manter em memória. Essas urls sempre serão mantidas quentes mesmo após atingirem o limite estabelecido em **maxRewarmCount**. O objetivo é manter essas entradas sempre disponíveis em cache                                                                               |

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

A propriedade **misc** define mecanismos de funcionamento geral do servidor.

| Property | Description |
|---------|------------|
| **timeout** | timeout global em segundos para as queries upstream |
| **queryLog** | true/false, padrão é true. Controla se eventos individuais de consulta são registrados e armazenados. Quando false: detalhes da consulta são suprimidos do log DNS, campos sensíveis (domínio, cliente, tipo, resposta) são removidos dos eventos notificados aos assinantes, os contadores do dashboard continuam funcionando, mas a tabela de histórico de consultas fica vazia |
| **refreshLists** | true/false, default é false. Quando habilitado o servidor irá refazer o download das listas de *allow* e *block* periodicamente para atualizar seus valores. Se muitas listas forem usadas, isso pode causar um aumento no consumo de memória. Aloque mais memória caso deseje habilitar a função |
| **blockingEnabled** | true/false, padrão é true. Quando definido como false, a unidade de bloqueio é completamente desabilitada e todas as consultas passam sem verificação de listas. Útil para desabilitar temporariamente o filtro sem remover as configurações de lista |
| **serveExpired** | true/false, default é false. Aderindo a dns rfc8767, quando habilitado, **DNSao** servirá entradas que já expiraram quando nenhuma consulta upstream resultar em uma resposta definitiva (timeout, SERVFAIL ou REFUSED) para maximizar disponibilidade DNS  |
| **serveExpiredMax** | default é 86400 (um dia). Quando **serveExpired** está habilitado, esse é o tempo máximo de segundos que uma entrada do cache local resultará em sucesso antes da entrada ser considerada expirada e removida |
| **dnssec** | Define o funcionamento geral sobre DNSSEC do servidor. Mais detalhes na tabela abaixo. O valor default é **simple** |

#### dnssec

A propriedade **dnssec** define o comportamento de **DNSao** sobre as flags e validação *DNSSEC* , e tem o valor default de **simple**. Os valores válidos são **off, simple e rigid**. **DNSao** não executa a validação crypt para as flags e chaves, mas confia na resposta dos servidores upstream para definir seu comportamento.

| Level | Description |
| ----- | ----------- |
| **off** | a requisição do cliente será enviada para os upstreams sem manipulação das flags DNSSEC. **DNSao** não irá validar se a resposta tem validação DNSSEC antes de retornar ao cliente |
| **simple** | a requisição do cliente será enviada para os upstreams adicionando a flag DNSSEC. **DNSao** irá responder ao cliente mas não bloqueará as respostas não validadas. A query também terá um *padding* para o próximo tamanho multiplo de 128 bytes para permitir ofuscação extra, seguindo dns rfc7830 |
| **rigid** | a requisição do cliente será enviada para os upstreams adicionando a flag DNSSEC. **DNSao** só responderá ao cliente se a resposta possuir a flag DNSSEC disponível e válida. Caso contrário, será respondido um **SERVFAIL**. A query também terá *pading*, mas para o tamanho máximo permitido no pacote que será enviado ao upstream para máxima ofuscação. *Atenção: isso irá bloquear vários domínios, visto que muitos deles não possuem DNSSEC habilitado* |

Independente da resposta do upstream, **DNSao** não habilita a flag *AD* na resposta, pois não executa as validações dos hashs internamente (necessário pela dns rfc4035 3.2.3)

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

A propriedade **resolver** define os *upstreams* que serão consultados. Você deve especificar as configs respectivas para cada protocolo ("dot", "doh" ou "udp"). Estas são as propriedades de alto nível:

| Propriedade     | Descrição                                                                                                                                                                                                                                                                                                                      |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **tlsPoolSize** | tamanho máximo do *pool* de conexões DOT por *upstream*. Usar *pool* melhora a performance, já que o *handshake* TLS é custoso, mas aumentá-lo demais não trará necessariamente mais velocidade — uma única conexão pode servir múltiplas requisições e conexões stale são descartadas pelo *upstream*                     |
| **multiplier**  | para quantos *upstreams* cada query será enviada. O **DNSao** usa a resposta mais rápida e descarta as demais. Há um *trade-off* entre velocidade e privacidade: quanto mais *upstreams* por requisição, mais servidores verão suas queries. Se privacidade é o principal objetivo, defina *multiplier* como 1 e use *upstreams* DOT ou DOH. |
| **upstreamThreadPoolSize** | tamanho do pool compartilhado de threads usado para executar chamadas aos upstreams. O default é **64** |
| **upstreamQueueSize** | tamanho da fila limitada para tarefas upstream. O default é **640** (64 * 10) |

#### Internals da Execução Upstream

As chamadas aos upstreams são executadas via um `ThreadPoolExecutor` compartilhado (`UpstreamThreadPoolExecutor`) configurado com um número fixo de threads (`resolver.upstreamThreadPoolSize`) e uma fila limitada (`resolver.upstreamQueueSize`).

Quando o pool e a fila estão saturados, o **DNSao** usa `CallerRunsPolicy` como mecanismo de *backpressure*: a thread chamadora executa a chamada upstream inline, desacelerando o processamento de novas queries ao invés de criar mais threads ou permitir crescimento ilimitado de memória.

O `resolver.multiplier` controla quantas tarefas upstream uma única query pode agendar; sob saturação, as tarefas podem ficar na fila ou serem executadas inline por causa do *backpressure*.

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

### lists

```yaml
lists:
  blockLists:
    steven: "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
    bets: "https://raw.githubusercontent.com/zangadoprojets/pi-hole-blocklist/refs/heads/main/Bets.txt"
  allowLists:
    allowList1: "http://url.of.allow.lists.com"
```

Essa config é opcional, mas pode ser usada para configurar listas de domínios a serem bloqueados. O parâmetro esperado é a url onde o servidor pode fazer o download das listas. Qualquer domínio mapeado nas listas de **blockLists** será respondido com o IP **0.0.0.0**, seguindo o padrão de *DNS Sink Hole*.

As listas podem estar no formato *hosts*:

```txt
87.123.55.32 domain.to.be.blocked       # o IP é ignorado, todos os domínios irão para 0.0.0.0
```

ou lista simples:

```txt
domain1.to.be.blocked
domain2.to.be.blocked
domain3.to.be.blocked
```

Um caso de uso comum é usar [StevenBlack](https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts), uma blocklist famosa e atualizada regularmente. Os links são baixados quando **DNSao** inicia e são atualizados a cada 8 horas, caso a config **refreshLists** estiver habilitada.

Também é possível configurar **allowLists**: possuem o mesmo comportamento de download e releitura que as **blockLists**, mas servem como um indicativo de domínios que você **não** quer bloquear mesmo se estiverem presentes nas **blockLists**. É útil para evitar falsos-positivo, e cenários onde algum item de alguma blocklist que é de seu interesse. 

Tanto as **blockLists** e **allowLists** exigem um nome para cada lista (no exemplo acima, os nomes são "steven", "bets" e "allowList1").

Nomes precisam ser únicos entre as blockLists e allowLists para funcionarem efetivamente.

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

Essa config é opcional, mas pode ser usada para seletivamente bloquear ou permitir domínios baseado no cliente. Dessa forma, domínios específicos podem ser bloqueados para alguns devices, mas não para toda a rede.

No exemplo acima, o grupo nomeado **group1** terá dois membros (os ips terminando em 55 e 40), e só bloqueará os domínios da lista em "steven", e permitirá os domínios da lista "allowList1".                                  

O grupo **group2** terá um único membro e não bloqueará ou permitirá nenhuma lista específica.

Todo os clientes não definidos individualmente em um grupo entrarão no grupo **MAIN**.

O grupo **MAIN** pode opcionalmente ser definido manualmente no YAML. Quando explicitamente definido, seus `members`, `allows` e `blocks` são preservados como estão. Quando `main` está **ausente** da configuração, o **DNSao** o cria automaticamente como um grupo genérico usando todas as blockLists e allowLists definidas na seção `lists`.

### listeners

```yaml
listeners:
  http:
    - "http://host:port/listener"
```

Config opcional que pode ser usada para informar serviços http externos para receber as queries DNS via POST. Após cada requisição dns ser recebida e processada, **DNSao** irá executar um POST para cada url da lista com o body no formato abaixo:

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
O campo **source** informa qual upstream respondeu a query, caso ela tenha sido resolvida por um upstream, caso contrário, virá nulo.

## Logging

O logging é configurado através da seção `log` no `application.yml`. **DNSao** usa `java.util.logging` (JUL) com três loggers nomeados: **DNS**, **CACHE** e **INFRA**.

### Configuração

```yaml
log:
  rootLevel: WARN
  dns: DEBUG
  cache: DEBUG
  infra: DEBUG
  # Log opcional em arquivo (se ausente, imprime apenas no console):
  file:
    path: "/var/log/dnsao/dnsao-%g.log"
    maxSize: 10485760
    maxFiles: 5
```

### Loggers

- **DNS** — resolução de consultas DNS, ciclo de vida do servidor
- **CACHE** — operações de cache (acertos, erros, rewarm)
- **INFRA** — eventos de infraestrutura (carregamento de listas, conexões upstream)

### Níveis

| Valor config | Nível JUL | Descrição |
|---|---|---|
| `TRACE` | FINER | Detalhes de diagnóstico |
| `DEBUG` | FINE | Informação de depuração |
| `INFO` | INFO | Mensagens operacionais normais |
| `WARN` | WARNING | Situações inesperadas mas recuperáveis |
| `ERROR` | SEVERE | Falhas graves |
| `OFF` | OFF | Suprime todas as mensagens |

### Log em arquivo

Quando `file.path` é definido, o DNSao escreve logs de forma assíncrona em arquivos rotativos. O padrão suporta `%g` como índice de geração de arquivo.

### Formato de saída no console

```
[HH:mm:ss.SSS] LEVEL [thread] [LOGGER] message
```

<div style="margin-bottom: 60px;"></div>
