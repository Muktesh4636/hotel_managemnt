from django.utils import timezone
from rest_framework import serializers

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


class VenueTableSerializer(serializers.ModelSerializer):
    class Meta:
        model = VenueTable
        fields = ("id", "label", "section", "status")


class MenuItemSerializer(serializers.ModelSerializer):
    """API field `price` is amount in smallest currency unit (paise), stored as `price_cents` on the model."""

    price = serializers.IntegerField(source="price_cents", min_value=0)
    image_url = serializers.SerializerMethodField()

    class Meta:
        model = MenuItem
        fields = (
            "id",
            "name",
            "category",
            "price",
            "is_available",
            "custom_photo_url",
            "image_url",
        )

    def get_image_url(self, obj: MenuItem) -> str:
        """Absolute URL for uploaded photos; empty for legacy local paths (APK-only)."""
        request = self.context.get("request")
        raw = (obj.custom_photo_url or "").strip()
        if not raw:
            return ""
        if raw.startswith("http://") or raw.startswith("https://"):
            return raw
        if raw.startswith("/media/"):
            if request:
                return request.build_absolute_uri(raw)
            return raw
        return ""


class OrderLineReadSerializer(serializers.ModelSerializer):
    class Meta:
        model = OrderLine
        fields = (
            "id",
            "menu_item_id",
            "quantity",
            "unit_price_cents",
            "kitchen_status",
        )


class VenueOrderPatchSerializer(serializers.ModelSerializer):
    class Meta:
        model = VenueOrder
        fields = ("status", "notes", "total_cents", "table_id")


class VenueOrderReadSerializer(serializers.ModelSerializer):
    lines = OrderLineReadSerializer(many=True, read_only=True)
    table_id = serializers.IntegerField(allow_null=True, read_only=True)

    class Meta:
        model = VenueOrder
        fields = (
            "id",
            "table_id",
            "status",
            "created_at_epoch_millis",
            "total_cents",
            "notes",
            "lines",
        )


class OrderLineWriteSerializer(serializers.Serializer):
    menu_item_id = serializers.IntegerField()
    quantity = serializers.IntegerField(min_value=1)
    unit_price_cents = serializers.IntegerField(required=False, min_value=0)
    kitchen_status = serializers.CharField(required=False, default="QUEUED")


class VenueOrderWriteSerializer(serializers.ModelSerializer):
    lines = OrderLineWriteSerializer(many=True, write_only=True)
    table_id = serializers.PrimaryKeyRelatedField(
        source="table",
        queryset=VenueTable.objects.none(),
        allow_null=True,
        required=False,
    )

    class Meta:
        model = VenueOrder
        fields = (
            "id",
            "table_id",
            "status",
            "created_at_epoch_millis",
            "total_cents",
            "notes",
            "lines",
        )
        read_only_fields = ("id", "total_cents")

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        request = self.context.get("request")
        if request and request.user.is_authenticated:
            self.fields["table_id"].queryset = VenueTable.objects.filter(owner=request.user)

    def create(self, validated_data):
        lines_data = validated_data.pop("lines")
        request = self.context["request"]
        user = request.user
        if not validated_data.get("created_at_epoch_millis"):
            validated_data["created_at_epoch_millis"] = int(
                timezone.now().timestamp() * 1000,
            )
        order = VenueOrder.objects.create(owner=user, **validated_data)
        total = 0
        for ld in lines_data:
            mi = MenuItem.objects.get(pk=ld["menu_item_id"], owner=user)
            qty = ld["quantity"]
            unit = ld.get("unit_price_cents")
            if unit is None:
                unit = mi.price_cents
            total += unit * qty
            OrderLine.objects.create(
                order=order,
                menu_item=mi,
                quantity=qty,
                unit_price_cents=unit,
                kitchen_status=ld.get("kitchen_status", "QUEUED"),
            )
        order.total_cents = total
        order.save(update_fields=["total_cents"])
        return order

    def update(self, instance, validated_data):
        lines_data = validated_data.pop("lines", None)
        for attr, val in validated_data.items():
            setattr(instance, attr, val)
        instance.save()
        if lines_data is not None:
            instance.lines.all().delete()
            user = self.context["request"].user
            total = 0
            for ld in lines_data:
                mi = MenuItem.objects.get(pk=ld["menu_item_id"], owner=user)
                qty = ld["quantity"]
                unit = ld.get("unit_price_cents")
                if unit is None:
                    unit = mi.price_cents
                total += unit * qty
                OrderLine.objects.create(
                    order=instance,
                    menu_item=mi,
                    quantity=qty,
                    unit_price_cents=unit,
                    kitchen_status=ld.get("kitchen_status", "QUEUED"),
                )
            instance.total_cents = total
            instance.save(update_fields=["total_cents"])
        return instance


