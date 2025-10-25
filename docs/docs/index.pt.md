---
title: Início
---

# DNSao

<p align="center">
  <img src="/dnsao/assets/logo.svg" alt="DNSao Logo" width="200">
</p>
<p align="center">
<strong>DNSao</strong> é um encaminhador DNS multi-upstream de alta performance
</p>

---
- **DNS Sinkhole** : bloqueia domínios com base em listas, agindo como um [DNS sinkhole](https://en.wikipedia.org/wiki/DNS_sinkhole){:target="_blank"}, impedindo acesso a conteúdos indesejados, publicidade invasiva e dificultando o rastreamento por trackers
- **Suporte a DNS over TLS e DNS over HTTPS**: realiza consultas a servidores upstream usando UDP tradicional, [DoT](https://en.wikipedia.org/wiki/DNS_over_TLS){:target="_blank"} e [DoH](https://en.wikipedia.org/wiki/DNS_over_HTTPS){:target="_blank"} garantindo maior privacidade nas resoluções
- **Consulta paralela a múltiplos upstreams**: pode ser configurado para enviar a mesma consulta para vários servidores upstream em paralelo e retorna a resposta mais rápida, reduzindo a latência de navegação
- **Protocolos multiplos como servidor**: responde queries dns em UDP, TCP e HTTP, possibilitando diferentes setups de clientes
- **Cache de Alta Performance**: armazena respostas respeitando o TTL original, inclui cache negativo e mecanismo de rewarm (pré-aquecimento) para manter entradas frequentemente usadas sempre disponíveis
- **Política configurável de DNSSEC**: solicita dados DNSSEC aos upstreams e aplica políticas (off/simple/rigid) confiando na validação do upstream via bit AD
- **Configuração em YAML**: toda a configuração do servidor é centralizada em um único arquivo .yaml, fácil de versionar e replicar entre múltiplas instâncias para alta disponibilidade
- **Mapeamento de DNS local**:  permite definir resoluções locais de domínios para IPs específicos — ideal para homelabs, autohospedagem e redes internas 
- **Dashboard de métricas**: disponibiliza um dashboard de métricas para acompanhamento de operação e performance 
- **Baixo consumo de recursos**: roda confortavelmente com 256 MB de RAM, mesmo em hardware antigo ou dispositivos compactos
- **Grátis e de Código Aberto**: software livre, mantido abertamente no GitHub, permitindo auditoria e uso irrestrito
- **Runtime stateless**: não depende de bancos de dados e outros sistemas periféricos, possibilitando rápido cold start
- **Baixo número de dependências**: apenas 5: dnsjava, logback, javalin, minimal-json e snakeyaml

---

## Objetivo

Outros softwares de DNS atuam como [DNS Sinkhole](https://docs.pi-hole.net/){:target="_blank"}, ou suportam multiplos [upstreams com DOT](https://github.com/getdnsapi/stubby){:target="_blank"}, ou fazem a [resolução recursiva de DNS](https://nlnetlabs.nl/projects/unbound/about/){:target="_blank"}, mas é sempre necessário combinar soluções para poder atingir um nível satisfatório de privacidade ou velocidade. O **DNSao** existe para ser a única ferramenta de DNS necessária em sua rede. 

---

Todo o código está disponível no [Github](https://github.com/vitallan/dnsao) do projeto, incluindo últimas releases e documentação para desenvolvimento.

[Aprenda a instalar e usar!](installation.pt.md){ .md-button .md-button--primary }
{: style="text-align:center" }

## Screenshots

### Sumário de queries do dia 

<p align="center">
  <img src="/dnsao/assets/screenshot-summary.png" alt="summary">
</p>

### Gráfico com as queries por horário

<p align="center">
  <img src="/dnsao/assets/screenshot-timeline.png" alt="timeline graph">
</p>

### Distribuição de upstreams

<p align="center">
  <img src="/dnsao/assets/screenshot-upstream-distribution.png" alt="upstream distribution">
</p>

[Aprenda a instalar e usar!](installation.pt.md){ .md-button .md-button--primary }
{: style="text-align:center" }

## Benchmarks

Benchmarks de DNS podem ser um pouco injustos, porque depois da rajada inicial de chamadas e de todos os domínios estarem cacheados, é só um exercício de quão rápido o processador pode pegar algo da memória e empacotar corretamente o retorno, mas algumas pessoas podem achar necessário, então aqui seguem dois exemplos de testes usando **dnsperf** com uma lista de 250 domínios.

Primeiro rodando em um LXC usando um core de um processador recente (i5-12400) onde acessando 10000 queries por segundo resulta em 100% de sucesso:

```
avital@texugo:~/temp$ dnsperf -s dnsao1.intranet -d domains.txt -l 1200 -Q 10000
DNS Performance Testing Tool
Version 2.14.0

[Status] Command line: dnsperf -s dnsao1.intranet -d domains.txt -l 1200 -Q 10000
[Status] Sending queries (to 192.168.68.128:53)
[Status] Started at: Sat Oct 25 10:30:23 2025
[Status] Stopping after 1200.000000 seconds
[Status] Testing complete (time limit)

Statistics:

  Queries sent:         11997016
  Queries completed:    11997016 (100.00%)
  Queries lost:         0 (0.00%)

  Response codes:       NOERROR 10993096 (91.63%), NXDOMAIN 1003920 (8.37%)
  Average packet size:  request 30, response 267
  Run time (s):         1200.000091
  Queries per second:   9997.512575

  Average Latency (s):  0.000414 (min 0.000093, max 0.503841)
  Latency StdDev (s):   0.001498
```

E os dois abaixo usando um raspberry pi 3, com a mesma lista de domínios. Batendo na marca de 100 queries por segundo, ele começa a mostrar alguma limitação:

```
avital@texugo:~/temp$ dnsperf -s dnsao2.intranet -d domains.txt -l 1200 -Q 50
DNS Performance Testing Tool
Version 2.14.0

[Status] Command line: dnsperf -s dnsao2.intranet -d domains.txt -l 1200 -Q 50
[Status] Sending queries (to 192.168.15.50:53)
[Status] Started at: Sat Oct 25 12:28:45 2025
[Status] Stopping after 1200.000000 seconds
[Status] Testing complete (time limit)

Statistics:

  Queries sent:         60000
  Queries completed:    60000 (100.00%)
  Queries lost:         0 (0.00%)

  Response codes:       NOERROR 51968 (86.61%), NXDOMAIN 8032 (13.39%)
  Average packet size:  request 30, response 297
  Run time (s):         1200.000098
  Queries per second:   49.999996

  Average Latency (s):  0.004377 (min 0.001777, max 0.258354)
  Latency StdDev (s):   0.004117
  

avital@texugo:~/temp$ dnsperf -s dnsao2.intranet -d domains.txt -l 1200 -Q 100
DNS Performance Testing Tool
Version 2.14.0

[Status] Command line: dnsperf -s dnsao2.intranet -d domains.txt -l 1200 -Q 100
[Status] Sending queries (to 192.168.15.50:53)
[Status] Started at: Sat Oct 25 12:54:28 2025
[Status] Stopping after 1200.000000 seconds
Warning: received a response with an unexpected (maybe timed out) id: 24603
[Timeout] Query timed out: msg id 24602
[Timeout] Query timed out: msg id 11636
[Status] Testing complete (time limit)

Statistics:

  Queries sent:         120000
  Queries completed:    119998 (100.00%)
  Queries lost:         2 (0.00%)

  Response codes:       NOERROR 103934 (86.61%), NXDOMAIN 16064 (13.39%)
  Average packet size:  request 30, response 276
  Run time (s):         1200.000090
  Queries per second:   99.998326

  Average Latency (s):  0.004140 (min 0.001823, max 0.184822)
  Latency StdDev (s):   0.002594
```

[Aprenda a instalar e usar!](installation.pt.md){ .md-button .md-button--primary }
{: style="text-align:center" }

<div style="margin-bottom: 60px;"></div>
