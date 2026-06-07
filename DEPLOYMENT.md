# VisionTrack — Deployment Guide

## Release APK Build

### 1. Create a signing keystore
```bash
keytool -genkey -v \
  -keystore visiontrack-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias visiontrack
```

### 2. Configure signing in `app/build.gradle.kts`
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile     = file("../visiontrack-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias      = "visiontrack"
            keyPassword   = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 3. Build release APK
```bash
export KEYSTORE_PASSWORD=your_keystore_password
export KEY_PASSWORD=your_key_password

./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### 4. Build Android App Bundle (for Play Store)
```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

---

## APK Size Optimization

The following techniques are applied in `app/build.gradle.kts`:

| Technique            | Saving     |
|----------------------|------------|
| R8 minification      | ~35%       |
| Resource shrinking   | ~15%       |
| ABI split (arm64+x86_64 only) | ~40% |
| TFLite noCompress    | No overhead|
| ProGuard rules       | Keeps TFLite/Firebase |

Expected release APK size: **~18–22 MB**

---

## Firebase Production Checklist

- [ ] Set Firestore security rules (restrict to authenticated users)
- [ ] Enable Crashlytics collection in release builds
- [ ] Set Analytics collection enabled in release
- [ ] Configure FCM topics for alert notifications
- [ ] Set up Firebase App Check for API abuse prevention
- [ ] Enable App Check attestation (Play Integrity / DeviceCheck)

### Recommended Firestore Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Users can only read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    match /sessions/{sessionId} {
      allow read, write: if request.auth != null
        && resource.data.userId == request.auth.uid;

      match /detections/{detectionId} {
        allow read, write: if request.auth != null;
      }
    }
  }
}
```

---

## CI/CD (GitHub Actions)

Create `.github/workflows/android.yml`:

```yaml
name: VisionTrack CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ hashFiles('**/*.gradle*', '**/libs.versions.toml') }}

      - name: Create google-services.json
        run: echo '${{ secrets.GOOGLE_SERVICES_JSON }}' > app/google-services.json

      - name: Run unit tests
        run: ./gradlew testDebugUnitTest

      - name: Build debug APK
        run: ./gradlew assembleDebug

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

---

## Performance Monitoring Setup

### Battery consumption analysis
```bash
# Capture battery stats before/after 10-minute detection session
adb shell dumpsys batterystats --reset
# run the app for 10 minutes
adb shell dumpsys batterystats > batterystats.txt
python analyze_battery.py batterystats.txt
```

### Systrace for frame rendering
```bash
python $ANDROID_HOME/platform-tools/systrace/systrace.py \
  gfx view camera dalvik \
  -a com.visiontrack.app \
  -o trace.html
```

### GPU profiling (Android GPU Inspector)
1. Build with `debuggable = true` + `profileable` manifest tag
2. Connect Android GPU Inspector
3. Profile the `LiveDetectionScreen` frame timeline
4. Look for gaps between `ImageAnalysis` delivery and `BoundingBoxOverlay.onDraw`

---

## Node.js Analytics Backend (Optional)

```bash
cd backend/
npm install

# Configure environment
cp .env.example .env
# Edit .env:
#   MONGODB_URI=mongodb://localhost:27017/visiontrack
#   PORT=3000
#   JWT_SECRET=your_secret

npm run dev
```

### REST API Endpoints
| Method | Endpoint                    | Description              |
|--------|-----------------------------|--------------------------|
| POST   | /api/auth/register          | Create user              |
| POST   | /api/auth/login             | Login → JWT token        |
| GET    | /api/sessions               | List sessions            |
| POST   | /api/sessions               | Create session           |
| GET    | /api/sessions/:id/detections| Get detections           |
| GET    | /api/analytics/summary      | Dashboard metrics        |
| GET    | /api/analytics/top-objects  | Most detected classes    |

---

## Troubleshooting

| Issue                            | Fix                                                     |
|----------------------------------|---------------------------------------------------------|
| TFLite model not found           | Ensure `yolov8n.tflite` is in `assets/models/`         |
| GPU delegate crash               | Falls back to NNAPI → set `numThreads=4`               |
| CameraX black preview            | Check `CAMERA` permission is granted at runtime         |
| Firebase auth fails              | Verify `google-services.json` matches your package name|
| Room migration crash             | Increment DB version or use `fallbackToDestructiveMigration()` |
| Low FPS on emulator              | Emulator uses CPU only — test on physical device        |
