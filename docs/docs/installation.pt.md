# Instalação

Há três formas de instalar **DNSao**:

- [Por script](#instalacao-por-script)
- [Via Docker (recomendado)](#instalacao-via-docker)
- [Manual](#instalacao-manual)

Depois de terminar a instalação, você pode configurar seus dispositivos (ou, idealmente, seu roteador) para usar **DNSao** como seu servidor de DNS. Acesse a porta web definida no [aplication.yml](configuration.pt.md) para ter acesso ao dashboard de métricas.

## Instalação por script

A única dependência de **DNSao** é a presença de uma jdk versão 17 ou maior. 

Se o seu servidor for debian based:

```bash
apt-get update -y
apt-get install -y openjdk-17-jre-headless
```

Se for red hat:

```bash
dnf install -y java-17-openjdk-headless
```

outras opções podem ser encontradas no [próprio site da openjdk](https://openjdk.org/install/){:target="_blank"}. Após a instalação da jdk, a máquina estará preparada para rodar **DNSao**.

Antes de instalar, visite o [script de instalação]({{ install_url }}){:target="_blank"} para revisar e confirmar o que está sendo executado. Confirme também que não há nenhum processo escutando a porta 53.

```bash
sudo ss -tulpn | grep :53
```

Esse comando deve retornar vazio.
   
para executar o script de instalação, basta rodar o comando abaixo:

```bash
curl -sSL {{ install_url }} | bash
```

No próprio servidor, se o comando **dig** estiver disponível, você pode validar a instalação com:

```bash
dig debian.org @127.0.0.1
```

O resultado deve ser algo parecido com:

```bash
; <<>> DiG 9.20.11-4-Debian <<>> debian.org @127.0.0.1
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 4434
;; flags: qr rd ra ad; QUERY: 1, ANSWER: 4, AUTHORITY: 0, ADDITIONAL: 1

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 512
;; QUESTION SECTION:
;debian.org.                    IN      A

;; ANSWER SECTION:
debian.org.             279     IN      A       151.101.2.132
debian.org.             279     IN      A       151.101.130.132
debian.org.             279     IN      A       151.101.194.132
debian.org.             279     IN      A       151.101.66.132

;; Query time: 7 msec
;; SERVER: 192.168.150.150#53(192.168.150.150) (UDP)
;; WHEN: Wed Jan 05 17:38:05 -03 2020
;; MSG SIZE  rcvd: 103
```

Os logs da aplicação deverão estar disponíveis em 

```bash
tail -f /var/log/dnsao/dnsao.log
```

Você pode então acessar http://IP.DO.SEU.SERVIDOR:8044 e analisar o painel de métricas de **DNSao**. Usando esse método fará **DNSao** executar como um serviço systemctl, então comandos systemctl deverão ser usados para a gestão do serviço:

```bash
sudo systemctl stop dnsao
sudo systemctl enable dnsao
sudo systemctl start dnsao
```

Para desinstalar, voce pode usar o [script de desinstalação]({{uninstall_url}}).

## Instalação via Docker

Você pode usar docker para rodar **DNSao** também. Confirme que não há nada rodando na porta 53:

```bash
sudo ss -tulpn | grep :53
```

Esse comando não deve retornar nada. Então você pode usar docker compose:

```yaml

version: "3.8"

services:
  dnsao:
    image: ghcr.io/vitallan/dnsao:latest
    container_name: dnsao
    restart: unless-stopped

    ports:
      - "53:8053/tcp"
      - "53:8053/udp"
      - "8044:8044"

    volumes:
      - /your/local/volume:/etc/dnsao

```

E executar `docker compose up -d`.

Se `/your/local/volume` estiver vazio, **DNSao** irá fazer o download dos arquivos application.yml e logback.xml padrão para docker no volume montado e os usará.

## Instalação manual

Você também pode baixar o [último jar]({{latest_jar_url}}) disponibilizado e realizar as [configurações manualmente](configuration.pt.md). Lembrando que o **DNSao** precisa de uma configuração de aplicação e um arquivo de configuração para os logs. Um exemplo de execução padrão seria o abaixo:

```bash
java -Dconfig=/etc/dnsao/application.yml -Dlogback.configurationFile=/etc/dnsao/logback.xml -jar dnsao.jar
```

Lembrando que, em linux, portas abaixo de 1024 precisam de permissão de root para rodar sem ser por serviço. Considere isso quando for executar manualmente o servidor.

Outro detalhe importante: por ser uma aplicação java, é recomendado limitar os tamanhos de memória usados, para evitar consumo exagerado. Em seu script de instalação padrão, **DNSao** é executado com as seguintes flags:

- **-Xms128m -Xmx128m** : limita o tamanho usado pela heap para 128 mb
- **-XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m** : limita o tamanho usado pelo metaspace
- **-Xss512k** : limita o tamanho máximo da stack para cada thread

O comando final fica então:

```bash
java -Dconfig=/etc/dnsao/application.yml -Dlogback.configurationFile=/etc/dnsao/logback.xml -Xms128m -Xmx128m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m -Xss512k  -jar /etc/dnsao/dnsao.jar
```

<div style="margin-bottom: 60px;"></div>