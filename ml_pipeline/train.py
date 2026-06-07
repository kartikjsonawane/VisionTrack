"""
VisionTrack — YOLOv8 Custom Training Pipeline
===============================================
Fine-tunes YOLOv8 on a custom dataset (e.g., logistics, retail, surveillance)
starting from COCO pre-trained weights.

Usage
------
    python train.py \
        --data data/custom.yaml \
        --model yolov8n.pt \
        --epochs 100 \
        --batch 16 \
        --imgsz 640 \
        --name visiontrack_v1

Dataset YAML format (data/custom.yaml):
    path: /path/to/dataset
    train: images/train
    val:   images/val
    test:  images/test  # optional
    nc: 3               # number of classes
    names: ['person', 'vehicle', 'package']
"""

import argparse
import json
import os
import time
from pathlib import Path

try:
    from ultralytics import YOLO
    from ultralytics.utils import LOGGER
except ImportError:
    raise SystemExit("pip install ultralytics>=8.0.0")


# ---------------------------------------------------------------------------
# Training entry-point
# ---------------------------------------------------------------------------
def train(
    data: str,
    model: str      = "yolov8n.pt",
    epochs: int     = 100,
    batch: int      = 16,
    imgsz: int      = 640,
    name: str       = "visiontrack_v1",
    device: str     = "0",          # "0" = GPU 0, "cpu" for CPU-only
    workers: int    = 8,
    patience: int   = 20,           # Early-stopping patience
    lr0: float      = 0.01,
    lrf: float      = 0.01,
    momentum: float = 0.937,
    weight_decay: float = 5e-4,
    augment: bool   = True,
    resume: bool    = False,
    project: str    = "runs/train"
):
    print(f"\n{'='*60}")
    print(f"  VisionTrack Training Pipeline")
    print(f"  Model   : {model}")
    print(f"  Data    : {data}")
    print(f"  Epochs  : {epochs}  |  Batch : {batch}  |  ImgSz : {imgsz}")
    print(f"  Device  : {device}")
    print(f"{'='*60}\n")

    yolo = YOLO(model)

    results = yolo.train(
        data        = data,
        epochs      = epochs,
        batch       = batch,
        imgsz       = imgsz,
        device      = device,
        workers     = workers,
        patience    = patience,
        lr0         = lr0,
        lrf         = lrf,
        momentum    = momentum,
        weight_decay= weight_decay,
        # Augmentations
        augment     = augment,
        hsv_h       = 0.015,
        hsv_s       = 0.7,
        hsv_v       = 0.4,
        degrees     = 5.0,
        translate   = 0.1,
        scale       = 0.5,
        flipud      = 0.0,
        fliplr      = 0.5,
        mosaic      = 1.0,
        mixup       = 0.1,
        copy_paste  = 0.1,
        # Logging
        project     = project,
        name        = name,
        exist_ok    = resume,
        resume      = resume,
        save        = True,
        save_period = 10,
        plots       = True,
        verbose     = True,
    )

    # Save training summary
    summary = {
        "model":           model,
        "data":            data,
        "epochs_trained":  epochs,
        "best_map50":      float(results.results_dict.get("metrics/mAP50(B)", 0)),
        "best_map50_95":   float(results.results_dict.get("metrics/mAP50-95(B)", 0)),
        "best_precision":  float(results.results_dict.get("metrics/precision(B)", 0)),
        "best_recall":     float(results.results_dict.get("metrics/recall(B)", 0)),
        "save_dir":        str(results.save_dir),
    }
    summary_path = Path(results.save_dir) / "training_summary.json"
    summary_path.write_text(json.dumps(summary, indent=2))

    print(f"\n{'='*60}")
    print(f"  Training complete!")
    print(f"  mAP@0.5    : {summary['best_map50']*100:.1f}%")
    print(f"  mAP@0.5:0.95: {summary['best_map50_95']*100:.1f}%")
    print(f"  Precision  : {summary['best_precision']*100:.1f}%")
    print(f"  Recall     : {summary['best_recall']*100:.1f}%")
    print(f"  Saved to   : {summary['save_dir']}")
    print(f"{'='*60}\n")

    return results


# ---------------------------------------------------------------------------
# Quick validation on held-out test set
# ---------------------------------------------------------------------------
def validate(model_path: str, data: str, imgsz: int = 640, device: str = "0"):
    """Run validation on a trained model and print metrics."""
    yolo = YOLO(model_path)
    metrics = yolo.val(data=data, imgsz=imgsz, device=device, plots=True, save_json=True)

    print(f"\n{'='*60}")
    print("  Validation Results")
    print(f"  mAP@0.5    : {metrics.box.map50*100:.2f}%")
    print(f"  mAP@0.5:0.95: {metrics.box.map*100:.2f}%")
    print(f"  Precision  : {metrics.box.mp*100:.2f}%")
    print(f"  Recall     : {metrics.box.mr*100:.2f}%")
    print(f"{'='*60}\n")

    return metrics


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="VisionTrack YOLOv8 Training")
    parser.add_argument("--data",    required=True,            help="Path to data YAML")
    parser.add_argument("--model",   default="yolov8n.pt",     help="Base model weights")
    parser.add_argument("--epochs",  default=100,  type=int)
    parser.add_argument("--batch",   default=16,   type=int)
    parser.add_argument("--imgsz",   default=640,  type=int)
    parser.add_argument("--name",    default="visiontrack_v1")
    parser.add_argument("--device",  default="0",              help="CUDA device or 'cpu'")
    parser.add_argument("--workers", default=8,    type=int)
    parser.add_argument("--resume",  action="store_true",      help="Resume from last checkpoint")
    parser.add_argument("--val",     action="store_true",      help="Only run validation")
    args = parser.parse_args()

    if args.val:
        validate(args.model, args.data, args.imgsz, args.device)
    else:
        train(
            data    = args.data,
            model   = args.model,
            epochs  = args.epochs,
            batch   = args.batch,
            imgsz   = args.imgsz,
            name    = args.name,
            device  = args.device,
            workers = args.workers,
            resume  = args.resume,
        )
