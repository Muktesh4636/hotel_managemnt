#!/usr/bin/env bash
# Called on the server after rsync if backend/.env is missing.
set -euo pipefail
APP_ROOT="${DEPLOY_APP_ROOT:-/srv/restaurant-app}"
ENVF="${APP_ROOT}/backend/.env"
if [[ -f "$ENVF" ]]; then
  echo "==> $ENVF already exists"
  exit 0
fi
KEY="$(openssl rand -hex 32)"
cat >"$ENVF" <<EOF
DJANGO_SECRET_KEY=${KEY}
DJANGO_DEBUG=false
DJANGO_ALLOWED_HOSTS=pimux.store,www.pimux.store,72.61.148.117,localhost,127.0.0.1
DJANGO_CSRF_TRUSTED_ORIGINS=https://pimux.store,https://www.pimux.store
CORS_ALLOWED_ORIGINS=https://pimux.store,https://www.pimux.store
SUBSCRIPTION_UPI_VPA=9182351381@ybl
EOF
chmod 600 "$ENVF"
chown www-data:www-data "$ENVF" 2>/dev/null || true
echo "==> Created $ENVF"
