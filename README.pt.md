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


**DNSao** é um encaminhador de DNS multi-upstream escrito em Java — com suporte a cache, DNS-over-TLS (DoT) e um painel integrado para métricas em tempo real.

> O DNSao está atualmente em desenvolvimento ativo

---

## Visão Geral

O DNSao foi projetado para ser um encaminhador de DNS leve, configurável e voltado à privacidade.  

Ele lida com consultas DNS via UDP e TCP, oferece suporte a upstreams criptografados modernos (DoT), mantém um sistema de cache flexível e fornece uma interface web para visualização de métricas.

### Funcionalidades Atuais

- Responde consultas DNS locais via UDP e TCP
- Múltiplos resolvedores DNS upstream (UDP/DoT)
- Sistema de cache com reconhecimento de TTL e revalidação assíncrona
- Registro opcional de consultas (query logging)
- Painel de métricas em tempo real desenvolvido com Bulma + Chart.js
- Suporte a listas de bloqueio e mapeamentos/substituições locais
- Configuração unificada via `application.yml`
- Pronto para uso com systemd e footprint leve

---

Mais informações podem ser encontradas na documentação.

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

**Importante**: a branch main é a que é usada para desenvolvimento atualmente, então não deve ser considerada estável. Se o foco é a estabilidade do build, use a tag "prod"

## Configuração

Todas as opções de configuração do aplicativo são definidas em um único arquivo `application.yml`.

Mais informações detalhadas podem ser encontradas na documentação.

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

Contribuições são bem-vindas!
Abra issues, sugira melhorias ou envie pull requests diretamente no [GitHub](https://github.com/vitallan/dnsao).

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

* [Documentação Oficial](https://github.com/vitallan/dnsao)
* [Repositório no GitHub](https://github.com/vitallan/dnsao)

---

Licenciado sob a Licença MIT © 2025 Allan Vital
