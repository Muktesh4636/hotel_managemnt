from django.contrib.auth import authenticate
from django.contrib.auth.models import User
from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.permissions import AllowAny, IsAdminUser, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .serializers import UserSummarySerializer


class LoginView(APIView):
    """
    POST JSON: { "login_id": "<phone or username>", "password": "..." }
    (alias: "username" instead of "login_id" is accepted.)

    Returns an API token for the mobile app. Users must exist and be active;
    create them only in Django Admin unless you add a public register endpoint.
    """

    permission_classes = [AllowAny]

    def post(self, request):
        raw = (
            request.data.get("login_id")
            or request.data.get("username")
            or ""
        ).strip()
        password = request.data.get("password") or ""
        if not raw or not password:
            return Response(
                {"detail": "login_id (or username) and password are required."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        # Match mobile app: phone-like → digits only; else lowercase
        login_key = _normalize_login_id(raw)
        user = authenticate(request, username=login_key, password=password)
        if user is None:
            user = authenticate(request, username=raw, password=password)
        if user is None or not user.is_active:
            return Response(
                {"detail": "Invalid credentials or account disabled."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        token, _ = Token.objects.get_or_create(user=user)
        return Response(
            {
                "token": token.key,
                "user": UserSummarySerializer(user).data,
            }
        )


class LogoutView(APIView):
    """POST: delete the caller's token (sign out this device)."""

    permission_classes = [IsAuthenticated]

    def post(self, request):
        Token.objects.filter(user=request.user).delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class MeView(APIView):
    """GET: current user (for app startup checks)."""

    permission_classes = [IsAuthenticated]

    def get(self, request):
        return Response(UserSummarySerializer(request.user).data)


class UserListView(APIView):
    """
    GET: list all users (staff / superuser only).
    Central control from your own admin tools or the browsable API.
    """

    permission_classes = [IsAdminUser]

    def get(self, request):
        qs = User.objects.all().order_by("id")
        return Response(UserSummarySerializer(qs, many=True).data)


def _normalize_login_id(raw: str) -> str:
    t = raw.strip()
    if not t:
        return ""
    digits = "".join(ch for ch in t if ch.isdigit())
    phone_like = len(digits) >= 10 and all(
        ch.isdigit() or ch in "+-() " for ch in t
    )
    if phone_like:
        return digits
    return t.lower()
