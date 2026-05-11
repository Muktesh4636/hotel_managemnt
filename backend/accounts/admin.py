from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as DjangoUserAdmin
from django.contrib.auth.models import User

# Replace default user admin with a list tuned for operator control.
admin.site.unregister(User)


@admin.register(User)
class UserAdmin(DjangoUserAdmin):
    """All staff and mobile users are controlled here (add, edit, deactivate, reset password)."""

    list_display = (
        "username",
        "email",
        "first_name",
        "last_name",
        "is_staff",
        "is_superuser",
        "is_active",
        "last_login",
        "date_joined",
    )
    list_filter = ("is_staff", "is_superuser", "is_active")
    search_fields = ("username", "email", "first_name", "last_name")
    ordering = ("username",)
