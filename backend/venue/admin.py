from django.contrib import admin

from .models import (
    Expense,
    InventoryItem,
    MenuItem,
    OrderLine,
    StaffAbsence,
    StaffMember,
    TableReservation,
    VenueOrder,
    VenueSettings,
    VenueTable,
)


@admin.register(VenueTable)
class VenueTableAdmin(admin.ModelAdmin):
    list_display = ("id", "owner", "label", "section", "status")
    list_filter = ("section", "status")
    search_fields = ("label", "owner__username")


@admin.register(TableReservation)
class TableReservationAdmin(admin.ModelAdmin):
    list_display = ("id", "owner", "guest_name", "party_size", "table", "status", "start_epoch_millis")
    list_filter = ("status",)
    search_fields = ("guest_name", "phone", "owner__username")


@admin.register(MenuItem)
class MenuItemAdmin(admin.ModelAdmin):
    list_display = ("id", "owner", "name", "category", "price_cents", "is_available")
    search_fields = ("name", "owner__username")


class OrderLineInline(admin.TabularInline):
    model = OrderLine
    extra = 0


@admin.register(VenueOrder)
class VenueOrderAdmin(admin.ModelAdmin):
    list_display = ("id", "owner", "status", "total_cents", "created_at_epoch_millis")
    list_filter = ("status",)
    inlines = [OrderLineInline]


@admin.register(InventoryItem)
class InventoryItemAdmin(admin.ModelAdmin):
    list_display = ("id", "owner", "name", "quantity", "unit")


@admin.register(StaffMember)
class StaffMemberAdmin(admin.ModelAdmin):
    list_display = ("id", "owner", "name", "role", "on_shift", "salary_cents")


@admin.register(StaffAbsence)
class StaffAbsenceAdmin(admin.ModelAdmin):
    list_display = ("id", "staff", "day_start_epoch_millis")


@admin.register(Expense)
class ExpenseAdmin(admin.ModelAdmin):
    list_display = ("id", "owner", "label", "amount_cents", "created_at_epoch_millis")


@admin.register(VenueSettings)
class VenueSettingsAdmin(admin.ModelAdmin):
    list_display = ("owner", "venue_name", "tax_percent")
