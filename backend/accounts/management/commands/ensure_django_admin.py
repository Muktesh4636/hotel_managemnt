"""Create or promote a user so they can sign in at /admin/ (staff + superuser + password)."""

from django.contrib.auth.models import User
from django.core.management.base import BaseCommand


class Command(BaseCommand):
    help = (
        "Ensure a Django admin account exists: is_staff, is_superuser, active, password set. "
        "Use on the production server after deploy (API-only users from create_signin_user cannot access /admin/)."
    )

    def add_arguments(self, parser):
        parser.add_argument(
            "username",
            nargs="?",
            default="9182351381",
            help="Django User.username (default: 9182351381)",
        )
        parser.add_argument(
            "password",
            nargs="?",
            default="123456",
            help="Plain password (default: 123456; use a strong password in production)",
        )

    def handle(self, *args, **options):
        username = (options["username"] or "").strip()
        password = options["password"] or ""
        if not username:
            self.stderr.write(self.style.ERROR("username is required"))
            return
        if len(password) < 6:
            self.stderr.write(self.style.ERROR("password must be at least 6 characters"))
            return

        user, created = User.objects.get_or_create(
            username=username,
            defaults={"email": "", "is_active": True},
        )
        user.is_staff = True
        user.is_superuser = True
        user.is_active = True
        user.set_password(password)
        user.save()
        action = "Created" if created else "Updated"
        self.stdout.write(
            self.style.SUCCESS(
                f"{action} Django admin user {username!r} (staff + superuser). Sign in at /admin/",
            ),
        )
