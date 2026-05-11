#!/usr/bin/env bash
#
# One-shot deploy: rsync backend + deploy/ to the server, then run install_on_server.sh remotely.
#
# Setup (once):
#   cp deploy/.env.deploy.example deploy/.env.deploy
#   nano deploy/.env.deploy
#
# Run (from repo root):
#   ./deploy/deploy.sh
#
# Or without .env.deploy (only host/user from environment):
#   DEPLOY_HOST=1.2.3.4 DEPLOY_USER=root ./deploy/deploy.sh
#
# Password auth: install sshpass locally, set DEPLOY_SSH_PASSWORD in .env.deploy
# Prefer: DEPLOY_SSH_KEY=/path/to/private_key
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$SCRIPT_DIR/.env.deploy"

if [[ -f "$ENV_FILE" ]]; then
  echo "==> Loading $ENV_FILE"
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

# Optional first argument: user@host (overrides .env.deploy for this run)
if [[ -n "${1:-}" && "$1" == *"@"* ]]; then
  _u="${1%%@*}"
  _h="${1#*@}"
  DEPLOY_USER="$_u"
  DEPLOY_HOST="$_h"
  shift
fi

: "${DEPLOY_HOST:?Set DEPLOY_HOST in deploy/.env.deploy (see deploy/.env.deploy.example), or run: ./deploy/deploy.sh user@host}"
DEPLOY_USER="${DEPLOY_USER:-root}"
DEPLOY_APP_ROOT="${DEPLOY_APP_ROOT:-/srv/restaurant-app}"
TARGET="${DEPLOY_USER}@${DEPLOY_HOST}"

# ssh / rsync -e: single string for -e (avoid spaces in DEPLOY_SSH_KEY path).
if [[ -n "${DEPLOY_SSH_KEY:-}" ]]; then
  _RSYNC_RSH="ssh -i ${DEPLOY_SSH_KEY} -o StrictHostKeyChecking=accept-new -o ConnectTimeout=20"
  _SSH_BASE=(ssh -i "$DEPLOY_SSH_KEY" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=20)
else
  _RSYNC_RSH="ssh -o StrictHostKeyChecking=accept-new -o ConnectTimeout=20"
  _SSH_BASE=(ssh -o StrictHostKeyChecking=accept-new -o ConnectTimeout=20)
fi
if [[ -n "${DEPLOY_SSH_PASSWORD:-}" ]] && command -v sshpass >/dev/null 2>&1; then
  _RSYNC_RSH="sshpass -e ${_RSYNC_RSH}"
fi

run_ssh() {
  if [[ -n "${DEPLOY_SSH_PASSWORD:-}" ]] && command -v sshpass >/dev/null 2>&1; then
    SSHPASS="$DEPLOY_SSH_PASSWORD" sshpass -e "${_SSH_BASE[@]}" "$TARGET" "$@"
  elif [[ -n "${DEPLOY_SSH_PASSWORD:-}" ]]; then
    echo "ERROR: DEPLOY_SSH_PASSWORD is set but 'sshpass' is not installed." >&2
    echo "  macOS: brew install hudochenkov/sshpass/sshpass" >&2
    echo "  Or set DEPLOY_SSH_KEY and use normal ssh keys." >&2
    exit 1
  else
    "${_SSH_BASE[@]}" "$TARGET" "$@"
  fi
}

run_rsync() {
  if [[ -n "${DEPLOY_SSH_PASSWORD:-}" ]] && command -v sshpass >/dev/null 2>&1; then
    export SSHPASS="$DEPLOY_SSH_PASSWORD"
    rsync -avz --delete -e "$_RSYNC_RSH" "$@"
    unset SSHPASS
  else
    rsync -avz --delete -e "$_RSYNC_RSH" "$@"
  fi
}

remote_install_cmd() {
  if [[ -n "${DEPLOY_REMOTE_INSTALL:-}" ]]; then
    echo "$DEPLOY_REMOTE_INSTALL"
  elif [[ "${DEPLOY_USER}" == "root" ]]; then
    echo "bash ${DEPLOY_APP_ROOT}/deploy/install_on_server.sh"
  else
    echo "sudo bash ${DEPLOY_APP_ROOT}/deploy/install_on_server.sh"
  fi
}

echo "==> Target: $TARGET  (app root on server: ${DEPLOY_APP_ROOT})"

echo "==> Preparing remote directory..."
run_ssh "mkdir -p '${DEPLOY_APP_ROOT}'"

echo "==> Rsync backend/ (excludes venv, sqlite, pycache)..."
run_rsync \
  --exclude '__pycache__' \
  --exclude '.venv' \
  --exclude '*.pyc' \
  --exclude 'db.sqlite3' \
  --exclude '.pytest_cache' \
  --exclude '.env' \
  --exclude 'staticfiles' \
  --exclude 'media' \
  "$REPO_ROOT/backend/" "${TARGET}:${DEPLOY_APP_ROOT}/backend/"

echo "==> Rsync deploy/..."
run_rsync "$REPO_ROOT/deploy/" "${TARGET}:${DEPLOY_APP_ROOT}/deploy/"

echo "==> Create backend/.env on server if missing..."
run_ssh "DEPLOY_APP_ROOT='${DEPLOY_APP_ROOT}' bash ${DEPLOY_APP_ROOT}/deploy/create_remote_env.sh"

echo "==> Run remote install..."
INST=$(remote_install_cmd)
echo "    $INST"
run_ssh "$INST"

# Optional: create/update Django user for web + APK login (same DB as production API).
# Set in deploy/.env.deploy: PROVISION_DJANGO_USER and PROVISION_DJANGO_PASSWORD (password ≥ 6 chars).
if [[ -n "${PROVISION_DJANGO_USER:-}" && -n "${PROVISION_DJANGO_PASSWORD:-}" ]]; then
  echo "==> Provisioning Django API user: ${PROVISION_DJANGO_USER}"
  _pu=$(printf %q "$PROVISION_DJANGO_USER")
  _pp=$(printf %q "$PROVISION_DJANGO_PASSWORD")
  run_ssh "cd '${DEPLOY_APP_ROOT}/backend' && . .venv/bin/activate && python manage.py create_signin_user ${_pu} ${_pp}"
fi

echo ""
echo "==> Deploy finished."
DOMAIN="${DEPLOY_PUBLIC_DOMAIN:-${DEPLOY_HOST}}"
echo "    Web CRM: https://${DOMAIN}/  (if DEPLOY_HOST is an IP, set DEPLOY_PUBLIC_DOMAIN to your real hostname)"
echo "    API:     https://${DOMAIN}/api/v1/"
echo "    Admin:   https://${DOMAIN}/admin/"
