from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import VenueSettings
from .serializers import VenueSettingsSerializer


class VenueSettingsApi(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        obj, _ = VenueSettings.objects.get_or_create(
            owner=request.user,
            defaults={"venue_name": "My Restaurant"},
        )
        return Response(VenueSettingsSerializer(obj).data)

    def put(self, request):
        obj, _ = VenueSettings.objects.get_or_create(
            owner=request.user,
            defaults={"venue_name": "My Restaurant"},
        )
        ser = VenueSettingsSerializer(obj, data=request.data, partial=True)
        ser.is_valid(raise_exception=True)
        ser.save()
        return Response(ser.data)
