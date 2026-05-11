"""Serve the Android APK for sidebar download links."""
from __future__ import annotations

import os
from pathlib import Path

from django.conf import settings
from django.http import FileResponse, HttpResponse


APK_BASENAME = "pimux.apk"


def download_android_apk(request):
    """
    Sends the CRM Android APK as a download.
    Resolution order:
      1. RESTAURANT_APK_PATH (absolute path on the server)
      2. Collected static: STATIC_ROOT/venue/downloads/pimux.apk
      3. Dev source static: venue/static/venue/downloads/pimux.apk
    """
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

    for path in roots:
        try:
            if path.is_file():
                return FileResponse(
                    path.open("rb"),
                    as_attachment=True,
                    filename=APK_BASENAME,
                    content_type="application/vnd.android.package-archive",
                )
        except OSError:
            continue

    return HttpResponse(
        "The Android APK is not on this server yet. Build the app (./gradlew assembleRelease "
        "or assembleDebug), copy the .apk to venue/static/venue/downloads/pimux.apk "
        "and redeploy, or set RESTAURANT_APK_PATH on the server.",
        status=404,
        content_type="text/plain; charset=utf-8",
    )
