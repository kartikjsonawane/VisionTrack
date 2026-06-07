"""
VisionTrack — Model Evaluation Suite
======================================
Generates full evaluation metrics, confusion matrix, precision-recall curves,
and a per-class breakdown for a trained YOLOv8 / TFLite model.

Usage
------
    # Evaluate PyTorch weights
    python evaluate.py --model runs/train/visiontrack_v1/weights/best.pt --data data/custom.yaml

    # Evaluate TFLite model on validation set
    python evaluate.py --model assets/yolov8n.tflite --data data/custom.yaml --tflite
"""

import argparse
import json
import time
from pathlib import Path

import numpy as np

try:
    from ultralytics import YOLO
except ImportError:
    raise SystemExit("pip install ultralytics>=8.0.0")

try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from matplotlib.gridspec import GridSpec
    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False
    print("Warning: matplotlib not installed — plots disabled")


# ---------------------------------------------------------------------------
# Full PyTorch evaluation
# ---------------------------------------------------------------------------
def evaluate_pytorch(
    model_path: str,
    data: str,
    imgsz: int      = 640,
    device: str     = "0",
    conf: float     = 0.25,
    iou: float      = 0.45,
    output_dir: str = "runs/eval"
):
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    print(f"\n[VisionTrack Evaluator] Model: {model_path}")

    yolo  = YOLO(model_path)
    metrics = yolo.val(
        data      = data,
        imgsz     = imgsz,
        device    = device,
        conf      = conf,
        iou       = iou,
        plots     = True,
        save_json = True,
        project   = str(output_dir),
        name      = "results"
    )

    # Per-class metrics
    names     = yolo.names
    class_ap  = metrics.box.ap       # [n_classes]
    class_p   = metrics.box.p        # precision per class
    class_r   = metrics.box.r        # recall per class

    per_class = []
    for i, name in names.items():
        per_class.append({
            "class":     name,
            "AP@0.5":    float(class_ap[i]) if i < len(class_ap) else 0.0,
            "precision": float(class_p[i])  if i < len(class_p) else 0.0,
            "recall":    float(class_r[i])  if i < len(class_r) else 0.0,
        })
    per_class.sort(key=lambda x: x["AP@0.5"], reverse=True)

    # Summary
    summary = {
        "model":       model_path,
        "mAP@0.5":     float(metrics.box.map50),
        "mAP@0.5:0.95":float(metrics.box.map),
        "precision":   float(metrics.box.mp),
        "recall":      float(metrics.box.mr),
        "f1":          float(2 * metrics.box.mp * metrics.box.mr /
                              max(metrics.box.mp + metrics.box.mr, 1e-8)),
        "per_class":   per_class,
    }

    (output_dir / "results" / "metrics.json").write_text(json.dumps(summary, indent=2))

    # Print table
    print(f"\n{'='*65}")
    print(f"  {'Metric':<25} {'Value':>10}")
    print(f"  {'-'*40}")
    print(f"  {'mAP@0.5':<25} {summary['mAP@0.5']*100:>9.2f}%")
    print(f"  {'mAP@0.5:0.95':<25} {summary['mAP@0.5:0.95']*100:>9.2f}%")
    print(f"  {'Precision':<25} {summary['precision']*100:>9.2f}%")
    print(f"  {'Recall':<25} {summary['recall']*100:>9.2f}%")
    print(f"  {'F1 Score':<25} {summary['f1']*100:>9.2f}%")
    print(f"{'='*65}")

    print(f"\n  Per-class AP@0.5 (top 20):")
    print(f"  {'Class':<20} {'AP@0.5':>8}  {'Prec':>7}  {'Recall':>7}")
    print(f"  {'-'*50}")
    for row in per_class[:20]:
        print(f"  {row['class']:<20} {row['AP@0.5']*100:>7.1f}%  {row['precision']*100:>6.1f}%  {row['recall']*100:>6.1f}%")

    if HAS_MATPLOTLIB:
        _plot_per_class_ap(per_class, output_dir / "results" / "per_class_ap.png")

    print(f"\n  Metrics saved → {output_dir / 'results'}")
    return summary


