"""
Browser Operations hub — layout and colors aligned with the Android app (Jetpack Compose).
"""
from __future__ import annotations

import urllib.parse

from django.conf import settings
from django.shortcuts import render

# Same order and copy as AdminScreens.hubEntriesAll (Destinations slug for future deep links).
OPERATIONS_ENTRIES = [
    {
        "slug": "orders",
        "title": "Order history",
        "subtitle": "Tap ticket for items · long-press for edit/delete",
    },
    {
        "slug": "menu_admin",
        "title": "Menu & item availability",
        "subtitle": "Edit categories, prices, 86 items",
    },
    {
        "slug": "qr_menu",
        "title": "Customer QR menu",
        "subtitle": "QR code — guests order from phone to kitchen",
    },
    {
        "slug": "inventory",
        "title": "Inventory & stock",
        "subtitle": "Track ingredients and low-stock alerts",
    },
    {
        "slug": "expenses",
        "title": "Expenses",
        "subtitle": "Track operating costs and running total",
    },
    {
        "slug": "staff",
        "title": "Staff",
        "subtitle": "Salaries, absent days & roster",
    },
    {
        "slug": "reports",
        "title": "Reports",
        "subtitle": "Revenue, expenses, salaries & net profit",
    },
    {
        "slug": "tables_floor",
        "title": "Tables & floor",
        "subtitle": "Layout, merge or split tables, floor status",
    },
    {
        "slug": "reservations",
        "title": "Reservations & waitlist",
        "subtitle": "Bookings, party size, time slots",
    },
    {
        "slug": "suppliers_po",
        "title": "Suppliers & purchase orders",
        "subtitle": "Vendors, POs, receiving",
    },
    {
        "slug": "waste_log",
        "title": "Waste & spoilage",
        "subtitle": "Log shrink apart from sales",
    },
    {
        "slug": "cash_drawer",
        "title": "Cash drawer & shifts",
        "subtitle": "Opening float, shift close summary",
    },
    {
        "slug": "customer_feedback",
        "title": "Customer feedback",
        "subtitle": "Notes linked to order or table",
    },
    {
        "slug": "settings",
        "title": "Global settings",
        "subtitle": "Menu categories, modules, tax & venue name",
    },
]

# Same tiers as AdminScreens.billingPlanOffers (web shows static copy; app remains source for edits).
BILLING_PLANS = [
    {
        "price_inr": 299,
        "headline": "Starter",
        "summary": "POS and core back office — no tables module, no extra admins.",
        "bullets": [
            "Counter and quick-service workflows: orders, kitchen, menu, inventory, expenses, staff and reports on your venue account.",
            "Does not include Tables & floor: no table layout, table assignment, or table-wise check flows.",
            "Does not include Manage admins: one primary operator model — no separate admin logins with their own roles.",
        ],
    },
    {
        "price_inr": 499,
        "headline": "Tables",
        "summary": "Tables and floor; guest-facing website is an optional paid add-on.",
        "bullets": [
            "Everything in the Starter (₹299) tier.",
            "Tables & floor: layout, assign orders to tables, and table-linked service as the app supports.",
            "Suited to cafés and restaurants that run on table numbers and floor sections.",
            "Website: optional add-on — hosted menu page or own-domain site — ask us for pricing when you subscribe with this plan.",
        ],
    },
    {
        "price_inr": 799,
        "headline": "Manage admins",
        "summary": "Multiple admins with per-person permissions; website included free.",
        "bullets": [
            "Includes the full Tables plan (₹499): floor and table workflows plus everything in the Starter (₹299) tier.",
            "Several admin or manager accounts, each with their own login.",
            "Granular permissions you can mix per person, for example: edit menu and prices, 86 items, inventory and stock, purchase or waste logs, expenses, payroll and staff roster, reports and exports, global settings and modules, QR menu and integrations — grant only what each role needs.",
            "Restaurant website is included free with this plan — hosted guest-facing page and web presence; own-domain delivery where we activate it — no extra website charge.",
        ],
    },
    {
        "price_inr": 1599,
        "headline": "Multiple restaurants",
        "summary": "Several venues under one account; website included free.",
        "bullets": [
            "Run multiple restaurant locations from one subscription: coordinated operations and reporting across venues (exact rollout with us).",
            "Staff and admin patterns aligned with the Manage admins (₹799) tier, scaled for multi-outlet teams.",
            "Restaurant website is included free for this tier — hosted site and guest-facing web for your brand; own-domain options where we include them — no extra website charge.",
        ],
    },
]

BADGE_EMOJI = ["🍽️", "📦", "🪑", "👨‍🍳", "🍲", "🍴"]

# Accent ring colours (ItemBadge.kt AccentRingColors) as CSS hex
ACCENT_RING = ["#C2543D", "#3D6B58", "#D4A574", "#5B8FA8", "#E8A598", "#8B7355"]


def _format_inr(n: int) -> str:
    return "₹{:,}".format(n)


def _upi_query(vpa: str, amount_inr: int, plan_headline: str) -> str:
    return urllib.parse.urlencode(
        {
            "pa": vpa,
            "pn": "Restaurant",
            "am": str(amount_inr),
            "cu": "INR",
            "tn": ("Plan: " + plan_headline)[:80],
        }
    )


def operations_hub(request):
    vpa = str(getattr(settings, "SUBSCRIPTION_UPI_VPA", "") or "").strip() or "9182351381@ybl"
    entries = []
    for i, row in enumerate(OPERATIONS_ENTRIES):
        entries.append(
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
        "venue/operations_hub.html",
        {
            "entries": entries,
            "plans": plans,
            "upi_vpa": vpa,
        },
    )
