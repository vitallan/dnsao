#!/usr/bin/env bash
set -Eeuo pipefail
IFS=$'\n\t'

APP_NAME="dnsao"
APP_USER="${APP_NAME}"
APP_GROUP="${APP_NAME}"

APP_DIR="/etc/${APP_NAME}"
LOG_DIR="/var/log/${APP_NAME}"
SERVICE_FILE="/etc/systemd/system/${APP_NAME}.service"

JAR_URL="https://github.com/vitallan/dnsao/releases/latest/download/dnsao.jar"
LOGBACK_URL="https://raw.githubusercontent.com/vitallan/dnsao/refs/tags/prod/config-samples/balanced/logback.xml"
APP_YML_URL="https://raw.githubusercontent.com/vitallan/dnsao/refs/tags/prod/config-samples/balanced/application.yml"

JAVA_BIN="$(command -v java || true)"
CURL_BIN="$(command -v curl || true)"
NOLOGIN_BIN="$(command -v nologin || echo /usr/sbin/nologin)"

abort() { echo "ERRO: $*" >&2; exit 1; }
info()  { echo "==> $*"; }

if ss -tulpn | grep -qE '(:|%lo:)53\b'; then
  info "Port 53 is in use (probably systemd-resolved)."
  info "If wanted, to disable: sudo systemctl disable --now systemd-resolved && sudo rm -f /etc/resolv.conf && echo 'nameserver 127.0.0.1' | sudo tee /etc/resolv.conf"
  info "or fix the local dns before installing DNSao"
fi

require_root() {
  [[ "${EUID}" -eq 0 ]] || abort "install script should be run as root"
}

require_systemd() {
  command -v systemctl >/dev/null 2>&1 || abort "systemd not found"
  [[ -d /run/systemd/system ]] || abort "PID 1 is not systemd"
}

require_tools() {
  [[ -n "${CURL_BIN}" ]] || abort "curl not found in PATH"
  [[ -n "${JAVA_BIN}" ]] || abort "java not found in PATH"
}

create_user_group() {
  if ! getent group "${APP_GROUP}" >/dev/null; then
    info "creating group ${APP_GROUP}"
    groupadd --system "${APP_GROUP}"
  fi
  if ! id -u "${APP_USER}" >/dev/null 2>&1; then
    info "creating user ${APP_USER}"
    useradd --system --no-create-home --shell "${NOLOGIN_BIN}" --gid "${APP_GROUP}" "${APP_USER}"
  fi
}

create_dirs() {
  info "creating directories"
  install -d -m 0755 "${APP_DIR}"
  install -d -m 0755 -o "${APP_USER}" -g "${APP_GROUP}" "${LOG_DIR}"
}

download_file() {
  local url="$1" dest="$2" mode="$3" owner="$4" group="$5"
  local tmp="$(mktemp)"
  info "downloading ${url} -> ${dest}"
  "${CURL_BIN}" -fL --proto '=https' --tlsv1.2 -o "${tmp}" "${url}" || abort "download failed ${url}"
  install -m "${mode}" -o "${owner}" -g "${group}" -D "${tmp}" "${dest}"
  rm -f "${tmp}"
}

write_unit_file() {
  info "writing service file ${SERVICE_FILE}"
  cat > "${SERVICE_FILE}" <<'UNIT'
[Unit]
Description=DNSao — multi-upstream DNS forwarder
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=dnsao
Group=dnsao
WorkingDirectory=/etc/dnsao

ExecStart=/usr/bin/env java \
  -Xms128m -Xmx128m \
  -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m \
  -Xss512k \
  -Dconfig=/etc/dnsao/application.yml \
  -Dlogback.configurationFile=/etc/dnsao/logback.xml \
  -jar /etc/dnsao/dnsao.jar

Restart=on-failure
RestartSec=5

AmbientCapabilities=CAP_NET_BIND_SERVICE
CapabilityBoundingSet=CAP_NET_BIND_SERVICE
NoNewPrivileges=true

ProtectSystem=strict
ProtectHome=true
PrivateTmp=true
ReadWritePaths=/var/log/dnsao

LimitNOFILE=65536

StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
UNIT
}

reload_enable_start() {
  info "reloading systemd"
  systemctl daemon-reload
  info "enabling ${APP_NAME} on boot"
  systemctl enable "${APP_NAME}.service"
  info "starting service"
  systemctl restart "${APP_NAME}.service"
}

print_summary() {
  cat <<EOF

Installation finished ok

Files:
  • Jar:           ${APP_DIR}/dnsao.jar
  • Config:        ${APP_DIR}/application.yml
  • Logback:       ${APP_DIR}/logback.xml
  • Logs:          ${LOG_DIR}
  • Unit systemd:  ${SERVICE_FILE}

Useful commands:
  systemctl status ${APP_NAME}
  journalctl -u ${APP_NAME} -e
  tail -n 100 -f ${LOG_DIR}/*.log

EOF
}


require_root
require_systemd
require_tools
create_user_group
create_dirs

download_file "${JAR_URL}"      "${APP_DIR}/dnsao.jar"        "0644" "${APP_USER}" "${APP_GROUP}"
download_file "${LOGBACK_URL}"  "${APP_DIR}/logback.xml"     "0644" "root"       "root"
download_file "${APP_YML_URL}"  "${APP_DIR}/application.yml" "0644" "root"       "root"

chown "${APP_USER}:${APP_GROUP}" "${APP_DIR}/dnsao.jar"
chmod 0644 "${APP_DIR}/dnsao.jar"

write_unit_file
reload_enable_start
print_summary
