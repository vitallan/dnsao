# Backlog

Esta página registra os próximos passos e pendências maiores do projeto.

- [Javalin](#remover-javalin)
- [Modo Recursivo](#modo-recursivo)
- [Sair do MkDocs](#sair-do-mkdocs)

Esses são objetivos de longo prazo e itens de backlog.

## Remover Javalin

Os servidores UDP e TCP são criados e manipulados diretamente no **DNSao**, mas o servidor web foi criado usando **Javalin**. Os motivos para isso são diversos, mas o mais importante foi para não perder tempo lidando com um servidor web nessa fase do projeto. 

Embora **Javalin** seja um ótimo projeto e bem aderente aos propósitos de um runtime leve, faz mais sentido ter um servidor http interno para lidar com a disponibilização de métricas.

## Modo Recursivo

O modo recursivo já está disponível no **DNSao** e hoje já é utilizável, especialmente em cenários de homelab. Os itens abaixo registram o que ainda falta para deixá-lo mais completo.

### Cache negativo

Respostas NXDOMAIN e NODATA ainda precisam de um tratamento de cache mais completo, incluindo TTL negativo derivado de SOA.

### Suporte a DNAME

O modo recursivo já suporta CNAME, mas o comportamento de DNAME ainda precisa ser implementado.

### Regras mais fortes de aceitação de referral e answer

O modo recursivo já trata glue, referrals sem glue e regras de bailiwick mais rígidas, mas ainda precisa de regras mais completas para aceitar referrals e answers.

### Melhor tratamento para nameservers quebrados ou lame

O modo recursivo ainda precisa de um tratamento melhor para nameservers lame, vazios ou defeituosos, incluindo comportamento de retry e supressão temporária.

## Sair do MkDocs

Pior ferramenta para criar um site estático.

<div style="margin-bottom: 60px;"></div>
