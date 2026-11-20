# AlarmApp (Android)

## Build
Open this folder in Android Studio, let Gradle sync, press Run.

## Connect to the backend
1. Start the backend on your computer: `uvicorn main:app --host 0.0.0.0 --reload`
2. In the app: Connected apps -> set Server URL to `http://<your-computer-Wi-Fi-IP>:8000`
   (phone and computer on the same Wi-Fi; emulator default `http://10.0.2.2:8000` already works).
3. The screen shows your 6-digit pair code. Use it with `POST /v1/connections`.

## Real push notifications (optional at first)
Without Firebase the app still registers and syncs alarms when opened; pushes just don't arrive live.
1. console.firebase.google.com -> create a project -> add an Android app with package `com.example.alarmapp`.
2. Download `google-services.json` into the `app/` folder, then Sync Gradle.
3. Backend: Project settings -> Service accounts -> Generate private key, then
   `export GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json` before starting uvicorn.

## Dev-only settings to change before release
- `android:usesCleartextTraffic="true"` in the manifest (use https instead).
- The default `API_BASE_URL` in `app/build.gradle.kts`.
