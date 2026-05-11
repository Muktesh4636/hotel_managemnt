#!/usr/bin/env bash
# Run ON THE SERVER as root once: obtain certs, merge HTTPS into nginx, reload.
# Prereq: nginx already serves pimux.store on :80 with /.well-known/ (from install_on_server.sh).

set -euo pipefail
APP_ROOT="${APP_ROOT:-/srv/restaurant-app}"
mkdir -p /var/www/certbot

if [[ ! -f /etc/letsencrypt/live/pimux.store/fullchain.pem ]]; then
  certbot certonly \
    --webroot -w /var/www/certbot \
    -d pimux.store -d www.pimux.store \
    --non-interactive --agree-tos \
    --register-unsafely-without-email
fi

MAIN="${APP_ROOT}/deploy/nginx-pimux.store.conf"
SSL="${APP_ROOT}/deploy/nginx-pimux.store.ssl.conf"
if [[ ! -f "$SSL" ]]; then
  echo "Missing $SSL" >&2
  exit 1
fi

cat "$MAIN" "$SSL" > /etc/nginx/sites-available/pimux.store
nginx -t
systemctl reload nginx
echo "HTTPS enabled for pimux.store — test: curl -skI https://pimux.store/app/ | head -3"
