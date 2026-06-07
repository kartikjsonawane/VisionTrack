"""
VisionTrack — Dataset Preparation Utility
==========================================
Converts common annotation formats (COCO JSON, Pascal VOC XML, LabelImg TXT)
to YOLOv8-compatible format, and creates the dataset YAML config.

Usage
------
    # From COCO JSON
    python dataset_prep.py \
        --source coco \
        --annotations data/raw/instances_train2017.json \
        --images     data/raw/train2017/ \
        --output     data/visiontrack/ \
        --split 0.8 0.15 0.05

    # From Pascal VOC XML
    python dataset_prep.py \
        --source voc \
        --annotations data/raw/Annotations/ \
        --images     data/raw/JPEGImages/ \
        --output     data/visiontrack/
"""

import argparse
import json
import os
import random
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Dict, List, Tuple


# ---------------------------------------------------------------------------
# COCO JSON → YOLO TXT
# ---------------------------------------------------------------------------
def convert_coco(
    annotations_path: str,
    images_dir: str,
    output_dir: str,
    split: Tuple[float, float, float] = (0.8, 0.15, 0.05)
) -> dict:
    """Convert COCO annotation JSON to YOLO label .txt files."""
    with open(annotations_path) as f:
        coco = json.load(f)

    categories = {cat["id"]: cat["name"] for cat in coco["categories"]}
    # Build category → index mapping (0-based)
    cat_to_idx = {cat_id: idx for idx, cat_id in enumerate(sorted(categories.keys()))}
    class_names = [categories[cid] for cid in sorted(categories.keys())]

    # image_id → {file_name, width, height}
    images_info = {
        img["id"]: {
            "file_name": img["file_name"],
            "width":  img["width"],
            "height": img["height"]
        }
        for img in coco["images"]
    }

    # Build per-image annotation dict
    annots_by_image: Dict[int, list] = {}
    for ann in coco["annotations"]:
        img_id = ann["image_id"]
        annots_by_image.setdefault(img_id, [])
        x, y, w, h = ann["bbox"]
        img_info   = images_info[img_id]
        iw, ih     = img_info["width"], img_info["height"]
        # Normalize to [0,1]
        cx = (x + w / 2) / iw
        cy = (y + h / 2) / ih
        nw = w / iw
        nh = h / ih
        cls_idx = cat_to_idx[ann["category_id"]]
        annots_by_image[img_id].append(f"{cls_idx} {cx:.6f} {cy:.6f} {nw:.6f} {nh:.6f}")

    # Split image IDs
    img_ids = list(images_info.keys())
    random.shuffle(img_ids)
    n = len(img_ids)
    n_train = int(n * split[0])
    n_val   = int(n * split[1])
    splits  = {
        "train": img_ids[:n_train],
        "val":   img_ids[n_train:n_train+n_val],
        "test":  img_ids[n_train+n_val:]
    }

    out = Path(output_dir)
    for split_name, ids in splits.items():
        (out / "images" / split_name).mkdir(parents=True, exist_ok=True)
        (out / "labels" / split_name).mkdir(parents=True, exist_ok=True)
        for img_id in ids:
            info     = images_info[img_id]
            src_img  = Path(images_dir) / info["file_name"]
            if not src_img.exists():
                continue
            dst_img  = out / "images" / split_name / info["file_name"]
            shutil.copy(src_img, dst_img)
            lbl_path = out / "labels" / split_name / (Path(info["file_name"]).stem + ".txt")
            annots   = annots_by_image.get(img_id, [])
            lbl_path.write_text("\n".join(annots))

    return {"class_names": class_names, "splits": {k: len(v) for k, v in splits.items()}}


