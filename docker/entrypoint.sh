#!/usr/bin/env sh
set -eu

DNSAO_HOME="${DNSAO_HOME:-/opt/dnsao}"
DNSAO_CONFIG="${DNSAO_CONFIG:-/etc/dnsao}"
APP_JAR="${DNSAO_HOME}/dnsao.jar"

APP_YML="${DNSAO_CONFIG}/application.yml"
LOGBACK_XML="${DNSAO_CONFIG}/logback.xml"

APP_YML_URL="${APP_YML_URL:-https://raw.githubusercontent.com/vitallan/dnsao/refs/heads/main/config-samples/docker/application.yml}"
LOGBACK_URL="${LOGBACK_URL:-https://raw.githubusercontent.com/vitallan/dnsao/refs/heads/main/config-samples/docker/logback.xml}"

if [ "$(id -u)" -eq 0 ]; then
  RUN_USER="${RUN_USER:-dnsao}"
  RUN_GROUP="${RUN_GROUP:-dnsao}"
  DNSAO_HOME="${DNSAO_HOME:-/opt/dnsao}"
  DNSAO_CONFIG="${DNSAO_CONFIG:-/etc/dnsao}"

  mkdir -p "${DNSAO_HOME}" "${DNSAO_CONFIG}"
  chown -R "${RUN_USER}:${RUN_GROUP}" "${DNSAO_HOME}" "${DNSAO_CONFIG}"

  exec gosu "${RUN_USER}:${RUN_GROUP}" "$0" "$@"
fi


echo "[dnsao] Home: ${DNSAO_HOME}"
echo "[dnsao] Config dir: ${DNSAO_CONFIG}"

if [ ! -f "${APP_JAR}" ]; then
  echo "[dnsao] ERRO: JAR não encontrado em ${APP_JAR}." >&2
  exit 1
fi

mkdir -p "${DNSAO_CONFIG}" 

if [ ! -f "${APP_YML}" ]; then
  echo "[dnsao] application.yml not provided. Downloading default ${APP_YML_URL}..."
  if ! curl -fsSL "${APP_YML_URL}" -o "${APP_YML}"; then
    echo "[dnsao] ERROR: error downloading application.yml. Check connection or provide the application.yml " >&2
    exit 2
  fi
fi

if [ ! -f "${LOGBACK_XML}" ]; then
  echo "[dnsao] logback.xml not provided. Downloading default ${LOGBACK_URL}..."
  if ! curl -fsSL "${LOGBACK_URL}" -o "${LOGBACK_XML}"; then
    echo "[dnsao] ERROR: error downloading logback.xml; Check connection or provide the logback.xml." >&2
	exit 2
  fi
fi

JAVA_ARGS="-Dconfig=${APP_YML}"
JAVA_ARGS="${JAVA_ARGS} -Dlogback.configurationFile=${LOGBACK_XML}"

if [ "$#" -ge 1 ]; then
  CMD_BIN="$1"
  shift || true
else
  CMD_BIN="java"
  set -- -jar "${APP_JAR}"
fi

echo "[dnsao] Started: ${CMD_BIN} ${JAVA_OPTS:-} ${JAVA_ARGS} $*"
exec "${CMD_BIN}" ${JAVA_OPTS:-} ${JAVA_ARGS} "$@"
