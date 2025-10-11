# Backlog

**DNSao** ainda está em começo de desenvolvimento, mas para deixar registrado quais são as ideias de próximos passos da ferramenta, seguem abaixo:

- [Métricas](#melhoria-nas-metricas)
- [Javalin](#remover-javalin)
- [Resolução Recursiva de DNS](#resolucao-recursiva)
- [DNSSEC](#dnssec)
- [Sair do MkDocs](#sair-do-mkdocs)

## Melhoria nas métricas

O dashboard de métricas de **DNSao** ainda é muito simples e não conta com todas as informações necessárias para uma operação plena unicamente pela interface administrativa. Em cenários com log de queries ligado, seria interessante poder acompanhar as queries diretamente pela interface administrativa. 

Os contadores gerais também perdem a importância conforme o tempo passa, e devem ser substituidos por contadores temporais (últimas 24h, por exemplo). A granularidade das estatísticas também pode melhorar, bem como disponibilizar informações por cliente.

## Remover Javalin

Os servidores UDP e TCP são criados e manipulados diretamente no **DNSao**, mas o servidor web foi criado usando **Javalin**. Os motivos para isso são diversos, mas o mais importante foi para não perder tempo lidando com um servidor web nessa fase do projeto. 

Embora **Javalin** seja um ótimo projeto e bem aderente aos propósitos de um runtime leve, faz mais sentido ter um servidor http interno para lidar com a disponibilização de métricas.

## Resolução Recursiva

Similar ao [Unbound](https://nlnetlabs.nl/projects/unbound/about/){:target="_blank"}, isso tornaria **DNSao** em um resolvedor DNS, e não um encaminhador. Com essa feature, as queries DNS não seriam propagadas para um "upstream": seriam consultadas diretamente nos servidores raiz e resolvidas diretamente por **DNSao**. 

## DNSSEC

Com resolução recursiva e aderindo a validação [DNSSEC](https://en.wikipedia.org/wiki/Domain_Name_System_Security_Extensions){:target="_blank"}, **DNSao** se torna um servidor DNS completo, garantindo segurança nas requisições e validando todas as chaves da cadeia de resolução hierárquica, permitindo ao usuário escolher qual setup DNS mais se encaixa em seu cenário, mas usando **DNSao** para qualquer caminho.

## Sair do MkDocs

Pior ferramenta para criar um site estático.

<div style="margin-bottom: 60px;"></div>
