---
title: Início
---

# DNSao

<p align="center">
  <img src="assets/logo.svg" alt="DNSao Logo" width="200">
</p>
<p align="center">
<strong>DNSao</strong> é um encaminhador DNS multi-upstream de alta performance
</p>

---
- **DNS Sinkhole** : bloqueia domínios com base em listas, agindo como um [DNS sinkhole](https://en.wikipedia.org/wiki/DNS_sinkhole){:target="_blank"}, impedindo acesso a conteúdos indesejados, publicidade invasiva e dificultando o rastreamento por trackers
- **Suporte a DNS over TLS**: realiza consultas a servidores upstream usando UDP tradicional ou [DoT](https://en.wikipedia.org/wiki/DNS_over_TLS){:target="_blank"}, garantindo maior privacidade nas resoluções
- **Consulta paralela a múltiplos upstreams**: envia a mesma consulta para vários servidores upstream em paralelo e retorna a resposta mais rápida, reduzindo a latência de navegação
- **Cache de Alta Performance**: armazena respostas respeitando o TTL original, inclui cache negativo e mecanismo de rewarm (pré-aquecimento) para manter entradas frequentemente usadas sempre disponíveis
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

<div style="margin-bottom: 60px;"></div>
