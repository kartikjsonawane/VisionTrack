# Model Assets

Place your exported TFLite model here as `yolov8n.tflite`.

## How to generate
Run from the `ml_pipeline/` directory:

```bash
pip install -r requirements.txt

# 1. Prepare calibration images (COCO val subset or custom images)
mkdir -p data/calib_images
# copy ~200 representative images into data/calib_images/

# 2. Convert YOLOv8n with INT8 quantization (recommended)
python convert_yolov8.py \
    --model yolov8n.pt \
    --output ../app/src/main/assets/models/ \
    --quant int8 \
    --calib-data data/calib_images/ \
    --benchmark
```

## Expected model specs after conversion
| Metric        | Value      |
|---------------|------------|
| Input shape   | 1×640×640×3 |
| Output shape  | 1×84×8400  |
| Size (INT8)   | ~6.2 MB    |
| Size (FP16)   | ~12.5 MB   |
| GPU FPS       | 28–30      |
| CPU FPS       | 8–12       |
| Latency (GPU) | 33–38 ms   |
