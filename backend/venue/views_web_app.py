"""
Full web shell at / and /app/ — same bottom navigation and Material 3 palette
as the Android APK (see android/.../theme/AppTheme.kt), using the REST API.
"""
from __future__ import annotations

import json

from django.conf import settings
from django.http import HttpResponse
from django.shortcuts import render

from .views_operations import (
    ACCENT_RING,
    BADGE_EMOJI,
    BILLING_PLANS,
    OPERATIONS_ENTRIES,
    _format_inr,
    _upi_query,
)


def web_app(request):
    vpa = str(getattr(settings, "SUBSCRIPTION_UPI_VPA", "") or "").strip() or "9182351381@ybl"
    modules = []
    for i, row in enumerate(OPERATIONS_ENTRIES):
        modules.append(
            {
                **row,
                "index": i,
                "card_variant": i % 3,
                "badge_emoji": BADGE_EMOJI[i % len(BADGE_EMOJI)],
                "ring_color": ACCENT_RING[i % len(ACCENT_RING)],
            }
        )
    plans = []
    for p in BILLING_PLANS:
        qs = _upi_query(vpa, p["price_inr"], p["headline"])
        plans.append(
            {
                **p,
                "price_display": _format_inr(p["price_inr"]),
                "upi_qs": qs,
            }
        )
    return render(
        request,
        "venue/web_app.html",
        {
            "web_modules": modules,
            "billing_plans": plans,
            "upi_vpa": vpa,
        },
    )


def web_app_manifest(request):
    """PWA-style manifest so browsers (and Add to Home Screen) treat this like the companion to the APK."""
    start = request.build_absolute_uri("/")
    body = {
        "name": "Restaurant management CRM",
        "short_name": "Restaurant CRM",
        "description": "Same venue account as the Android app — desk web CRM and mobile APK.",
        "start_url": start,
        "scope": "/",
        "display": "standalone",
        "background_color": "#FFFBF7",
        "theme_color": "#C2543D",
    }
    return HttpResponse(
        json.dumps(body),
        content_type="application/manifest+json; charset=utf-8",
    )
