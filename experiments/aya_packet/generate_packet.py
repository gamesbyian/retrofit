"""Produce and byte-verify a fictional AYA spatial-composition/v1 packet.

Never sends media, obtains consent or claims an assistant accepted files.
Structural conformance is checked separately by AYA's pinned canonical validator.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath

from PIL import Image, ImageDraw

MIME_FORMATS = {
    "image/png": "PNG", "image/jpeg": "JPEG", "image/webp": "WEBP"
}


def _make_png(path: Path, size: tuple[int, int], kind: str) -> dict:
    path.parent.mkdir(parents=True, exist_ok=True)
    image = Image.new("RGBA" if kind == "overlay" else "RGB", size, (0, 0, 0, 0) if kind == "overlay" else (68, 70, 73))
    d = ImageDraw.Draw(image)
    if kind == "source":
        d.rectangle((0, 0, 319, 54), fill=(188, 184, 179))
        d.rectangle((148, 42, 168, 230), fill=(115, 120, 122))
        d.rectangle((28, 126, 78, 213), fill=(56, 62, 66))
        d.ellipse((85, 142, 115, 214), fill=(31, 136, 72))
        for x in range(182, 295, 15):
            d.line((x, 90, x, 188), fill=(230, 232, 232), width=5)
    elif kind == "overlay":
        for x, y, label in ((65, 224, "A"), (53, 160, "B"), (245, 110, "C")):
            d.ellipse((x-10, y-10, x+10, y+10), outline=(0, 0, 0, 255), width=2)
            d.text((x-4, y-6), label, fill=(255, 245, 240, 255))
    elif kind in ("boiler_front", "boiler_three_quarter"):
        d.rectangle((70, 24, 170, 200), fill=(189, 196, 197), outline=(15, 17, 18), width=4)
        d.rectangle((90, 45, 146, 64), fill=(18, 55, 83))
        if kind == "boiler_three_quarter":
            d.polygon([(170,24),(205,44),(205,217),(170,200)], fill=(130,139,142))
    elif kind == "hx_front":
        d.rectangle((52, 55, 188, 170), fill=(185, 185, 175), outline=(25, 25, 25), width=4)
        for y in range(75, 154, 20):
            d.line((60, y, 180, y), fill=(80, 85, 83), width=3)
    else:
        raise ValueError("Unknown generated fixture")
    image.save(path, format="PNG")
    data = path.read_bytes()
    return dict(path=path.as_posix(), mime="image/png", sha256=hashlib.sha256(data).hexdigest(),
                bytes=len(data), width=size[0], height=size[1])


def create_packet(root: Path) -> dict:
    """Create only fictional image bytes, never manufacturer or client references."""
    root = Path(root)
    generated = {}
    for rel, size, kind in [
        ("source/room.png", (320, 240), "source"),
        ("overlays/letters.png", (320, 240), "overlay"),
        ("references/boiler-front.png", (240, 240), "boiler_front"),
        ("references/boiler-threequarter.png", (240, 240), "boiler_three_quarter"),
        ("references/heat-exchanger-front.png", (240, 240), "hx_front"),
    ]:
        pic = _make_png(root / rel, size, kind)
        pic["path"] = rel
        generated[kind] = pic

    packet = {
        "format": "aya-spatial-composition/v1",
        "source": generated["source"],
        "overlay": generated["overlay"],
        "annotations": [
            {"id": "A", "label": "Floor stain", "geometry": {"kind": "point", "point": {"x": 0.2, "y": 0.94}}},
            {"id": "B", "label": "Existing boiler", "geometry": {"kind": "rect", "x": 0.08, "y": 0.48, "width": 0.2, "height": 0.42}},
            {"id": "C", "label": "Potential wall position", "geometry": {"kind": "point", "point": {"x": 0.78, "y": 0.45}}},
        ],
        "assets": [
            {"id": "FictionBoiler", "name": "Unbranded fictional boiler", "category": "boiler", "views": [
                {"angle": "front", "image": generated["boiler_front"]},
                {"angle": "three-quarter", "image": generated["boiler_three_quarter"]},
            ]},
            {"id": "FictionHX", "name": "Unbranded fictional exchanger", "category": "heat exchanger", "views": [
                {"angle": "front", "image": generated["hx_front"]},
            ]},
        ],
        "operations": [
            {"targetAnnotationId": "A", "kind": "clean", "detail": "Illustrate stain removal only"},
            {"targetAnnotationId": "B", "kind": "replace", "assetId": "FictionBoiler", "detail": "Conceptual replacement at B"},
            {"targetAnnotationId": "C", "kind": "add", "assetId": "FictionHX", "detail": "Proposed placement only, no mounting validation"},
        ],
        "constraints": {"preserveUnmarked": True, "engineeringVerified": False, "protectedAnnotationIds": []},
        "instructions": "Make a conceptual after-image. Retain all unrelated equipment, geometry and labels.",
    }
    root.mkdir(parents=True, exist_ok=True)
    (root / "manifest.json").write_text(json.dumps(packet, sort_keys=True, indent=2) + "\n", encoding="utf-8")
    return packet


def iter_images(packet: dict):
    yield packet["source"]
    if "overlay" in packet:
        yield packet["overlay"]
    for asset in packet["assets"]:
        for view in asset["views"]:
            yield view["image"]


def verify_payload_files(root: Path, packet: dict) -> list[str]:
    """Host-side exact-byte verifier. NOT a substitute for AYA schema/consent."""
    root = Path(root).resolve()
    errors = []
    used = set()
    for item in iter_images(packet):
        relative = item["path"]
        p = PurePosixPath(relative)
        if p.is_absolute() or p.parts[0] not in ("source", "references", "overlays") or \
                any(s in (".", "..") for s in p.parts) or len(p.parts) < 2:
            errors.append(f"unsafe path: {relative}")
            continue
        if relative in used:
            errors.append(f"duplicate file path: {relative}")
            continue
        used.add(relative)
        candidate = root.joinpath(*p.parts)
        if candidate.resolve() == root or not candidate.resolve().is_relative_to(root):
            errors.append(f"path escaped authorized root: {relative}")
            continue
        if any(part.is_symlink() for part in (root.joinpath(*p.parts[:n]) for n in range(1, len(p.parts)+1))):
            errors.append(f"symlink denied: {relative}")
            continue
        if not candidate.is_file():
            errors.append(f"missing regular image: {relative}")
            continue
        content = candidate.read_bytes()
        if len(content) != item["bytes"]:
            errors.append(f"byte length mismatch: {relative}")
        if hashlib.sha256(content).hexdigest() != item["sha256"]:
            errors.append(f"sha256 mismatch: {relative}")
        try:
            with Image.open(candidate) as decoded:
                decoded.load()
                if decoded.format != MIME_FORMATS.get(item["mime"]):
                    errors.append(f"image MIME/magic mismatch: {relative}")
                if decoded.size != (item["width"], item["height"]):
                    errors.append(f"decoded size mismatch: {relative}")
        except (OSError, ValueError):
            errors.append(f"unreadable image: {relative}")
    return errors


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--out-dir", required=True, type=Path)
    args = ap.parse_args()
    packet = create_packet(args.out_dir)
    failures = verify_payload_files(args.out_dir, packet)
    if failures:
        raise SystemExit("FAIL: " + "; ".join(failures))
    print(json.dumps({"status": "PASS", "format": packet["format"], "attachments": len(list(iter_images(packet))),
                      "total_bytes": sum(item["bytes"] for item in iter_images(packet)),
                      "structure_checked_by_aya_validator": False,
                      "user_consent_observed": False, "external_receiver_observed": False}, indent=2))
