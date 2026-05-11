from django.conf import settings
from django.db import models


class VenueTable(models.Model):
    owner = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="venue_tables",
    )
    label = models.CharField(max_length=200)
    section = models.CharField(max_length=200, default="Main")
    status = models.CharField(max_length=32, default="FREE")

    class Meta:
        ordering = ("section", "label")


class MenuItem(models.Model):
    owner = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="menu_items",
    )
    name = models.CharField(max_length=400)
    category = models.CharField(max_length=200)
    price_cents = models.IntegerField(default=0)
    is_available = models.BooleanField(default=True)
    custom_photo_url = models.CharField(max_length=2000, blank=True, default="")

    class Meta:
        ordering = ("category", "name")


class VenueOrder(models.Model):
    owner = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="venue_orders",
    )
    table = models.ForeignKey(
        VenueTable,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="orders",
    )
    status = models.CharField(max_length=32, default="OPEN")
    created_at_epoch_millis = models.BigIntegerField(default=0)
    total_cents = models.IntegerField(default=0)
    notes = models.CharField(max_length=2000, null=True, blank=True)

    class Meta:
        ordering = ("-created_at_epoch_millis",)


class OrderLine(models.Model):
    order = models.ForeignKey(
        VenueOrder,
        on_delete=models.CASCADE,
        related_name="lines",
    )
    menu_item = models.ForeignKey(MenuItem, on_delete=models.CASCADE)
    quantity = models.IntegerField()
    unit_price_cents = models.IntegerField()
    kitchen_status = models.CharField(max_length=32, default="QUEUED")


class InventoryItem(models.Model):
    owner = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="inventory_items",
    )
    name = models.CharField(max_length=400)
    quantity = models.FloatField(default=0.0)
    unit = models.CharField(max_length=64, default="kg")
    low_stock_threshold = models.FloatField(default=0.0)


class StaffMember(models.Model):
    owner = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="staff_members",
    )
    name = models.CharField(max_length=200)
    role = models.CharField(max_length=200, default="Staff")
    on_shift = models.BooleanField(default=False)
    salary_cents = models.IntegerField(default=0)


class StaffAbsence(models.Model):
    staff = models.ForeignKey(
        StaffMember,
        on_delete=models.CASCADE,
        related_name="absences",
    )
    day_start_epoch_millis = models.BigIntegerField()
    note = models.CharField(max_length=2000, blank=True, null=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=("staff", "day_start_epoch_millis"),
                name="venue_staffabsence_staff_day",
            ),
        ]


class Expense(models.Model):
    owner = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="expenses",
    )
    expense_category = models.CharField(max_length=200, default="")
    label = models.CharField(max_length=400)
    amount_cents = models.IntegerField()
    note = models.CharField(max_length=2000, blank=True, null=True)
    created_at_epoch_millis = models.BigIntegerField()


class VenueSettings(models.Model):
    owner = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="venue_settings",
    )
    venue_name = models.CharField(max_length=400, default="My Restaurant")
    tax_percent = models.FloatField(default=5.0)
    service_charge_percent = models.FloatField(default=0.0)
    qr_menu_token = models.CharField(max_length=200, blank=True, default="")
    menu_categories = models.TextField(blank=True, default="")
    expense_categories = models.TextField(blank=True, default="")
    modules_json = models.TextField(blank=True, default="{}")