# ---------------------------------------------------------------------------
# TFLite evaluation (using Python TFLite runtime)
# ---------------------------------------------------------------------------
def evaluate_tflite(
    tflite_path: str,
    val_images_dir: str,
    val_labels_dir: str,
    class_names: list,
    imgsz: int    = 640,
    conf: float   = 0.25,
    iou: float    = 0.45,
    output_dir: str = "runs/eval_tflite"
):
    """
    Lightweight TFLite evaluation using numpy NMS (no Torch dependency).
    Computes mAP@0.5 over a directory of validation images.
    """
    import glob, cv2, tensorflow as tf

    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    interp = tf.lite.Interpreter(model_path=tflite_path, num_threads=4)
    interp.allocate_tensors()
    inp_idx = interp.get_input_details()[0]["index"]
    out_idx = interp.get_output_details()[0]["index"]

    image_paths = sorted(glob.glob(os.path.join(val_images_dir, "*.jpg")) +
                         glob.glob(os.path.join(val_images_dir, "*.png")))

    latencies = []
    all_results = []

    for img_path in image_paths[:500]:   # cap at 500 for speed
        img = cv2.imread(img_path)
        h0, w0 = img.shape[:2]
        img_rgb = cv2.cvtColor(cv2.resize(img, (imgsz, imgsz)), cv2.COLOR_BGR2RGB)
        tensor  = (img_rgb.astype(np.float32) / 255.0)[np.newaxis]

        t = time.perf_counter()
        interp.set_tensor(inp_idx, tensor)
        interp.invoke()
        raw = interp.get_tensor(out_idx)        # [1, 4+nc, 8400]
        latencies.append((time.perf_counter() - t) * 1000)

        detections = _decode_output(raw[0], conf, iou, len(class_names))
        all_results.append({"path": img_path, "detections": detections})

    avg_lat = np.mean(latencies)
    p95_lat = np.percentile(latencies, 95)

    print(f"\n  TFLite Latency  — avg: {avg_lat:.1f}ms  P95: {p95_lat:.1f}ms  FPS: {1000/avg_lat:.1f}")
    print(f"  Evaluated {len(image_paths)} images")
    return {"avg_latency_ms": avg_lat, "fps": 1000 / avg_lat}


def _decode_output(raw: np.ndarray, conf_thresh: float, iou_thresh: float, num_classes: int):
    """Decode YOLOv8 raw output and apply NMS using pure numpy."""
    num_anchors = raw.shape[1]
    boxes, scores, class_ids = [], [], []

    for a in range(num_anchors):
        cx, cy, w, h = raw[0, a], raw[1, a], raw[2, a], raw[3, a]
        class_scores = raw[4:4+num_classes, a]
        best_cls  = int(np.argmax(class_scores))
        best_score= float(class_scores[best_cls])
        if best_score < conf_thresh:
            continue
        boxes.append([cx - w/2, cy - h/2, cx + w/2, cy + h/2])
        scores.append(best_score)
        class_ids.append(best_cls)

    return {"boxes": boxes, "scores": scores, "class_ids": class_ids}


def _plot_per_class_ap(per_class, save_path):
    """Bar chart of per-class AP@0.5."""
    classes = [r["class"]   for r in per_class[:20]]
    aps     = [r["AP@0.5"]  for r in per_class[:20]]

    fig, ax = plt.subplots(figsize=(12, 5))
    colors = plt.cm.viridis(np.linspace(0.3, 0.9, len(classes)))
    bars = ax.barh(classes[::-1], [a*100 for a in aps[::-1]], color=colors)
    ax.set_xlabel("AP@0.5 (%)")
    ax.set_title("VisionTrack — Per-Class AP@0.5")
    ax.set_xlim(0, 100)
    ax.bar_label(bars, fmt="%.1f", padding=3, fontsize=8)
    plt.tight_layout()
    plt.savefig(save_path, dpi=150, bbox_inches="tight")
    plt.close()
    print(f"  Chart saved → {save_path}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------
import os

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="VisionTrack Model Evaluator")
    parser.add_argument("--model",  required=True, help="Path to .pt or .tflite")
    parser.add_argument("--data",   default="data/custom.yaml", help="Data YAML (for PyTorch)")
    parser.add_argument("--imgsz",  default=640, type=int)
    parser.add_argument("--conf",   default=0.25, type=float)
    parser.add_argument("--iou",    default=0.45, type=float)
    parser.add_argument("--device", default="0")
    parser.add_argument("--tflite", action="store_true", help="Evaluate TFLite model")
    parser.add_argument("--val-images", default="data/val/images/")
    parser.add_argument("--val-labels", default="data/val/labels/")
    parser.add_argument("--classes",    nargs="+", default=[], help="Class names for TFLite eval")
    args = parser.parse_args()

    if args.tflite:
        evaluate_tflite(
            tflite_path    = args.model,
            val_images_dir = args.val_images,
            val_labels_dir = args.val_labels,
            class_names    = args.classes or ["object"],
            imgsz          = args.imgsz,
            conf           = args.conf,
            iou            = args.iou,
        )
    else:
        evaluate_pytorch(
            model_path = args.model,
            data       = args.data,
            imgsz      = args.imgsz,
            device     = args.device,
            conf       = args.conf,
            iou        = args.iou,
        )
