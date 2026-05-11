# Restaurant management — Django backend (central user control)

This service is the **source of truth for accounts**: operators create and manage every user from **Django Admin**. The Android app can call the REST API to **log in** and receive a **token** (wire the app to this API in a follow-up).

## Quick start

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python manage.py migrate
python manage.py createsuperuser
python manage.py runserver 0.0.0.0:8000
```

- **Admin (manage all users):** http://127.0.0.1:8000/admin/  
  Create users with **Username** = normalized login (see below). Uncheck **Staff status** for normal mobile users; keep **Active** checked unless you want to block sign-in.

- **API base:** http://127.0.0.1:8000/api/v1/

### Login (mobile / tools)

`POST /api/v1/auth/login/`

```json
{
  "login_id": "9876543210",
  "password": "your-password"
}
```

Response:

```json
{
  "token": "…",
  "user": { "id": 1, "username": "9876543210", ... }
}
```

Use header on later requests: `Authorization: Token <token>`.

`POST /api/v1/auth/logout/` — invalidates the token (requires auth).

`GET /api/v1/auth/me/` — current user (requires auth).

`GET /api/v1/users/` — list all users (**Django admin / staff only**).

### Username rules (match the Android app)

- **Phone-style input** (only digits, spaces, `+`, `-`, parentheses): stored username should be **digits only** (e.g. `919876543210`).
- **Otherwise:** username is **lowercased** (e.g. `raj`).

When you add a user in Admin, set **Username** exactly to that normalized value so mobile login matches.

## Production notes

- Set `DJANGO_SECRET_KEY`, `DJANGO_DEBUG=false`, `DJANGO_ALLOWED_HOSTS`, and `CORS_ALLOWED_ORIGINS` (see `restaurant_backend/settings.py`).
- Use PostgreSQL instead of SQLite.
- Put the API behind HTTPS (reverse proxy or platform TLS).

## Repository layout

- `android/` — staff & guest app (currently local DB; can be pointed at this API next).
- `backend/` — this Django project.
