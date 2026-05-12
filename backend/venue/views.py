from pathlib import Path

from django.conf import settings as django_settings
from django.db import transaction
from django.utils import timezone
from rest_framework import status, viewsets
from rest_framework.decorators import action, api_view, permission_classes
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response

from .models import (
    Expense,
    InventoryItem,
    MenuItem,
    OrderLine,
    StaffAbsence,
    StaffMember,
    VenueOrder,
    VenueSettings,
    VenueTable,
)
from .serializers import (
    ExpenseSerializer,
    InventoryItemSerializer,
    MenuItemSerializer,
    OrderLineCreateSerializer,
    OrderLinePatchSerializer,
    OrderLineReadSerializer,
    StaffAbsenceSerializer,
    StaffMemberSerializer,
    VenueOrderPatchSerializer,
    VenueOrderReadSerializer,
    VenueOrderWriteSerializer,
    VenueSettingsSerializer,
    VenueTableSerializer,
)


class OwnerScopedViewSet(viewsets.ModelViewSet):
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        qs = super().get_queryset()
        return qs.filter(owner=self.request.user)

    def perform_create(self, serializer):
        serializer.save(owner=self.request.user)


class VenueTableViewSet(OwnerScopedViewSet):
    queryset = VenueTable.objects.all()
    serializer_class = VenueTableSerializer


