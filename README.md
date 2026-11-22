# AlarmApp (Android)

Build with GitHub Actions (or Android Studio): push to `main`, then download the APK from the Actions tab.

## Firebase (instant alarms)
1. Firebase Console -> your project -> add an Android app with package `com.example.alarmapp`.
2. Download `google-services.json` into the `app/` folder. The Gradle plugin switches on automatically
   when that file exists.
3. Open the app -> Settings -> "Instant alarms" should read Ready.

## Connect to the backend
Connected apps -> Server URL -> your server address (https://... once deployed) -> Save.
See the backend's DEPLOY.md.

## Dev-only settings to change before release
- `android:usesCleartextTraffic="true"` in the manifest (use https instead).
- The default `API_BASE_URL` in `app/build.gradle.kts`.
- The placeholder package name `com.example.alarmapp`.
