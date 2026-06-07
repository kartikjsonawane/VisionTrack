# VisionTrack 🎯

**Real-time AI object detection Android app powered by YOLOv8 TensorFlow Lite**

[![Android](https://img.shields.io/badge/Platform-Android%2013+-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![TensorFlow Lite](https://img.shields.io/badge/TFLite-YOLOv8n-FF6F00?style=flat-square&logo=tensorflow&logoColor=white)](https://www.tensorflow.org/lite)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

> Built by **[Kartik Sonawane](https://github.com/kartikjsonawane)**

---

## Overview

VisionTrack runs a **YOLOv8 nano** TFLite model entirely on-device using the phone's camera. It detects and tracks up to 80 COCO object classes in real time, overlays bounding boxes with confidence scores, tracks per-frame inference latency, and persists every detection session to a local Room database with optional Firebase cloud sync.

Tested on **OnePlus DN2101 (Android 13, arm64)** and **Pixel 6 emulator (API 34)**.

---

## Features

| | |
|---|---|
| 📷 Live CameraX feed with bounding box overlay | ⚡ ~25ms inference on ARM64 |
| 🗃️ Session history with per-session stats | 📊 Analytics — detections by hour and day |
| 🔐 Firebase Auth (email/password) | ☁️ Firestore cloud sync |
| 🎯 Adjustable confidence threshold (10–90%) | 🔔 Zone-based entry/exit alerting |
| 📈 Rolling FPS and latency HUD | 🧪 Demo mode when model file is absent |

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                  Jetpack Compose UI                       │
│  Splash · Login · Home · Detection · History · Analytics  │
└────────────────────┬─────────────────────────────────────┘
                     │ StateFlow / SharedFlow
┌────────────────────▼─────────────────────────────────────┐
│               ViewModels (@HiltViewModel)                 │
│  DetectionViewModel · HomeViewModel · HistoryViewModel    │
└──────────┬───────────────────────────┬────────────────────┘
           │ Use Cases                 │ Use Cases
┌──────────▼──────────┐   ┌────────────▼──────────────────┐
│     ML Pipeline      │   │        Data Layer             │
│                      │   │                               │
│  CameraX ImageAnalysis   │  Room DB                      │
│  DetectionAnalyzer   │   │  ├─ DetectionSessionDao       │
│  ObjectDetectionHelper   │  └─ DetectionDao              │
│  YOLOv8Detector      │   │                               │
│  ├─ TFLite Interpreter   │  Firebase                     │
│  ├─ INT8 quantized   │   │  ├─ FirebaseAuth              │
│  └─ Vectorized NMS   │   │  └─ Firestore                 │
└─────────────────────┘   └───────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| Camera | CameraX 1.4 — ImageAnalysis, RGBA_8888 |
| ML | YOLOv8n TFLite — INT8 quantized, 640×640 |
| DI | Hilt (Dagger) |
| Database | Room 2.6 + coroutine Flow |
| Auth & Sync | Firebase Auth + Firestore |
| Build | AGP 8.7.3 · Gradle 8.9 · KSP 2.0.21 |
| Min SDK | 26 (Android 8.0) · Target SDK 35 |

---

## Project Structure

```
app/src/main/java/com/visiontrack/
├── di/                        Hilt modules (DB, Firebase, ML, dispatchers)
├── domain/
│   ├── model/                 DetectedObject · DetectionSession · InferenceMetrics
│   ├── repository/            Repository interfaces
│   └── usecase/               DetectionUseCases · AuthUseCases
├── data/
│   ├── local/                 Room entities · DAOs · VisionTrackDatabase
│   ├── repository/            DetectionRepositoryImpl · AuthRepositoryImpl
│   └── mapper/                Entity <-> Domain mappers
├── ml/
│   ├── YOLOv8Detector.kt      TFLite engine · NMS · demo mode fallback
│   ├── ObjectDetectionHelper.kt  Frame processing · tracker · rolling metrics
│   └── BoundingBoxOverlay.kt  Custom Canvas overlay (zero-alloc per frame)
└── presentation/
    ├── detection/             LiveDetectionScreen · DetectionViewModel · DetectionAnalyzer
    ├── home/                  HomeScreen · HomeViewModel
    ├── history/               DetectionHistoryScreen
    ├── analytics/             AnalyticsDashboardScreen
    ├── auth/                  Login · Register · AuthViewModel
    ├── profile/               ProfileScreen
    ├── settings/              SettingsScreen
    ├── splash/                SplashScreen
    └── navigation/            VisionTrackNavGraph
```

---

## ML Pipeline

### Inference (YOLOv8Detector)

- Model loaded via `FileUtil.loadMappedFile` — memory-mapped, zero-copy
- Input: `[1, 640, 640, 3]` float32 normalized to `[0, 1]`
- Output: `[1, 84, 8400]` — 4 box coords + 80 class scores × 8400 anchors
- Per-class NMS with configurable IoU threshold (default 0.45)
- **Demo mode**: when `yolov8n.tflite` is absent, generates realistic simulated detections so the app runs on any device without the model file

### Frame to Inference to UI

```
CameraX ImageAnalysis (RGBA_8888)
  └─ DetectionAnalyzer.analyze()
       └─ toBitmap() → rotateBitmap()
            └─ ObjectDetectionHelper.processFrame()
                 ├─ YOLOv8Detector.detect()         ← TFLite inference
                 ├─ assignTrackingId()               ← centroid-IoU tracker
                 └─ metricsBuffer (30-frame rolling avg)
                      └─ DetectionViewModel.onFrameDetected()
                           ├─ stamp sessionId on each DetectedObject
                           ├─ batch flush to Room every 10 frames
                           └─ StateFlow update → Compose recomposition
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- Android device or emulator — API 26+
- Firebase project with Email/Password Auth and Firestore enabled

### Clone and Open

```bash
git clone https://github.com/kartikjsonawane/VisionTrack.git
```

Open the cloned folder in Android Studio and let Gradle sync finish.

### Firebase Setup

1. Go to [console.firebase.google.com](https://console.firebase.google.com) and create a project
2. Add an Android app with package `com.visiontrack.app`
3. Download `google-services.json` → place in `app/`
4. Enable **Email/Password** sign-in under Authentication
5. Create a **Firestore** database (test mode to start)

### Add the TFLite Model (optional)

Without the model the app runs in **demo mode** — fully functional with simulated detections.

```bash
pip install ultralytics
python -c "from ultralytics import YOLO; YOLO('yolov8n.pt').export(format='tflite', imgsz=640, int8=True)"
# Copy output .tflite → app/src/main/assets/yolov8n.tflite
```

### Build

```bash
./gradlew assembleDebug
./gradlew installDebug
```

---

## Python ML Scripts

```
ml_pipeline/
├── convert_yolov8.py    PT → TFLite (FP32 / FP16 / INT8)
├── train.py             Custom dataset fine-tuning
├── evaluate.py          mAP · PR curves · per-class AP
├── dataset_prep.py      COCO / VOC → YOLO format conversion
└── requirements.txt
```

```bash
# Train on a custom dataset
python ml_pipeline/train.py \
    --data data/custom/dataset.yaml \
    --model yolov8n.pt \
    --epochs 100 --batch 16 --device 0

# Convert to INT8 TFLite
python ml_pipeline/convert_yolov8.py \
    --model runs/train/weights/best.pt \
    --quant int8
```

---

## Performance

| Device | Inference | Notes |
|---|---|---|
| OnePlus DN2101 (Snapdragon, arm64) | ~25 ms | Tested on physical device |
| Pixel 6 emulator (API 34, x86_64) | ~18 ms | CI / demo environment |
| Target mid-range (SD 730G) | < 40 ms | Meets 25 FPS target |

---

## Author

**Kartik Sonawane**
GitHub: [@kartikjsonawane](https://github.com/kartikjsonawane)
Email: kartikjaywantsonawane@gmail.com

---

## License

MIT License — Copyright (c) 2025 Kartik Sonawane

See [LICENSE](LICENSE) for full text.
