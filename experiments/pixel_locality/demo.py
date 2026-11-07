"""Emit entirely synthetic room-like fixtures and a deterministic locality report.

No client photo, OEM graphic, provider call, or model-generated image is used.
The candidate deliberately repaints the *whole* source and also changes the
marked boiler area, simulating an unbounded generative edit.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image, ImageDraw
from preserve import run_files


def make_demo(out_dir: Path) -> dict:
    out_dir.mkdir(parents=True, exist_ok=True)
    width, height = 512, 320
    source = Image.new("RGB", (width, height))
    pixels = source.load()
    for y in range(height):
        for x in range(width):
            base = 65 + (x * 37 + y * 19) % 45
            pixels[x, y] = (base, base + 6, base + 10)
    draw = ImageDraw.Draw(source)
    # Immutable room structure, independent of the intended boiler replacement.
    draw.rectangle((0, 0, 511, 76), fill=(195, 190, 181))
    for x in range(0, width, 64):
        draw.line((x, 0, x, 76), fill=(80, 80, 80), width=2)
    draw.rectangle((228, 57, 258, 300), fill=(100, 102, 105))
    draw.rectangle((0, 255, 511, 319), fill=(111, 110, 106))
    for x in (90, 112, 272, 286, 305, 326, 350, 375, 405):
        draw.line((x, 85, x, 222), fill=(201, 203, 201), width=8)
    draw.rectangle((72, 175, 148, 273), fill=(74, 79, 83), outline=(226, 225, 216), width=3)
    draw.rectangle((85, 190, 134, 205), fill=(31, 31, 34))
    draw.ellipse((153, 183, 201, 274), fill=(22, 121, 62))
    source_path = out_dir / "synthetic-original.png"
    source.save(source_path)

    # Deliberate widespread scene drift plus a completely different boiler.
    candidate = source.copy()
    candidate_pixels = candidate.load()
    for y in range(height):
        for x in range(width):
            r, g, b = candidate_pixels[x, y]
            candidate_pixels[x, y] = (min(255, r + 11), max(0, g - 9), min(255, b + 4))
    altered = ImageDraw.Draw(candidate)
    altered.rectangle((72, 175, 148, 273), fill=(211, 214, 215), outline=(17, 22, 31), width=3)
    altered.rectangle((85, 194, 134, 210), fill=(16, 47, 92))
    candidate_path = out_dir / "synthetic-global-drift.png"
    candidate.save(candidate_path)

    mask = Image.new("L", source.size, 0)
    allowed = ImageDraw.Draw(mask)
    allowed.rectangle((70, 169, 150, 278), fill=255)
    mask_path = out_dir / "allow-change-mask.png"
    mask.save(mask_path)
    output_path = out_dir / "locally-protected-concept.png"
    report = run_files(source_path, candidate_path, mask_path, output_path,
                       registered=True, replace=True)
    proof = report.as_dict()
    proof.update({
        "input_kind": "synthetic schematic-like colored pixels (not an actual room)",
        "real_model_edit_executed": False,
        "registration_proved": False,
        "registration_assumed_for_synthetic_fixture": True,
        "model_or_safety_correctness_proved": False,
        "source_file": source_path.name,
        "candidate_file": candidate_path.name,
        "mask_file": mask_path.name,
        "output_file": output_path.name,
        "status": "PASS" if report.drifted_protected_pixels_after == 0 and
                          report.drifted_protected_pixels_before > 0 else "FAIL",
    })
    (out_dir / "locality-report.json").write_text(json.dumps(proof, indent=2) + "\n")
    return proof


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out-dir", type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(make_demo(args.out_dir), indent=2))
