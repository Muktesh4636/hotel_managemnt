"""
Public guest menu + ordering by venue QR token (no login).
Each restaurant's VenueSettings.qr_menu_token scopes menu and order creation.
"""
from __future__ import annotations

from django.db import transaction
from django.http import HttpResponseNotFound
from django.shortcuts import render
from django.templatetags.static import static as static_url
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt
from rest_framework import status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny
from rest_framework.response import Response

from .models import MenuItem, OrderLine, VenueOrder, VenueSettings
from .serializers import MenuItemSerializer


def _settings_for_token(token: str) -> VenueSettings | None:
    t = (token or "").strip()
    if not t:
        return None
    return VenueSettings.objects.filter(qr_menu_token=t).select_related("owner").first()


@api_view(["GET"])
@permission_classes([AllowAny])
def guest_menu(request):
    """Return venue name and available menu for a valid qr_menu_token."""
    token = (request.GET.get("token") or "").strip()
    s = _settings_for_token(token)
    if not s:
        return Response({"detail": "Unknown or missing menu link."}, status=status.HTTP_404_NOT_FOUND)
    qs = MenuItem.objects.filter(owner=s.owner, is_available=True).order_by("category", "name")
    data = MenuItemSerializer(qs, many=True, context={"request": request}).data
    return Response(
        {
            "venue_name": s.venue_name,
            "qr_menu_token": token,
            "menu_items": data,
        },
    )


@csrf_exempt
@api_view(["POST"])
@permission_classes([AllowAny])
def guest_place_order(request):
    """
    Create a kitchen ticket for the venue identified by qr_menu_token.
    Body: {"qr_menu_token": "<uuid>", "lines": [{"menu_item_id": 1, "quantity": 2}, ...]}
    """
    body = request.data if isinstance(request.data, dict) else {}
    token = (body.get("qr_menu_token") or "").strip()
    s = _settings_for_token(token)
    if not s:
        return Response({"detail": "Unknown or missing menu link."}, status=status.HTTP_404_NOT_FOUND)

    lines_in = body.get("lines")
    if not isinstance(lines_in, list) or len(lines_in) == 0:
        return Response(
            {"detail": "lines must be a non-empty list."},
            status=status.HTTP_400_BAD_REQUEST,
        )
    if len(lines_in) > 80:
        return Response({"detail": "Too many lines in one order."}, status=status.HTTP_400_BAD_REQUEST)

    owner = s.owner
    try:
        with transaction.atomic():
            order = VenueOrder.objects.create(
                owner=owner,
                table=None,
                status="IN_KITCHEN",
                created_at_epoch_millis=int(timezone.now().timestamp() * 1000),
                total_cents=0,
                notes="QR guest web",
            )
            total = 0
            for row in lines_in:
                if not isinstance(row, dict):
                    raise ValueError("bad line")
                mid = int(row.get("menu_item_id"))
                qty = int(row.get("quantity", 0))
                if qty < 1 or qty > 99:
                    raise ValueError("bad quantity")
                mi = MenuItem.objects.select_for_update().get(pk=mid, owner=owner, is_available=True)
                unit = mi.price_cents
                total += unit * qty
                OrderLine.objects.create(
                    order=order,
                    menu_item=mi,
                    quantity=qty,
                    unit_price_cents=unit,
                    kitchen_status="QUEUED",
                )
            order.total_cents = total
            order.save(update_fields=["total_cents"])
    except MenuItem.DoesNotExist:
        return Response(
            {"detail": "One or more dishes are not on this menu or are unavailable."},
            status=status.HTTP_400_BAD_REQUEST,
        )
    except (TypeError, ValueError, KeyError):
        return Response(
            {"detail": "Invalid order payload. Use menu_item_id and quantity per line."},
            status=status.HTTP_400_BAD_REQUEST,
        )

    return Response({"ok": True, "order_id": order.id, "total_cents": order.total_cents})


def guest_menu_page(request, qr_token: str):
    """HTML guest ordering page (opened from QR code URL)."""
    token = (qr_token or "").strip()
    if not token or len(token) > 200:
        return HttpResponseNotFound("Invalid link.")
    if not _settings_for_token(token):
        return HttpResponseNotFound("This menu link is not valid.")
    return render(
        request,
        "venue/guest_menu.html",
        {
            "qr_token": token,
            "food_static_base": static_url("venue/food/"),
        },
    )
