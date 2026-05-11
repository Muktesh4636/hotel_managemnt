"""Create or update a Django user for POST /api/v1/auth/login/ (phone digits or lowercase username)."""

from django.contrib.auth.models import User
from django.core.management.base import BaseCommand


class Command(BaseCommand):
    help = (
        "Create or reset a user for API/mobile login. "
        "Username should match app normalization (e.g. phone as digits only)."
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
            help="Plain password to set (default: 123456)",
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
        user.set_password(password)
        user.is_active = True
        user.save()
        action = "Created" if created else "Updated password for"
        self.stdout.write(self.style.SUCCESS(f"{action} user {username!r}"))
