[![Deploy with Docker](https://img.shields.io/badge/Deploy%20with-Docker-2496ED?logo=docker&logoColor=white)](https://vitallan.github.io/dnsao/pt/installation/#instalacao-via-docker)
[![Deploy with systemd](https://img.shields.io/badge/Deploy%20with-systemd-000000?logo=systemd&logoColor=white)](https://vitallan.github.io/dnsao/pt/installation/#instalacao-por-script)

[![Tests](https://github.com/vitallan/dnsao/actions/workflows/ci.yml/badge.svg)](https://github.com/vitallan/dnsao/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/vitallan/dnsao)](https://github.com/vitallan/dnsao/releases)

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


**DNSao** é um encaminhador de DNS multi-upstream escrito em Java — com suporte a cache, DNS-over-TLS (DoT), DNS-over-HTTPS (DoH) e um painel integrado para métricas em tempo real.

> O DNSao está atualmente em desenvolvimento ativo

---

## Visão Geral

O DNSao foi projetado para ser um encaminhador de DNS leve, configurável e voltado à privacidade.  

Ele lida com consultas DNS via UDP, TCP e HTTP, oferece suporte a upstreams criptografados modernos (DoT/DoH), mantém um sistema de cache robusto e fornece uma interface web para visualização de métricas.

### Funcionalidades Atuais

- Responde consultas DNS locais via UDP, TCP e HTTP
- Múltiplos resolvedores DNS upstream (UDP/DoT/DoH)
- Sistema de cache com reconhecimento de TTL e revalidação assíncrona
- DNSSEC aware com políticas configuráveis
- Registro opcional de consultas (query logging)
- Painel de métricas em tempo real desenvolvido com Bulma + Chart.js
- Suporte a listas de bloqueio e mapeamentos/substituições locais
- Configuração unificada via `application.yml`
- Pronto para uso com systemd e footprint leve

---

Mais informações podem ser encontradas na [documentação](https://vitallan.github.io/dnsao/pt/).

## Compilando a Partir do Código-Fonte

Você pode compilar o DNSao usando **Maven** e **Java 17 ou superior**.

```bash
git clone https://github.com/vitallan/dnsao.git
cd dnsao
mvn clean package -DskipTests
```

O jar resultante ficará localizado em:

```
target/dnsao-<versão>.jar
```

Para executá-lo manualmente:

```bash
java -jar target/dnsao-<versão>.jar -Dconfig=/etc/dnsao/application.yml -Dlogback.configurationFile=/etc/dnsao/logback.xml
```

ou, usando o próprio maven:

```bash
mvn exec:java
```

---

A branch main deve ser considerada estável.

Releases novas e novas imagens docker são geradas a partir do commit/push de uma nova tag no formato v0.0.0. 

## Configuração

Todas as opções de configuração do aplicativo são definidas em um único arquivo `application.yml`.

Mais informações detalhadas podem ser encontradas na [documentação](https://vitallan.github.io/dnsao/pt/configuration).

---

## Painel de Métricas

O DNSao fornece um **painel de métricas integrado** que exibe:

* Volume de consultas
* Taxa de acertos e falhas de cache
* Tempo médio de resposta
* Distribuição por upstream

Acesse o painel através da porta administrativa configurada no seu `application.yml` (ex.: `http://localhost:8044`).

---

## Contribuindo

Contribuições são bem-vindas, mas o foco atual deve ser em reporte de bugs, sugestões de melhorias e feedbacks em geral. **DNSao** está com seu funcionamento estável, mas o código ainda está em criação e muitas das interfaces internas pode mudar sem avisos anteriores.

Issues são a forma recomendada de contato sobre o projeto [GitHub](https://github.com/vitallan/dnsao/issues).

---

## Créditos

O DNSao utiliza bibliotecas open-source, incluindo:

* [dnsjava](https://github.com/dnsjava/dnsjava) — implementação do protocolo DNS
* [Javalin](https://javalin.io) — servidor web leve
* [SnakeYAML](https://bitbucket.org/asomov/snakeyaml) — manipulação de arquivos YAML
* [Minimal-json](https://github.com/ralfstx/minimal-json) — manipulação de JSON
* [Logback](https://logback.qos.ch) — framework de logging

---

## Links

* [Documentação Oficial](https://vitallan.github.io/dnsao/pt)
* [Meu site](https://allanvital.com/pt-br/)

---

Licenciado sob a MIT © 2025 Allan Vital

