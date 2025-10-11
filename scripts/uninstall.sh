#!/usr/bin/env bash
set -Eeuo pipefail
IFS=$'\n\t'

APP_NAME="dnsao"
APP_USER="${APP_NAME}"
APP_GROUP="${APP_NAME}"

APP_DIR="/etc/${APP_NAME}"
LOG_DIR="/var/log/${APP_NAME}"
SERVICE_FILE="/etc/systemd/system/${APP_NAME}.service"

info()  { echo "==> $*"; }
warn()  { echo "!!  $*" >&2; }
abort() { echo "ERROR: $*" >&2; exit 1; }

require_root() {
  [[ "${EUID}" -eq 0 ]] || abort "uninstall script should be run as root"
}

require_systemd() {
  command -v systemctl >/dev/null 2>&1 || abort "systemd not found"
  [[ -d /run/systemd/system ]] || abort "PID 1 is not systemd"
}

safe_rm_rf() {
  local path="$1"
  case "${path}" in
    "/etc/${APP_NAME}"|"/var/log/${APP_NAME}")
      if [[ -e "${path}" ]]; then
        info "removing ${path}"
        rm -rf --one-file-system -- "${path}"
      fi
      ;;
    *)
      abort "unsafe path for removal: ${path}"
      ;;
  esac
}

stop_disable_service() {
  if systemctl list-unit-files | awk '{print $1}' | grep -qx "${APP_NAME}.service"; then
    if systemctl is-active --quiet "${APP_NAME}.service"; then
      info "stopping service ${APP_NAME}"
      systemctl stop "${APP_NAME}.service" || warn "failed to stop ${APP_NAME} (moving on)"
    fi
    if systemctl is-enabled --quiet "${APP_NAME}.service"; then
      info "disabling servicee ${APP_NAME}"
      systemctl disable "${APP_NAME}.service" || warn "failed to disable service ${APP_NAME} (moving on)"
    fi
  else
    systemctl stop "${APP_NAME}.service" >/dev/null 2>&1 || true
    systemctl disable "${APP_NAME}.service" >/dev/null 2>&1 || true
  fi

  info "removing unit ${SERVICE_FILE}"
  rm -f -- "${SERVICE_FILE}"
  rm -rf -- "/etc/systemd/system/${APP_NAME}.service.d" 2>/dev/null || true

  info "reloading systemd"
  systemctl daemon-reload
  systemctl reset-failed "${APP_NAME}.service" >/dev/null 2>&1 || true
}

kill_leftovers() {
  if id -u "${APP_USER}" >/dev/null 2>&1; then
    if pgrep -u "${APP_USER}" >/dev/null 2>&1; then
      warn "there are running processes under user ${APP_USER} ; killing.."
      pkill -TERM -u "${APP_USER}" || true
      sleep 1
      if pgrep -u "${APP_USER}" >/dev/null 2>&1; then
        pkill -KILL -u "${APP_USER}" || true
      fi
    fi
  fi
}

remove_user_group() {
  if id -u "${APP_USER}" >/dev/null 2>&1; then
    info "removing user ${APP_USER}"
    userdel -r "${APP_USER}" >/dev/null 2>&1 || userdel "${APP_USER}" || warn "failed to remove user ${APP_USER}"
  fi
  if getent group "${APP_GROUP}" >/dev/null 2>&1; then
    info "removing group ${APP_GROUP}"
    groupdel "${APP_GROUP}" >/dev/null 2>&1 || warn "failed to remove group ${APP_GROUP}"
  fi
}

summary() {
  cat <<EOF

Uninstall finished ok

Removed:
  • systemd service: ${SERVICE_FILE}
  • App dir:         ${APP_DIR}
  • Log dir:         ${LOG_DIR}
  • user/group:      ${APP_USER}:${APP_GROUP}

EOF
}

require_root
require_systemd
stop_disable_service
kill_leftovers

safe_rm_rf "${APP_DIR}"
safe_rm_rf "${LOG_DIR}"

remove_user_group
summary
