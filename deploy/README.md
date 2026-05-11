# Deploy backend to your VPS (pimux.store)

This deploys the **Django API** (`backend/`) behind **nginx** + **gunicorn**.  
The Android app is **not** deployed here (Play Store / APK sideload separately).

## 0. Security

You posted a root password in chat — **rotate it** on the server after setup and prefer **SSH keys**:

```bash
ssh-copy-id -i ~/.ssh/id_ed25519.pub root@72.61.148.117
```

Do **not** commit `.env` or passwords to git.

## 1. Automatic deploy (recommended)

From the **repo root**:

```bash
cp deploy/.env.deploy.example deploy/.env.deploy
# Edit: DEPLOY_HOST, DEPLOY_USER, and either DEPLOY_SSH_KEY or (sshpass +) DEPLOY_SSH_PASSWORD
nano deploy/.env.deploy
./deploy/deploy.sh
```

`deploy.sh` will: create `/srv/restaurant-app` on the server, **rsync** `backend/` + `deploy/`, then run **`install_on_server.sh`** remotely (venv, migrate, collectstatic, gunicorn, nginx).

- **Key auth (best):** set `DEPLOY_SSH_KEY=/path/to/id_ed25519` — no password stored.
- **Password auth:** install `sshpass` locally, set `DEPLOY_SSH_PASSWORD` in `.env.deploy` (do **not** commit this file; rotate server password after).

**Shorthand (no `.env.deploy` — only host/user in env):**

```bash
DEPLOY_HOST=72.61.148.117 DEPLOY_USER=root ./deploy/deploy.sh
```

**Legacy one-liner (same as above + implicit `.env.deploy` if you use only host):**

```bash
./deploy/push_to_server.sh root@72.61.148.117
```

**Manual rsync** (if you do not use the script):

```bash
export SERVER=root@72.61.148.117
ssh ${SERVER} 'mkdir -p /srv/restaurant-app'
rsync -avz --delete ./backend/ ${SERVER}:/srv/restaurant-app/backend/
rsync -avz ./deploy/ ${SERVER}:/srv/restaurant-app/deploy/
```

**Tarball** (Hostinger file manager / `scp`): create with  
`tar czf /tmp/restaurant-backend-deploy.tgz -C /path/to/repo backend deploy`  
then unpack under `/srv/restaurant-app` on the server.

## 2. On the server — create `.env`

```bash
ssh ${SERVER}
sudo mkdir -p /srv/restaurant-app
sudo nano /srv/restaurant-app/backend/.env
```

Copy fields from `deploy/env.example`. Generate a secret key:

```bash
python3 -c "from django.core.management.utils import get_random_secret_key; print(get_random_secret_key())"
```

Set `DJANGO_DEBUG=false` and comma-separated `DJANGO_ALLOWED_HOSTS` / `DJANGO_CSRF_TRUSTED_ORIGINS` / `CORS_ALLOWED_ORIGINS` for `https://pimux.store` (and `www` if used).

```bash
sudo chmod 600 /srv/restaurant-app/backend/.env
sudo chown www-data:www-data /srv/restaurant-app/backend/.env
```

## 3. On the server — install

```bash
sudo bash /srv/restaurant-app/deploy/install_on_server.sh
```

## 4. TLS (Let’s Encrypt)

If nginx already terminates HTTPS elsewhere, align `proxy_set_header X-Forwarded-Proto` with reality.  
Otherwise, on the server:

```bash
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot --nginx -d pimux.store -d www.pimux.store
```

Then enable the HTTPS `server { }` block in `deploy/nginx-pimux.store.conf` (or certbot may patch nginx for you).

## 5. Smoke test

```bash
curl -sI https://pimux.store/admin/
curl -sI https://pimux.store/api/v1/sync/full/
curl -sI https://pimux.store/app/
```

The **browser UI** that mirrors the Android shell (Home / POS / Kitchen / More + Operations) lives at **`/app/`** (token login, same REST API as the APK).

`/api/v1/sync/full/` returns **401** without a token — that still proves routing works.

## Point the Android app

In **Global settings → Log in to backend**, set base URL to the **site root only** (no `/api/v1` suffix — the app adds paths itself), e.g.:

`https://pimux.store`
