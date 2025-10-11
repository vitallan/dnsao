#!/usr/bin/env sh
set -euo pipefail

DNSAO_HOME="${DNSAO_HOME:-/opt/dnsao}"
DNSAO_CONFIG="${DNSAO_CONFIG:-/etc/dnsao}"
APP_JAR="${DNSAO_HOME}/dnsao.jar"

APP_YML="${DNSAO_CONFIG}/application.yml"
LOGBACK_XML="${DNSAO_CONFIG}/logback.xml"

# the default urls are pointing to 'prod' tag in github in default 
APP_YML_URL="${APP_YML_URL:-https://raw.githubusercontent.com/vitallan/dnsao/refs/tags/prod/config-samples/docker/application.yml}"
LOGBACK_URL="${LOGBACK_URL:-https://raw.githubusercontent.com/vitallan/dnsao/refs/tags/prod/config-samples/docker/logback.xml}"

echo "[dnsao] Home: ${DNSAO_HOME}"
echo "[dnsao] Config dir: ${DNSAO_CONFIG}"

if [ ! -f "${APP_JAR}" ]; then
  echo "[dnsao] ERRO: JAR não encontrado em ${APP_JAR}." >&2
  exit 1
fi

mkdir -p "${DNSAO_CONFIG}" 2>/dev/null || true

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
