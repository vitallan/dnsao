FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -DskipTests -B -U package

RUN set -eux; \
    mkdir -p /out; \
    JAR="$(ls -1 target/*.jar | grep -v -E '(sources|javadoc)' | head -n1)"; \
    cp "$JAR" /out/dnsao.jar

FROM eclipse-temurin:17-jre

LABEL org.opencontainers.image.title="DNSao" \
      org.opencontainers.image.description="Multi-upstream DNS forwarder with cache & upstream DoT support" \
      org.opencontainers.image.licenses="MIT"

ENV DNSAO_HOME=/opt/dnsao \
    DNSAO_CONFIG=/etc/dnsao \
    JAVA_OPTS="-Xms128m -Xmx128m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m -Xss512k -XX:+ExitOnOutOfMemoryError"

USER root
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl \
  && rm -rf /var/lib/apt/lists/*

RUN groupadd -r dnsao && useradd -r -g dnsao -d ${DNSAO_HOME} -s /usr/sbin/nologin dnsao

WORKDIR ${DNSAO_HOME}
COPY --from=build /out/dnsao.jar ${DNSAO_HOME}/dnsao.jar

RUN mkdir -p ${DNSAO_CONFIG} \
  && chown -R dnsao:dnsao ${DNSAO_HOME} ${DNSAO_CONFIG}
VOLUME ["/etc/dnsao"]

EXPOSE 8053/tcp 8053/udp 8044/tcp

COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

STOPSIGNAL SIGTERM
USER dnsao

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["java","-jar","/opt/dnsao/dnsao.jar"]
