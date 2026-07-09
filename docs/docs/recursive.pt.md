# Modo Recursivo

O **DNSao** suporta resolução recursiva diretamente a partir de *root hints*. Esse modo está disponível e é utilizável hoje, mas ainda não deve ser tratado como um resolvedor recursivo completo para todos os cenários da Internet.

## O que é

No modo recursivo, o **DNSao** não depende apenas de resolvers upstream externos. Em vez disso, ele consegue resolver nomes iterativamente, começando pelos *root hints*, seguindo delegações, resolvendo endereços de nameservers quando não há *glue* e cacheando os passos intermediários da resolução.

## Habilitando

O modo recursivo é ativado ao **omitir o bloco `resolver.upstreams`** do seu `application.yml`. Quando nenhum upstream é configurado, o DNSao inicia automaticamente no modo recursivo.

Configuração recursiva mínima:

```yaml
server:
  port: 53
  webPort: 8044

cache:
  enabled: true
  maxCacheEntries: 1000

misc:
  timeout: 3
  dnssec: "simple"
# Sem o bloco resolver.upstreams — isso ativa o modo recursivo
```

Você pode opcionalmente definir `resolver.rootHintsUrl` para sobrescrever a fonte padrão de root hints (`https://www.internic.net/domain/named.root`).

### Validando que o modo recursivo está ativo

Na inicialização, procure esta linha no log do servidor:

```
Mode: recursive (no upstreams configured)
```

Um aviso também será exibido para lembrar sobre o risco de resolvedor aberto:

```
Recursive mode enabled; do not expose publicly (open resolver risk).
```

No painel (`http://localhost:&lt;webPort&gt;/`), o card "Recursion" e a série do gráfico temporal mostrarão contagens de consultas resolvidas recursivamente. Se o servidor estiver no modo forward, o card "Recursion" permanecerá zerado.

## O que já funciona

- resolução iterativa a partir de *root hints*
- caminhada mais completa por delegações intermediárias
- uso de *glue* in-bailiwick
- rejeição de *glue* out-of-bailiwick
- rejeição de *glue* de domínio irmão sob regras mais rígidas de *bailiwick*
- resolução de nameservers sem *glue* via *helper lookups*
- seguimento de CNAME com proteção contra loops
- fallback para TCP quando um passo UDP vem truncado
- failover de nameservers e *racing* limitado entre candidatos
- cache intermediário da resolução recursiva, incluindo zonas delegadas mais profundas
- limites de tempo e de resolução auxiliar para evitar loops sem fim

## O que ainda falta

- validação de DNSSEC
- cache negativo com tratamento mais completo de TTL negativo baseado em SOA
- suporte a DNAME
- regras mais fortes para aceitação de *referrals* e *answers*
- melhor tratamento de nameservers quebrados ou *lame*

## Notas operacionais

- mantenha o cache habilitado ao usar o modo recursivo; o desempenho depende disso
- não exponha o modo recursivo publicamente sem controles de rede; resolvers recursivos abertos são perigosos
- o modo recursivo ainda possui lacunas de implementação, então o foco atual é usabilidade, não completude total de protocolo
- conectividade DNS de saída e bootstrap correto dos *root hints* são necessários para a resolução funcionar

<div style="margin-bottom: 60px;"></div>
