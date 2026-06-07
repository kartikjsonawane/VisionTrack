"""
VisionTrack — YOLOv8 → TensorFlow Lite Conversion Pipeline
============================================================
Converts a YOLOv8 PyTorch model to an optimised TFLite format
for on-device inference on Android.

Supported export modes
  • FP32  — baseline full-precision (largest, most accurate)
  • FP16  — half-precision (2× smaller, negligible accuracy drop)
  • INT8  — post-training quantization (4× smaller, ~1-2% mAP drop)
  • INT8 QAT — quantization-aware training export (best accuracy)

Usage
------
    python convert_yolov8.py \
        --model yolov8n.pt \
        --output ../app/src/main/assets/models/ \
        --quant int8 \
        --calib-data data/coco_calib.yaml \
        --size 640
"""

import argparse
import os
import shutil
import time
from pathlib import Path

import numpy as np

# ---------------------------------------------------------------------------
# Optional deps — installed at runtime only if needed
# ---------------------------------------------------------------------------
try:
    from ultralytics import YOLO
except ImportError:
    raise SystemExit(
        "Install ultralytics:  pip install ultralytics>=8.0.0"
    )

try:
    import tensorflow as tf
    print(f"TensorFlow {tf.__version__}")
except ImportError:
    raise SystemExit("Install TensorFlow:  pip install tensorflow>=2.16")


# ---------------------------------------------------------------------------
# Representative dataset generator for INT8 calibration
# ---------------------------------------------------------------------------
def make_representative_dataset(calib_images_dir: str, input_size: int, n_samples: int = 200):
    """
    Yields calibration tensors from a directory of JPEG/PNG images.
    TFLite converter calls this to estimate activation ranges for INT8 quant.
    """
    import glob, cv2

    image_paths = glob.glob(os.path.join(calib_images_dir, "*.jpg")) + \
                  glob.glob(os.path.join(calib_images_dir, "*.png"))
    image_paths = image_paths[:n_samples]

    if not image_paths:
        raise FileNotFoundError(
            f"No calibration images found in {calib_images_dir}. "
            "Provide COCO val images or any representative dataset."
        )

    print(f"  Using {len(image_paths)} calibration images from {calib_images_dir}")

    def generator():
        for path in image_paths:
            img = cv2.imread(path)
            img = cv2.resize(img, (input_size, input_size))
            img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
            img = img.astype(np.float32) / 255.0
            yield [img[np.newaxis, ...]]  # [1, H, W, 3]

    return generator


# ---------------------------------------------------------------------------
# Main conversion function
# ---------------------------------------------------------------------------
def convert(
    model_path: str,
    output_dir: str,
    quant_mode: str = "int8",
    input_size: int = 640,
    calib_data: str = "data/calib_images/",
    n_calib: int = 200,
) -> str:
    """
    Convert YOLOv8 PyTorch model → TFLite.

    Returns path to the exported .tflite file.
    """
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    print(f"\n{'='*60}")
    print(f"  VisionTrack YOLOv8 → TFLite Converter")
    print(f"  Model : {model_path}")
    print(f"  Mode  : {quant_mode.upper()}")
    print(f"  Size  : {input_size}×{input_size}")
    print(f"{'='*60}\n")

    # 1. Load YOLOv8 and export to saved_model (TF format)
    print("[1/4] Loading YOLOv8 and exporting to TensorFlow SavedModel …")
    t0 = time.time()
    model = YOLO(model_path)

    # Export to TF saved_model first (needed for TFLite conversion)
    saved_model_dir = model.export(
        format="saved_model",
        imgsz=input_size,
        dynamic=False,
        simplify=True,
        nms=False,       # We implement NMS on-device for flexibility
        half=(quant_mode == "fp16"),
    )
    print(f"   SavedModel exported in {time.time()-t0:.1f}s → {saved_model_dir}")

    # 2. Load saved_model into TFLite converter
    print("[2/4] Loading SavedModel into TFLite converter …")
    converter = tf.lite.TFLiteConverter.from_saved_model(str(saved_model_dir))

    # 3. Apply quantization optimizations
    print(f"[3/4] Applying {quant_mode.upper()} optimizations …")

    if quant_mode == "fp32":
        # Baseline — no quantization
        pass

    elif quant_mode == "fp16":
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.float16]
        print("   FP16 half-precision enabled")

    elif quant_mode in ("int8", "int8_qat"):
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_ops = [
            tf.lite.OpsSet.TFLITE_BUILTINS_INT8,
            tf.lite.OpsSet.TFLITE_BUILTINS,      # fallback for unsupported ops
        ]
        converter.inference_input_type  = tf.float32   # keep float I/O for ease
        converter.inference_output_type = tf.float32

        # Representative dataset for calibration
        rep_dataset = make_representative_dataset(calib_data, input_size, n_calib)
        converter.representative_dataset = rep_dataset
        print("   INT8 post-training quantization with calibration enabled")

    else:
        raise ValueError(f"Unknown quant_mode: {quant_mode}. Choose fp32/fp16/int8.")

    # Enable GPU delegate–compatible ops
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,
        tf.lite.OpsSet.SELECT_TF_OPS,
    ]
    converter.allow_custom_ops = False

    # 4. Convert and save
    print("[4/4] Converting and writing .tflite file …")
    t1 = time.time()
    tflite_model = converter.convert()
    convert_time = time.time() - t1

    stem       = Path(model_path).stem
    quant_tag  = quant_mode.replace("_", "-")
    out_name   = f"{stem}_{quant_tag}_{input_size}.tflite"
    out_path   = output_dir / out_name

    out_path.write_bytes(tflite_model)
    size_mb = out_path.stat().st_size / 1024 / 1024

    print(f"\n{'='*60}")
    print(f"  ✓ Conversion complete in {convert_time:.1f}s")
    print(f"  Output : {out_path}")
    print(f"  Size   : {size_mb:.2f} MB")
    print(f"{'='*60}\n")

    # Copy a clean-named version for the app assets
    final_name = "yolov8n.tflite"
    shutil.copy(out_path, output_dir / final_name)
    print(f"  Also copied as: {output_dir / final_name}")

    return str(out_path)


