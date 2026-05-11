from django.db import transaction
from django.utils import timezone
from rest_framework import viewsets
from rest_framework.decorators import api_view, permission_classes
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
    OrderLinePatchSerializer,
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


class OrderLineViewSet(viewsets.ModelViewSet):
    permission_classes = [IsAuthenticated]
    serializer_class = OrderLinePatchSerializer
    http_method_names = ["get", "patch", "head", "options"]

    def get_queryset(self):
        return OrderLine.objects.filter(order__owner=self.request.user)


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
            "menu_items": MenuItemSerializer(menu, many=True).data,
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
