"""Serve the Android APK for sidebar download links."""
from __future__ import annotations

import os
from pathlib import Path

from django.conf import settings
from django.http import FileResponse, HttpResponse


APK_BASENAME = "Pimux.apk"


def _apk_candidate_paths() -> list[Path]:
    """Same resolution order as the download view (first match wins)."""
    roots: list[Path] = []
    env_path = os.environ.get("RESTAURANT_APK_PATH", "").strip()
    if env_path:
        roots.append(Path(env_path))
    roots.append(Path(settings.STATIC_ROOT) / "venue" / "downloads" / APK_BASENAME)
    roots.append(
        Path(settings.BASE_DIR)
        / "venue"
        / "static"
        / "venue"
        / "downloads"
        / APK_BASENAME,
    )
    return roots


def apk_cache_version() -> str:
    """Stable token for ?v=… when the APK file on disk changes (avoids stale browser cache)."""
    for path in _apk_candidate_paths():
        try:
            if path.is_file():
                return str(int(path.stat().st_mtime))
        except OSError:
            continue
    return "0"


def download_android_apk(request):
    """
    Sends the CRM Android APK as a download.
    Resolution order:
      1. RESTAURANT_APK_PATH (absolute path on the server)
      2. Collected static: STATIC_ROOT/venue/downloads/Pimux.apk
      3. Dev source static: venue/static/venue/downloads/Pimux.apk
    """
    for path in _apk_candidate_paths():
        try:
            if path.is_file():
                resp = FileResponse(
                    path.open("rb"),
                    as_attachment=True,
                    filename=APK_BASENAME,
                    content_type="application/vnd.android.package-archive",
                )
                # Browsers aggressively cache APK URLs; without this users keep an old build.
                resp["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0"
                resp["Pragma"] = "no-cache"
                resp["Expires"] = "0"
                return resp
        except OSError:
            continue

    err = HttpResponse(
        "The Android APK is not on this server yet. Build the app (./gradlew assembleRelease "
        "or assembleDebug), copy the .apk to venue/static/venue/downloads/Pimux.apk "
        "and redeploy, or set RESTAURANT_APK_PATH on the server.",
        status=404,
        content_type="text/plain; charset=utf-8",
    )
    err["Cache-Control"] = "no-store"
    return err
