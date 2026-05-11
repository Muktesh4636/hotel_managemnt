"""
Full web shell at / and /app/ — same bottom navigation and Material 3 palette
as the Android APK (see android/.../theme/AppTheme.kt), using the REST API.

Dedicated paths (see restaurant_backend.urls) set `web_route` for the first
paint (main tab + optional workspace module). The client syncs the address
bar when you change tabs or open an operations module.
"""
from __future__ import annotations

import json

from django.conf import settings
from django.http import HttpRequest, HttpResponse
from django.shortcuts import render
from django.templatetags.static import static as static_url

from .views_operations import (
    ACCENT_RING,
    BADGE_EMOJI,
    BILLING_PLANS,
    OPERATIONS_ENTRIES,
    _format_inr,
    _upi_query,
)


def _valid_workspace_module(slug: str) -> bool:
    return any(row["slug"] == slug for row in OPERATIONS_ENTRIES)


def _web_route_from_request(
    request: HttpRequest,
    workspace_module: str | None = None,
) -> dict[str, str]:
    """
    segment: sign-in | dashboard | pos | kitchen | orders | reports | workspace | "" (default shell).
    module: internal operations slug (e.g. menu_admin) when URL is /workspace/<kebab>/.
    """
    # URLconf passes this kwarg for path("workspace/<slug:workspace_module>/", web_app).
    if workspace_module is not None:
        mod = workspace_module.lower().replace("-", "_")
        if mod and not _valid_workspace_module(mod):
            mod = ""
        return {"segment": "workspace", "module": mod}

    path = (request.path or "/").strip("/")
    parts = [p for p in path.split("/") if p]
    if not parts or parts[0] == "app":
        return {"segment": "", "module": ""}
    first = parts[0].lower()
    if first == "sign-in":
        return {"segment": "sign-in", "module": ""}
    if first in ("dashboard", "pos", "kitchen", "orders", "reports"):
        return {"segment": first, "module": ""}
    if first == "workspace":
        raw = parts[1] if len(parts) > 1 else ""
        mod = raw.lower().replace("-", "_") if raw else ""
        if mod and not _valid_workspace_module(mod):
            mod = ""
        return {"segment": "workspace", "module": mod}
    return {"segment": "", "module": ""}


def web_app(request: HttpRequest, workspace_module: str | None = None):
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
            "web_route": _web_route_from_request(request, workspace_module=workspace_module),
            "food_static_base": static_url("venue/food/"),
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
        "background_color": "#f5f2ee",
        "theme_color": "#b84732",
    }
    return HttpResponse(
        json.dumps(body),
        content_type="application/manifest+json; charset=utf-8",
    )
