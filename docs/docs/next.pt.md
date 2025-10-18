# Backlog

Para deixar registrado quais são as ideias de próximos passos da ferramenta, seguem abaixo:

- [Javalin](#remover-javalin)
- [Resolução Recursiva de DNS](#resolucao-recursiva)
- [Sair do MkDocs](#sair-do-mkdocs)

Esse são objetivos de longo prazo. No momento o foco é no funcionamento via upstreams.

## Remover Javalin

Os servidores UDP e TCP são criados e manipulados diretamente no **DNSao**, mas o servidor web foi criado usando **Javalin**. Os motivos para isso são diversos, mas o mais importante foi para não perder tempo lidando com um servidor web nessa fase do projeto. 

Embora **Javalin** seja um ótimo projeto e bem aderente aos propósitos de um runtime leve, faz mais sentido ter um servidor http interno para lidar com a disponibilização de métricas.

## Resolução Recursiva

Similar ao [Unbound](https://nlnetlabs.nl/projects/unbound/about/){:target="_blank"}, isso tornaria **DNSao** em um resolvedor DNS, e não um encaminhador. Com essa feature, as queries DNS não seriam propagadas para um "upstream": seriam consultadas diretamente nos servidores raiz e resolvidas diretamente por **DNSao**. 

## Sair do MkDocs

Pior ferramenta para criar um site estático.

<div style="margin-bottom: 60px;"></div>
