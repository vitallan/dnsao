# Installation

There are three ways to install **DNSao**:

- [Via script](#installation-via-script)
- [Via Docker (Recommended)](#installation-via-docker)
- [Manual](#manual-installation)

After the instalation is complete, you can point your devices (or, ideally your router) to use it as a DNS server. Reach to the web port defined in [application.yml](configuration.md) to check the metrics dashboard and enjoy the ride.

## Installation via script

The only dependency of **DNSao** is the presence of a JDK version 17 or higher.

If your server is Debian-based:

```bash
apt-get update -y
apt-get install -y openjdk-17-jre-headless
```

If red hat based:

```bash
dnf install -y java-17-openjdk-headless
```

Other options can be found on the [official OpenJDK website](https://openjdk.org/install/){:target="_blank"}. After installing the JDK, the machine will be ready to run **DNSao**.

Before installing, visit the [installation script]({{ install_url }}){:target="_blank"} to review and confirm what will be executed. Also, confirm that nothing else is listening on port 53 to avoid conflict:

```bash
sudo ss -tulpn | grep :53
```

This should return nothing.

To execute the installation script, simply run the following command:

```bash
curl -sSL {{ install_url }} | bash
```

On the server itself, if the **dig** command is available, you can validate the installation with:

```bash
dig debian.org @127.0.0.1
```

The result should look like this:

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

Application logs should be available at:

```bash
tail -f /var/log/dnsao/dnsao.log
```

You can now reach http://YOUR.SERVER.IP:8044 and check the metrics dashboard. Using this method will make **DNSao** run as a systemctl service, so systemctl commands should work:

```bash
sudo systemctl stop dnsao
sudo systemctl enable dnsao
sudo systemctl start dnsao
```

To uninstall, you can use the [uninstall script]({{uninstall_url}}).

## Installation via Docker

You can use docker to run **DNSao** as well. Make sure that the host machine is not running anything on port 53:

```bash
sudo ss -tulpn | grep :53
```

This should return nothing. Then you can use docker compose:

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

If `/your/local/volume` is empty, **DNSao** will download the default docker application.yml and logback.xml files to the volume and use then. 

## Manual installation

You can also download the [latest jar]({{latest_jar_url}}) and perform a [manual configuration](configuration.md). Remember that **DNSao** requires both an application configuration file and a log configuration file. A standard execution example would be as follows:

```bash
java -Dconfig=/etc/dnsao/application.yml -Dlogback.configurationFile=/etc/dnsao/logback.xml -jar dnsao.jar
```

Keep in mind that, on Linux, ports below 1024 require root privileges to run without being managed by a service. Consider this when running the server manually.

Another important detail: since it’s a Java application, it’s recommended to limit the memory sizes used to avoid excessive consumption. In its default installation script, **DNSao** runs with the following flags:

- **-Xms128m -Xmx128m**: limits the heap size to 128 MB  
- **-XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m**: limits the metaspace size  
- **-Xss512k**: limits the maximum stack size per thread  

The final command is then:

```bash
java -Dconfig=/etc/dnsao/application.yml -Dlogback.configurationFile=/etc/dnsao/logback.xml -Xms128m -Xmx128m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m -Xss512k  -jar /etc/dnsao/dnsao.jar
```

<div style="margin-bottom: 60px;"></div>