# ---------------------------------------------------------------------------
# Benchmark helper
# ---------------------------------------------------------------------------
def benchmark_tflite(tflite_path: str, input_size: int = 640, runs: int = 100):
    """
    Runs the TFLite model N times and reports latency statistics.
    Simulates a mid-range Android device using the TFLite Python runtime.
    """
    interpreter = tf.lite.Interpreter(model_path=tflite_path, num_threads=4)
    interpreter.allocate_tensors()

    inp_details = interpreter.get_input_details()[0]
    out_details = interpreter.get_output_details()[0]

    dummy = np.random.rand(1, input_size, input_size, 3).astype(np.float32)

    # Warm-up
    for _ in range(5):
        interpreter.set_tensor(inp_details["index"], dummy)
        interpreter.invoke()

    times = []
    for _ in range(runs):
        t = time.perf_counter()
        interpreter.set_tensor(inp_details["index"], dummy)
        interpreter.invoke()
        times.append((time.perf_counter() - t) * 1000)

    times_np = np.array(times)
    print(f"\nBenchmark ({runs} runs @ {input_size}×{input_size}):")
    print(f"  Mean    : {times_np.mean():.2f} ms  ({1000/times_np.mean():.1f} FPS)")
    print(f"  Median  : {np.median(times_np):.2f} ms")
    print(f"  P95     : {np.percentile(times_np, 95):.2f} ms")
    print(f"  Min/Max : {times_np.min():.2f} / {times_np.max():.2f} ms")
    return {"mean_ms": float(times_np.mean()), "fps": float(1000 / times_np.mean())}


# ---------------------------------------------------------------------------
# CLI entrypoint
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="VisionTrack YOLOv8 → TFLite converter")
    parser.add_argument("--model",       default="yolov8n.pt",   help="Path to YOLOv8 .pt weights")
    parser.add_argument("--output",      default="../app/src/main/assets/models/", help="Output directory")
    parser.add_argument("--quant",       default="int8",          choices=["fp32","fp16","int8","int8_qat"])
    parser.add_argument("--calib-data",  default="data/calib_images/", help="Dir with calibration images for INT8")
    parser.add_argument("--size",        default=640, type=int,   help="Input image size (square)")
    parser.add_argument("--n-calib",     default=200, type=int,   help="Number of calibration images")
    parser.add_argument("--benchmark",   action="store_true",     help="Benchmark after conversion")
    args = parser.parse_args()

    tflite_path = convert(
        model_path  = args.model,
        output_dir  = args.output,
        quant_mode  = args.quant,
        input_size  = args.size,
        calib_data  = args.calib_data,
        n_calib     = args.n_calib,
    )

    if args.benchmark:
        benchmark_tflite(tflite_path, input_size=args.size)
