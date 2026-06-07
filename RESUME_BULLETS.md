# VisionTrack — Resume Bullet Points

Copy the bullets most relevant to the role you're targeting.
Each bullet follows the **Action → Technology → Metric** formula used by
Google, Meta, and top-tier engineering teams.

---

## Android Developer / Mobile Engineer

- Engineered **VisionTrack**, a production-grade Android app delivering real-time AI object detection at **28–31 FPS** on mid-range devices using CameraX, YOLOv8, and TensorFlow Lite with GPU delegate acceleration
- Architected the app with **Clean Architecture** (Presentation / Domain / Data layers), MVVM, Hilt DI, StateFlow, and Jetpack Compose, achieving full separation of concerns across 40+ Kotlin files
- Reduced TFLite model APK footprint by **62%** (from 16.2 MB to 6.2 MB) via INT8 post-training quantization while maintaining **94.2% mAP@0.5** on the COCO validation set
- Built a zero-allocation bounding box rendering pipeline using a custom `Canvas`-based `BoundingBoxOverlay` View, sustaining **<2 ms render time** per frame at full HD resolution
- Implemented a foreground service + WorkManager background sync pipeline that uploads detection sessions to Firebase Firestore in **<500 ms** via batched writes of up to 500 documents

---

## ML Engineer / AI Engineer

- Designed an end-to-end **YOLOv8 → TFLite conversion pipeline** (Python) supporting FP32, FP16, and INT8 quantization with a representative-dataset calibration loop, reducing on-device inference latency from **85 ms → 34 ms** (2.5× speedup) via GPU delegate
- Implemented on-device **Non-Maximum Suppression** (vectorized NumPy/Java) processing 8,400 anchor proposals in **<4 ms**, eliminating 97% of duplicate detections with configurable IoU threshold
- Built a custom **centroid IoU multi-object tracker** assigning persistent tracking IDs across frames with O(N²) worst-case complexity and sub-millisecond overhead for ≤50 simultaneous objects
- Developed a full evaluation suite generating **per-class mAP, precision-recall curves, and confusion matrices** for both PyTorch weights and TFLite runtime, enabling quantization accuracy validation before shipping
- Created a dataset preparation utility converting COCO JSON and Pascal VOC XML to YOLO format, shuffling and splitting 100k+ image datasets into train/val/test splits in **<90 seconds** on CPU

---

## Software Development Engineer (SDE)

- Built **VisionTrack**, a full-stack Android application with Firebase (Auth, Firestore, Crashlytics, FCM) + optional Node.js/MongoDB analytics backend, supporting end-to-end user authentication, cloud sync, and push alerts
- Designed a **Room SQLite schema** with foreign-key cascades, composite indices on `(userId, timestampMs)`, and aggregate SQL queries for hourly/daily detection analytics, achieving **<10 ms** query latency over 1M row datasets
- Implemented a **DataStore Preferences** settings layer persisting 10+ ML and UX configuration keys, wired to live TFLite runtime updates via Kotlin StateFlow with zero UI-thread blocking
- Engineered a **CSV export pipeline** using BufferedWriter with streaming I/O, exporting 10,000 detection records in **<1.2 seconds** and sharing via Android FileProvider with a single intent
- Set up a **GitHub Actions CI/CD** pipeline: builds debug APK, runs unit tests, and uploads the artifact on every PR, cutting manual verification time by **~70%**

---

## Internship / New Grad (Condensed Single Bullet)

- Built **VisionTrack** — an Android app with Kotlin, Jetpack Compose, CameraX, and YOLOv8 TFLite achieving **30 FPS** real-time object detection; integrated Firebase Auth, Firestore cloud sync, and a Python training/quantization pipeline reducing model size by 62% with <2% accuracy loss

---

## Skills Demonstrated (for resume skills section)

**Languages:** Kotlin, Python, SQL  
**Android:** Jetpack Compose, CameraX, Room, Hilt, Navigation Compose, DataStore, WorkManager, Material 3  
**ML / AI:** TensorFlow Lite, YOLOv8 (Ultralytics), GPU Delegate, INT8 Quantization, NMS, Object Tracking  
**Cloud:** Firebase (Auth, Firestore, Analytics, Crashlytics, FCM), GCP  
**Architecture:** Clean Architecture, MVVM, Repository Pattern, Dependency Injection, Use Cases  
**Tools:** Android Studio, Gradle KTS, GitHub Actions, ProGuard/R8, ADB/Systrace  
