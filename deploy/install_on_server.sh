#!/usr/bin/env bash
# Run ON THE SERVER as root (or with sudo), after uploading the project to /srv/restaurant-app
#   e.g. rsync -avz --delete ./backend/ root@72.61.148.117:/srv/restaurant-app/backend/
#        rsync -avz ./deploy/ root@72.61.148.117:/srv/restaurant-app/deploy/
#
set -euo pipefail

APP_ROOT="${APP_ROOT:-/srv/restaurant-app}"
BACKEND="${APP_ROOT}/backend"
VENV="${BACKEND}/.venv"
STATIC="${APP_ROOT}/staticfiles"

echo "==> App root: ${APP_ROOT}"

if [[ ! -f "${BACKEND}/manage.py" ]]; then
  echo "Missing ${BACKEND}/manage.py — upload the backend tree first." >&2
  exit 1
fi

if [[ ! -f "${BACKEND}/.env" ]]; then
  echo "Create ${BACKEND}/.env from deploy/env.example (chmod 600)." >&2
  exit 1
fi

apt-get update -qq
apt-get install -y -qq python3-venv python3-pip nginx rsync

install -d -m 755 "${STATIC}"

if [[ ! -d "${VENV}" ]]; then
  python3 -m venv "${VENV}"
fi
"${VENV}/bin/pip" install --upgrade pip
"${VENV}/bin/pip" install -r "${BACKEND}/requirements.txt"

chown -R www-data:www-data "${BACKEND}" "${STATIC}"

sudo -u www-data bash -c "
  set -a
  source '${BACKEND}/.env'
  set +a
  cd '${BACKEND}'
  '${VENV}/bin/python' manage.py migrate --noinput
  '${VENV}/bin/python' manage.py collectstatic --noinput
"

# STATIC_ROOT is under backend; nginx serves from ${STATIC}
if [[ -d "${BACKEND}/staticfiles" ]]; then
  rsync -a --delete "${BACKEND}/staticfiles/" "${STATIC}/"
  chown -R www-data:www-data "${STATIC}"
fi

cp "${APP_ROOT}/deploy/gunicorn-restaurant.service" /etc/systemd/system/gunicorn-restaurant.service
systemctl daemon-reload
systemctl enable gunicorn-restaurant
systemctl stop gunicorn-restaurant 2>/dev/null || true
systemctl reset-failed gunicorn-restaurant 2>/dev/null || true
systemctl start gunicorn-restaurant

if [[ -f "${APP_ROOT}/deploy/nginx-pimux.store.conf" ]]; then
  cp "${APP_ROOT}/deploy/nginx-pimux.store.conf" /etc/nginx/sites-available/pimux.store
  ln -sf /etc/nginx/sites-available/pimux.store /etc/nginx/sites-enabled/pimux.store
  nginx -t
  systemctl reload nginx
fi

echo "==> Done. Check: systemctl status gunicorn-restaurant"
echo "==> API: http://127.0.0.1:8002/api/v1/ (via nginx: your domain)"
