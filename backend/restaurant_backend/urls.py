from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.urls import include, path

from venue.views_download import download_android_apk
from venue.views_guest import guest_menu_page
from venue.views_operations import operations_hub
from venue.views_web_app import web_app, web_app_manifest

urlpatterns = [
    path("admin/", admin.site.urls),
    path("menu/<str:qr_token>/", guest_menu_page, name="guest-menu-page"),
    path("manifest.webmanifest", web_app_manifest, name="web-app-manifest"),
    path("", web_app, name="web-app-root"),
    path("app/", web_app, name="web-app"),
    # Deep links for the main CRM shell (same template as / and /app/).
    path("sign-in/", web_app, name="web-sign-in"),
    path("dashboard/", web_app, name="web-dashboard"),
    path("pos/", web_app, name="web-pos"),
    path("kitchen/", web_app, name="web-kitchen"),
    path("orders/", web_app, name="web-orders"),
    path("reports/", web_app, name="web-reports"),
    path("tables/", web_app, name="web-tables"),
    # /workspace/<slug>/ must be registered before /workspace/ so module paths match.
    path("workspace/<slug:workspace_module>/", web_app, name="web-workspace-module"),
    path("workspace/", web_app, name="web-workspace"),
    path("operations/", operations_hub, name="operations-hub"),
    path("download/android-app/", download_android_apk, name="download-android-apk"),
    path("api/v1/", include("accounts.urls")),
    path("api/v1/", include("venue.urls")),
]
if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
