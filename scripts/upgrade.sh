#!/usr/bin/env bash
set -euo pipefail

JAR_URL="https://github.com/vitallan/dnsao/releases/latest/download/dnsao.jar"
DEST_DIR="/etc/dnsao"
DEST_JAR="${DEST_DIR}/dnsao.jar"
SERVICE_NAME="dnsao.service"
OWNER="dnsao"
MODE="0644"
CURL_OPTS="--fL --retry 3 --retry-delay 2"

die() { echo "ERROR: $*" >&2; exit 1; }
info(){ echo "-- $*"; }

need_root() {
  if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
    die "this script must run as root (use sudo)."
  }
}

have_cmd() { command -v "$1" >/dev/null 2>&1; }

download_to() {
  local url="$1" out="$2"
  if have_cmd curl; then
    curl $CURL_OPTS -o "$out" "$url"
  else
    die "curl not found to execute download."
  fi
}

validate_jar() {
  local file="$1"
  if ! head -c 2 "$file" | grep -q $'PK'; then
    die "downloaded file is not a valid jar."
  fi
}

systemd_ok() {
  have_cmd systemctl || die "systemctl not found. download and upgrade dnsao manually"
}

service_action() {
  local action="$1"
  info "systemctl ${action} ${SERVICE_NAME}"
  systemctl "${action}" "${SERVICE_NAME}"
}

need_root
systemd_ok

umask 022
mkdir -p "$DEST_DIR"

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

tmp_jar="${tmpdir}/dnsao.jar"

info "Downloading jar latest from:"
info "  ${JAR_URL}"
download_to "$JAR_URL" "$tmp_jar"

validate_jar "$tmp_jar"

timestamp="$(date +%Y%m%d-%H%M%S)"
if [[ -f "$DEST_JAR" ]]; then
  backup="${DEST_JAR}.${timestamp}.bak"
  info "backup of current jar: $backup"
  cp -p "$DEST_JAR" "$backup"
fi

info "Installing jar in: $DEST_JAR"
install -o "${OWNER%:*}" -g "${OWNER#*:}" -m "$MODE" "$tmp_jar" "${DEST_JAR}.new"
mv -f "${DEST_JAR}.new" "$DEST_JAR"

info "Restarting service: ${SERVICE_NAME}"
service_action restart

if systemctl is-active --quiet "$SERVICE_NAME"; then
  info "Upgrade finished. Service is up."
else
  echo
  systemctl status "$SERVICE_NAME" || true
  echo
  journalctl -u "$SERVICE_NAME" -n 50 --no-pager || true
  die "service is not up after upgrade."
fi
