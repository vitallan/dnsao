#!/usr/bin/env bash
set -euo pipefail

JAR_URL="https://github.com/vitallan/dnsao/releases/latest/download/dnsao.jar"
DEST_DIR="/etc/dnsao"
DEST_JAR="${DEST_DIR}/dnsao.jar"
SERVICE_NAME="dnsao.service"
OWNER="dnsao:dnsao"
MODE="0644"
CURL_OPTS="-fL --retry 3 --retry-delay 2"

die() { echo "ERROR: $*" >&2; exit 1; }
info(){ echo "-- $*"; }

need_root() {
  if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
    die "this script must run as root (use sudo)."
  fi
}

have_cmd() { command -v "$1" >/dev/null 2>&1; }

download_to() {
  local url="$1" out="$2" cond_src="${3:-}"
  local extra=()
  if [[ -n "$cond_src" && -f "$cond_src" ]]; then
    extra=(-z "$cond_src")
  fi
  if have_cmd curl; then
    curl $CURL_OPTS "${extra[@]}" -o "$out" "$url"
  else
    die "curl not found to execute download."
  fi
}

validate_jar() {
  local file="$1"
  if ! head -c 2 "$file" | grep -q $'PK'; then
    die "downloaded file is not a valid jar."
  fi
  if [[ ! -s "$file" ]]; then
    die "downloaded file is empty."
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
download_to "$JAR_URL" "$tmp_jar" "$DEST_JAR"

if [[ ! -s "$tmp_jar" && -f "$DEST_JAR" ]]; then
  info "Already up to date. Nothing to do."
  exit 0
fi

validate_jar "$tmp_jar"

timestamp="$(date +%Y%m%d-%H%M%S)"
backup=""
if [[ -f "$DEST_JAR" ]]; then
  backup="${DEST_JAR}.${timestamp}.bak"
  info "backup of current jar: $backup"
  cp -p "$DEST_JAR" "$backup"
fi

info "Installing jar in: $DEST_JAR"
install -m "$MODE" "$tmp_jar" "${DEST_JAR}.new"
mv -f "${DEST_JAR}.new" "$DEST_JAR"

if [[ -n "$OWNER" ]]; then
  chown "$OWNER" "$DEST_JAR" 2>/dev/null || true
fi

info "Restarting service: ${SERVICE_NAME}"
set +e
systemctl restart "$SERVICE_NAME"
restart_rc=$?
set -e

if (( restart_rc != 0 )) || ! systemctl is-active --quiet "$SERVICE_NAME"; then
  echo
  systemctl status "$SERVICE_NAME" || true
  echo
  journalctl -u "$SERVICE_NAME" -n 50 --no-pager || true

  if [[ -n "$backup" && -f "$backup" ]]; then
    info "Rollback to previous jar..."
    cp -f "$backup" "$DEST_JAR"
    set +e
    systemctl restart "$SERVICE_NAME"
    rb_rc=$?
    set -e
    if (( rb_rc == 0 )) && systemctl is-active --quiet "$SERVICE_NAME"; then
      die "service failed after upgrade; rolled back successfully."
    else
      die "service failed after upgrade; rollback also failed."
    fi
  else
    die "service is not up after upgrade and no backup was available."
  fi
fi

if [[ -n "$backup" && -f "$backup" ]]; then
  info "Removing backup: $backup"
  rm -f "$backup"
fi

info "Upgrade finished. Service is up."
systemctl --no-pager status "$SERVICE_NAME" | sed -n '1,5p' || true