class OrderLinePatchSerializer(serializers.ModelSerializer):
    """PATCH kitchen line status and/or quantity (quantity ≥ 1; use DELETE to remove a line)."""

    class Meta:
        model = OrderLine
        fields = ("kitchen_status", "quantity")

    def validate_quantity(self, value: int) -> int:
        if value < 1:
            raise serializers.ValidationError("Quantity must be at least 1. Remove the line with DELETE instead.")
        return value


class OrderLineCreateSerializer(serializers.Serializer):
    """Add a line to an existing order (kitchen / POS corrections)."""

    order_id = serializers.IntegerField()
    menu_item_id = serializers.IntegerField()
    quantity = serializers.IntegerField(min_value=1, max_value=99)
    kitchen_status = serializers.CharField(required=False, default="QUEUED", max_length=32)

    def create(self, validated_data):
        request = self.context["request"]
        user = request.user
        try:
            order = VenueOrder.objects.get(pk=validated_data["order_id"], owner=user)
        except VenueOrder.DoesNotExist as exc:
            raise serializers.ValidationError({"order_id": "Unknown order."}) from exc
        if order.status in ("PAID", "CANCELLED"):
            raise serializers.ValidationError({"order_id": "Cannot edit this order."})
        try:
            mi = MenuItem.objects.get(pk=validated_data["menu_item_id"], owner=user, is_available=True)
        except MenuItem.DoesNotExist as exc:
            raise serializers.ValidationError({"menu_item_id": "Invalid or unavailable item."}) from exc
        return OrderLine.objects.create(
            order=order,
            menu_item=mi,
            quantity=validated_data["quantity"],
            unit_price_cents=mi.price_cents,
            kitchen_status=validated_data.get("kitchen_status", "QUEUED"),
        )


class InventoryItemSerializer(serializers.ModelSerializer):
    class Meta:
        model = InventoryItem
        fields = ("id", "name", "quantity", "unit", "low_stock_threshold")


class StaffMemberSerializer(serializers.ModelSerializer):
    class Meta:
        model = StaffMember
        fields = ("id", "name", "role", "on_shift", "salary_cents")


class StaffAbsenceSerializer(serializers.ModelSerializer):
    staff_id = serializers.PrimaryKeyRelatedField(
        source="staff",
        queryset=StaffMember.objects.none(),
    )

    class Meta:
        model = StaffAbsence
        fields = ("id", "staff_id", "day_start_epoch_millis", "note")

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        request = self.context.get("request")
        if request and request.user.is_authenticated:
            self.fields["staff_id"].queryset = StaffMember.objects.filter(owner=request.user)


class ExpenseSerializer(serializers.ModelSerializer):
    class Meta:
        model = Expense
        fields = (
            "id",
            "expense_category",
            "label",
            "amount_cents",
            "note",
            "created_at_epoch_millis",
        )


class VenueSettingsSerializer(serializers.ModelSerializer):
    class Meta:
        model = VenueSettings
        fields = (
            "venue_name",
            "tax_percent",
            "service_charge_percent",
            "qr_menu_token",
            "menu_categories",
            "expense_categories",
            "modules_json",
        )
