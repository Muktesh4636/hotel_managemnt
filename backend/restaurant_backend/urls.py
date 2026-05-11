from django.contrib import admin
from django.urls import include, path

from venue.views_operations import operations_hub
from venue.views_web_app import web_app, web_app_manifest

urlpatterns = [
    path("admin/", admin.site.urls),
    path("manifest.webmanifest", web_app_manifest, name="web-app-manifest"),
    path("", web_app, name="web-app-root"),
    path("app/", web_app, name="web-app"),
    path("operations/", operations_hub, name="operations-hub"),
    path("api/v1/", include("accounts.urls")),
    path("api/v1/", include("venue.urls")),
]
