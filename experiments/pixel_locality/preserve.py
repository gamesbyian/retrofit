"""Experimental *local* pixel-preservation oracle for conceptual imagery.

Accepted input: three ALREADY ORIENTED, REGISTERED frames (source, edited
candidate, allow-change mask) with equal pixel dimensions. A locally produced
PNG is pixel-identical to the decoded source wherever the mask equals zero.
Nothing in this module registers images, recognizes equipment, proves a vendor
model, supplies safety clearances, or claims a remote generator respects masks.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from dataclasses import dataclass
from pathlib import Path
from tempfile import NamedTemporaryFile

from PIL import Image

MAX_PIXELS = 24_000_000


@dataclass(frozen=True)
class PreservationReport:
    width: int
    height: int
    mode: str
    total_pixels: int
    protected_pixels: int
    allowed_pixels: int
    feathered_pixels: int
    drifted_protected_pixels_before: int
    drifted_protected_pixels_after: int
    source_pixel_sha256: str
    output_pixel_sha256: str

    def as_dict(self) -> dict[str, str | int]:
        return dict(vars(self))


def _pixel_hash(frame: Image.Image) -> str:
    """Mode and dimensions are part of the digest, not merely pixel payload."""
    payload = f"{frame.mode}:{frame.width}x{frame.height}:".encode() + frame.tobytes()
    return hashlib.sha256(payload).hexdigest()


def _validate(source: Image.Image, candidate: Image.Image, mask: Image.Image,
              allow_soft_mask: bool) -> tuple[int, int, int]:
    if source.mode not in ("RGB", "RGBA"):
        raise ValueError("Source must be decoded RGB or RGBA (already oriented)")
    if candidate.mode != source.mode:
        raise ValueError("Candidate mode mismatch; normalize explicitly before review")
    if mask.mode != "L":
        raise ValueError("Allow-change mask must be 8-bit L; do not pass provider alpha semantics")
    if source.size != candidate.size or source.size != mask.size:
        raise ValueError("Dimensions differ: refusing automatic resize or registration")
    if not (0 < source.width * source.height <= MAX_PIXELS):
        raise ValueError("Image exceeds the bounded 24-million-pixel experiment budget")
    for name, frame in (("source", source), ("candidate", candidate), ("mask", mask)):
        if frame.getexif().get(274, 1) not in (1, None):
            raise ValueError(f"{name} EXIF orientation is not normalized")
    hist = mask.histogram()
    protected = hist[0]
    allowed = source.width * source.height - protected
    soft = sum(hist[1:255])
    if not protected or not allowed:
        raise ValueError("Mask must have both protected and editable pixels")
    if soft and not allow_soft_mask:
        raise ValueError("Soft-edge mask requires explicit allow_soft_mask=True")
    return protected, allowed, soft


def preserve(source: Image.Image, candidate: Image.Image, mask: Image.Image,
             *, registered: bool, allow_soft_mask: bool = False
             ) -> tuple[Image.Image, PreservationReport]:
    """Composites allowed edits while copying every zero-mask decoded pixel.

    `registered` is a caller assertion, NOT proof of geometric alignment.
    This deliberate gate prevents accidentally treating matching dimensions as
    evidence of a shared camera frame. A test of real candidate registration
    and of mask review must occur separately before product integration.
    """
    if not registered:
        raise ValueError("Registration is unverified; explicit assertion required")
    protected, allowed, soft = _validate(source, candidate, mask, allow_soft_mask)
    source_hash = _pixel_hash(source)
    candidate_bytes = candidate.tobytes()
    source_bytes = source.tobytes()
    stride = len(source.getbands())
    mask_values = mask.tobytes()
    drift_before = sum(
        source_bytes[i*stride:(i+1)*stride] != candidate_bytes[i*stride:(i+1)*stride]
        for i, value in enumerate(mask_values) if value == 0
    )
    output = Image.composite(candidate, source, mask)
    output_bytes = output.tobytes()
    drift_after = sum(
        source_bytes[i*stride:(i+1)*stride] != output_bytes[i*stride:(i+1)*stride]
        for i, value in enumerate(mask_values) if value == 0
    )
    assert drift_after == 0, "Local compositing failed protected-pixel invariant"
    assert _pixel_hash(source) == source_hash, "Unexpected mutation of source"
    report = PreservationReport(
        source.width, source.height, source.mode, source.width * source.height,
        protected, allowed, soft, drift_before, drift_after, source_hash,
        _pixel_hash(output)
    )
    return output, report


def run_files(source_path: Path, candidate_path: Path, mask_path: Path,
              output_path: Path, *, registered: bool, allow_soft_mask: bool = False,
              replace: bool = False) -> PreservationReport:
    """Writes lossless PNG atomically, verifies the encoded/decoded pixels."""
    source_path, candidate_path, mask_path, output_path = map(
        lambda p: Path(p).resolve(), (source_path, candidate_path, mask_path, output_path)
    )
    if output_path.suffix.lower() != ".png":
        raise ValueError("Output must be PNG; lossy JPEG breaks pixel equality")
    if output_path in (source_path, candidate_path, mask_path):
        raise ValueError("Output must not overwrite source, candidate or mask")
    if output_path.exists() and not replace:
        raise FileExistsError("Output already exists; use --replace explicitly")
    with Image.open(source_path) as src, Image.open(candidate_path) as cand, Image.open(mask_path) as m:
        result, report = preserve(src, cand, m, registered=registered,
                                  allow_soft_mask=allow_soft_mask)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = None
    try:
        with NamedTemporaryFile(dir=output_path.parent, suffix=".png", delete=False) as fh:
            tmp_path = Path(fh.name)
        result.save(tmp_path, format="PNG")
        with Image.open(tmp_path) as round_trip:
            if round_trip.mode != result.mode or round_trip.size != result.size or \
                    round_trip.tobytes() != result.tobytes():
                raise ValueError("PNG roundtrip changed the decoded composited pixels")
        os.replace(tmp_path, output_path)
    finally:
        if tmp_path and tmp_path.exists():
            tmp_path.unlink()
    return report


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--source", required=True, type=Path)
    p.add_argument("--candidate", required=True, type=Path)
    p.add_argument("--mask", required=True, type=Path,
                   help="L-mode PNG: zero=protected, 255=editable; not an AI provider mask")
    p.add_argument("--out", required=True, type=Path, help="Lossless output PNG")
    p.add_argument("--registered", action="store_true",
                   help="Operator has verified same oriented pixel/camera frame")
    p.add_argument("--allow-soft-mask", action="store_true")
    p.add_argument("--replace", action="store_true")
    args = p.parse_args()
    report = run_files(args.source, args.candidate, args.mask, args.out,
                       registered=args.registered, allow_soft_mask=args.allow_soft_mask,
                       replace=args.replace)
    print(json.dumps(report.as_dict(), sort_keys=True, indent=2))


if __name__ == "__main__":
    main()