class MenuItemViewSet(OwnerScopedViewSet):
    queryset = MenuItem.objects.all()
    serializer_class = MenuItemSerializer

    @action(
        detail=True,
        methods=["post"],
        parser_classes=[MultiPartParser, FormParser],
    )
    def photo(self, request, pk=None):
        """Upload a JPEG/PNG/WebP; stored under MEDIA and exposed via image_url on sync."""
        item = self.get_object()
        up = request.FILES.get("file") or request.FILES.get("photo")
        if not up:
            return Response(
                {"detail": "Multipart field 'file' or 'photo' is required."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        ext = Path(up.name).suffix.lower() or ".jpg"
        if ext not in (".jpg", ".jpeg", ".png", ".webp"):
            ext = ".jpg"
        user_dir = Path(django_settings.MEDIA_ROOT) / "menu_photos" / str(request.user.id)
        user_dir.mkdir(parents=True, exist_ok=True)
        dest = user_dir / f"{item.id}{ext}"
        with dest.open("wb") as f:
            for chunk in up.chunks():
                f.write(chunk)
        rel = f"/media/menu_photos/{request.user.id}/{item.id}{ext}"
        item.custom_photo_url = rel
        item.save(update_fields=["custom_photo_url"])
        return Response(
            MenuItemSerializer(item, context={"request": request}).data,
            status=status.HTTP_200_OK,
        )


class VenueOrderViewSet(OwnerScopedViewSet):
    queryset = VenueOrder.objects.prefetch_related("lines").all()

    def get_serializer_class(self):
        if self.action == "partial_update":
            return VenueOrderPatchSerializer
        if self.action in ("create", "update"):
            return VenueOrderWriteSerializer
        return VenueOrderReadSerializer

    def perform_create(self, serializer):
        serializer.save()


def _recompute_venue_order_total(order: VenueOrder) -> None:
    total = sum(ln.quantity * ln.unit_price_cents for ln in order.lines.all())
    if order.total_cents != total:
        order.total_cents = total
        order.save(update_fields=["total_cents"])


class OrderLineViewSet(viewsets.ModelViewSet):
    """List/read lines; PATCH status/quantity; POST to add a line; DELETE to remove."""

    permission_classes = [IsAuthenticated]
    http_method_names = ["get", "post", "patch", "delete", "head", "options"]

    def get_queryset(self):
        return OrderLine.objects.filter(order__owner=self.request.user)

    def get_serializer_class(self):
        if self.action == "create":
            return OrderLineCreateSerializer
        if self.action in ("update", "partial_update"):
            return OrderLinePatchSerializer
        return OrderLineReadSerializer

    def create(self, request, *args, **kwargs):
        serializer = OrderLineCreateSerializer(data=request.data, context=self.get_serializer_context())
        serializer.is_valid(raise_exception=True)
        line = serializer.save()
        _recompute_venue_order_total(line.order)
        return Response(OrderLineReadSerializer(line).data, status=status.HTTP_201_CREATED)

    def perform_update(self, serializer):
        super().perform_update(serializer)
        _recompute_venue_order_total(serializer.instance.order)

    def perform_destroy(self, instance):
        order = instance.order
        super().perform_destroy(instance)
        _recompute_venue_order_total(order)


class InventoryItemViewSet(OwnerScopedViewSet):
    queryset = InventoryItem.objects.all()
    serializer_class = InventoryItemSerializer


class StaffMemberViewSet(OwnerScopedViewSet):
    queryset = StaffMember.objects.all()
    serializer_class = StaffMemberSerializer


class StaffAbsenceViewSet(viewsets.ModelViewSet):
    permission_classes = [IsAuthenticated]
    serializer_class = StaffAbsenceSerializer

    def get_queryset(self):
        return StaffAbsence.objects.filter(staff__owner=self.request.user)


class ExpenseViewSet(OwnerScopedViewSet):
    queryset = Expense.objects.all()
    serializer_class = ExpenseSerializer

    def perform_create(self, serializer):
        extra = {"owner": self.request.user}
        if "created_at_epoch_millis" not in serializer.validated_data:
            extra["created_at_epoch_millis"] = int(timezone.now().timestamp() * 1000)
        serializer.save(**extra)


@api_view(["GET"])
@permission_classes([IsAuthenticated])
def sync_full(request):
    user = request.user
    tables = VenueTable.objects.filter(owner=user)
    menu = MenuItem.objects.filter(owner=user)
    orders = VenueOrder.objects.filter(owner=user).prefetch_related("lines")
    inv = InventoryItem.objects.filter(owner=user)
    staff = StaffMember.objects.filter(owner=user)
    absences = StaffAbsence.objects.filter(staff__owner=user)
    expenses = Expense.objects.filter(owner=user)
    settings_obj, _ = VenueSettings.objects.get_or_create(
        owner=user,
        defaults={"venue_name": "My Restaurant"},
    )

    def line_dict(ln: OrderLine):
        return {
            "id": ln.id,
            "menu_item_id": ln.menu_item_id,
            "quantity": ln.quantity,
            "unit_price_cents": ln.unit_price_cents,
            "kitchen_status": ln.kitchen_status,
        }

    def order_dict(o: VenueOrder):
        return {
            "id": o.id,
            "table_id": o.table_id,
            "status": o.status,
            "created_at_epoch_millis": o.created_at_epoch_millis,
            "total_cents": o.total_cents,
            "notes": o.notes,
            "lines": [line_dict(l) for l in o.lines.all()],
        }

    return Response(
        {
            "tables": VenueTableSerializer(tables, many=True).data,
            "menu_items": MenuItemSerializer(menu, many=True, context={"request": request}).data,
            "orders": [order_dict(o) for o in orders],
            "inventory": InventoryItemSerializer(inv, many=True).data,
            "staff": StaffMemberSerializer(staff, many=True).data,
            "staff_absences": StaffAbsenceSerializer(absences, many=True).data,
            "expenses": ExpenseSerializer(expenses, many=True).data,
            "settings": VenueSettingsSerializer(settings_obj).data,
        },
    )


@api_view(["POST"])
@permission_classes([IsAuthenticated])
def sync_seed_defaults(request):
    user = request.user
    with transaction.atomic():
        if not VenueTable.objects.filter(owner=user).exists():
            rows = []
            for sec, labs in [
                ("Main", ["T1", "T2", "T3", "T4", "T5", "T6"]),
                ("Patio", ["P1", "P2", "P3", "P4"]),
                ("Bar", ["B1", "B2"]),
            ]:
                for lab in labs:
                    rows.append(
                        VenueTable(owner=user, label=lab, section=sec, status="FREE"),
                    )
            VenueTable.objects.bulk_create(rows)
        VenueSettings.objects.get_or_create(
            owner=user,
            defaults={"venue_name": "My Restaurant"},
        )
    return sync_full(request)