# ---------------------------------------------------------------------------
# Pascal VOC XML → YOLO TXT
# ---------------------------------------------------------------------------
def convert_voc(
    annotations_dir: str,
    images_dir: str,
    output_dir: str,
    split: Tuple[float, float, float] = (0.8, 0.15, 0.05)
) -> dict:
    xml_files   = sorted(Path(annotations_dir).glob("*.xml"))
    class_names = []

    records = []
    for xml_path in xml_files:
        tree = ET.parse(xml_path)
        root = tree.getroot()
        img_name = root.find("filename").text
        size     = root.find("size")
        iw = int(size.find("width").text)
        ih = int(size.find("height").text)

        yolo_lines = []
        for obj in root.findall("object"):
            cls = obj.find("name").text
            if cls not in class_names:
                class_names.append(cls)
            cls_idx = class_names.index(cls)
            bndbox  = obj.find("bndbox")
            xmin = float(bndbox.find("xmin").text)
            ymin = float(bndbox.find("ymin").text)
            xmax = float(bndbox.find("xmax").text)
            ymax = float(bndbox.find("ymax").text)
            cx = ((xmin + xmax) / 2) / iw
            cy = ((ymin + ymax) / 2) / ih
            nw = (xmax - xmin) / iw
            nh = (ymax - ymin) / ih
            yolo_lines.append(f"{cls_idx} {cx:.6f} {cy:.6f} {nw:.6f} {nh:.6f}")

        records.append({"image": img_name, "labels": yolo_lines})

    random.shuffle(records)
    n = len(records)
    n_train = int(n * split[0])
    n_val   = int(n * split[1])
    splits  = {
        "train": records[:n_train],
        "val":   records[n_train:n_train+n_val],
        "test":  records[n_train+n_val:]
    }

    out = Path(output_dir)
    for split_name, recs in splits.items():
        (out / "images" / split_name).mkdir(parents=True, exist_ok=True)
        (out / "labels" / split_name).mkdir(parents=True, exist_ok=True)
        for rec in recs:
            src = Path(images_dir) / rec["image"]
            if src.exists():
                shutil.copy(src, out / "images" / split_name / rec["image"])
            lbl = out / "labels" / split_name / (Path(rec["image"]).stem + ".txt")
            lbl.write_text("\n".join(rec["labels"]))

    return {"class_names": class_names, "splits": {k: len(v) for k, v in splits.items()}}


# ---------------------------------------------------------------------------
# Write dataset YAML
# ---------------------------------------------------------------------------
def write_yaml(output_dir: str, class_names: List[str], splits: dict) -> str:
    out  = Path(output_dir)
    yaml = f"""# VisionTrack Dataset — auto-generated by dataset_prep.py
path: {out.absolute()}
train: images/train
val:   images/val
test:  images/test

nc: {len(class_names)}
names: {class_names}

# Split summary
# train: {splits.get('train', 0)} images
# val:   {splits.get('val',   0)} images
# test:  {splits.get('test',  0)} images
"""
    yaml_path = out / "dataset.yaml"
    yaml_path.write_text(yaml)
    print(f"  YAML written → {yaml_path}")
    return str(yaml_path)


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="VisionTrack Dataset Prep")
    parser.add_argument("--source",      required=True, choices=["coco", "voc"], help="Annotation format")
    parser.add_argument("--annotations", required=True, help="COCO JSON file or VOC XML directory")
    parser.add_argument("--images",      required=True, help="Source images directory")
    parser.add_argument("--output",      default="data/visiontrack/")
    parser.add_argument("--split",       nargs=3, type=float, default=[0.8, 0.15, 0.05],
                        metavar=("TRAIN", "VAL", "TEST"))
    parser.add_argument("--seed",        default=42, type=int)
    args = parser.parse_args()

    random.seed(args.seed)
    split_tuple = tuple(args.split)

    print(f"\n[VisionTrack Dataset Prep]  format={args.source}  output={args.output}")

    if args.source == "coco":
        result = convert_coco(args.annotations, args.images, args.output, split_tuple)
    else:
        result = convert_voc(args.annotations, args.images, args.output, split_tuple)

    yaml_path = write_yaml(args.output, result["class_names"], result["splits"])

    print(f"\n  Done!")
    print(f"  Classes : {len(result['class_names'])}  — {result['class_names'][:10]}{'...' if len(result['class_names'])>10 else ''}")
    print(f"  Train   : {result['splits'].get('train',0)}")
    print(f"  Val     : {result['splits'].get('val',0)}")
    print(f"  Test    : {result['splits'].get('test',0)}")
    print(f"  YAML    : {yaml_path}")
