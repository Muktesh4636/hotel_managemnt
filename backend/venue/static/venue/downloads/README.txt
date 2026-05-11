The website sidebar “Download Android app” serves this file as restaurant-crm.apk.

Refresh the APK after each Android build (from this folder):
  cp ../../../../android/app/build/outputs/apk/debug/app-debug.apk ./restaurant-crm.apk
  (release: use outputs/apk/release/*.apk instead of debug)

Then deploy or: python manage.py collectstatic

Alternatively set RESTAURANT_APK_PATH on the server to an absolute path to any .apk file.
