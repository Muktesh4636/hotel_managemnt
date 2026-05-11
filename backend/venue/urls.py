from django.urls import include, path
from rest_framework.routers import DefaultRouter

from . import views
from . import views_guest
from .views_settings import VenueSettingsApi

router = DefaultRouter()
router.register(r"tables", views.VenueTableViewSet, basename="table")
router.register(r"menu-items", views.MenuItemViewSet, basename="menuitem")
router.register(r"orders", views.VenueOrderViewSet, basename="order")
router.register(r"order-lines", views.OrderLineViewSet, basename="orderline")
router.register(r"inventory", views.InventoryItemViewSet, basename="inventory")
router.register(r"staff", views.StaffMemberViewSet, basename="staff")
router.register(r"staff-absences", views.StaffAbsenceViewSet, basename="staffabsence")
router.register(r"expenses", views.ExpenseViewSet, basename="expense")

urlpatterns = [
    path("", include(router.urls)),
    path("guest/menu/", views_guest.guest_menu, name="guest-menu"),
    path("guest/orders/", views_guest.guest_place_order, name="guest-place-order"),
    path("sync/full/", views.sync_full, name="sync-full"),
    path("sync/seed-defaults/", views.sync_seed_defaults, name="sync-seed-defaults"),
    path(
        "settings/venue/",
        VenueSettingsApi.as_view(),
        name="venue-settings",
    ),
]
