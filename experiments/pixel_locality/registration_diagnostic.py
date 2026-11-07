"""Offline registration and prospective seam diagnostic, never an auto-approval oracle.

For a user-supplied source and independently generated candidate, this estimates a
partial affine match using SIFT points **outside** an explicitly specified editable
rectangle. It reports how much protected content was changed in the candidate and
how discontinuous the proposed edit boundary could look after local compositing.
No original photo, generated candidate, composited output or feature descriptor
is saved; JSON metrics and content SHA-256 only. Never uploads to a service.

Matching dimensions and a small RANSAC error do NOT certify a shared camera pose,
valid equipment connections, user-approved mask, photorealism or installation fit.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import cv2
import numpy as np


def _read(path: Path) -> np.ndarray:
    image = cv2.imread(str(path), cv2.IMREAD_COLOR)
    if image is None or image.ndim != 3 or image.shape[2] != 3:
        raise ValueError(f"Cannot decode 3-channel image at {path.name}")
    if image.shape[0] * image.shape[1] > 24_000_000:
        raise ValueError("Input exceeds 24 million pixels")
    return image


def inspect(
    source_path: Path, candidate_path: Path, edit_rect: tuple[int, int, int, int],
    *, diagnostic_resize: bool = False
) -> dict:
    source_path, candidate_path = Path(source_path), Path(candidate_path)
    original, candidate = _read(source_path), _read(candidate_path)
    height, width = original.shape[:2]
    other_height, other_width = candidate.shape[:2]
    changed_size = (height, width) != (other_height, other_width)
    if changed_size and not diagnostic_resize:
        raise ValueError("Different image dimensions: exact preservation requires registration; "
                         "enable diagnostic_resize only for exploratory matching")
    x1, y1, x2, y2 = edit_rect
    if not (0 < x1 < x2 < width and 0 < y1 < y2 < height):
        raise ValueError("Edit rectangle must be nonempty, strictly within the source frame")
    # Resizing before fitting is exploratory. It already interpolates candidate pixels.
    # NEVER treat these interpolated pixels as a verified registered output.
    work = (cv2.resize(candidate, (width, height), interpolation=cv2.INTER_LINEAR)
            if changed_size else candidate.copy())
    mask = np.zeros((height, width), dtype=np.uint8)
    mask[y1:y2 + 1, x1:x2 + 1] = 255
    protected = mask == 0
    # Keep features well away from the edited object and related piping.
    exclude = np.full((height, width), 255, dtype=np.uint8)
    clearance = max(12, int(min(width, height) * .03))
    exclude[max(0,y1-clearance):min(height,y2+clearance+1),
            max(0,x1-clearance):min(width,x2+clearance+1)] = 0
    sift = cv2.SIFT_create(nfeatures=3000)
    key_source, desc_source = sift.detectAndCompute(
        cv2.cvtColor(original, cv2.COLOR_BGR2GRAY), exclude
    )
    key_candidate, desc_candidate = sift.detectAndCompute(
        cv2.cvtColor(work, cv2.COLOR_BGR2GRAY), exclude
    )
    if desc_source is None or desc_candidate is None:
        raise ValueError("Insufficient identifiable features outside edit rectangle")
    pairs = cv2.FlannBasedMatcher(
        dict(algorithm=1, trees=5), dict(checks=64)
    ).knnMatch(desc_candidate, desc_source, k=2)
    matches = [a for pair in pairs if len(pair) == 2
               for a, b in [pair] if a.distance < .7 * b.distance]
    if len(matches) < 20:
        raise ValueError("Too few defensible feature matches to inspect registration")
    candidate_pts = np.float32([key_candidate[m.queryIdx].pt for m in matches]).reshape(-1,1,2)
    original_pts = np.float32([key_source[m.trainIdx].pt for m in matches]).reshape(-1,1,2)
    matrix, inlier_mask = cv2.estimateAffinePartial2D(
        candidate_pts, original_pts, method=cv2.RANSAC,
        ransacReprojThreshold=3.0, maxIters=3000, confidence=.995
    )
    if matrix is None or inlier_mask is None:
        raise ValueError("No usable partial affine registration proposal")
    inliers = inlier_mask.ravel().astype(bool)
    count = int(inliers.sum())
    if count < 20 or count < len(matches) * .5:
        raise ValueError("Low-consensus candidate alignment; do not composite")
    residual = np.linalg.norm(
        cv2.transform(candidate_pts, matrix) - original_pts, axis=2
    ).ravel()
    shifted = cv2.warpAffine(
        work, matrix, (width, height), flags=cv2.INTER_LINEAR,
        borderMode=cv2.BORDER_REFLECT
    )
    prospective = original.copy()
    prospective[mask != 0] = shifted[mask != 0]
    before = int(np.count_nonzero(
        np.any(shifted[protected] != original[protected], axis=1)
    ))
    after = int(np.count_nonzero(
        np.any(prospective[protected] != original[protected], axis=1)
    ))
    if after != 0:
        raise AssertionError("Local copy did not preserve protected source pixels")
    # 7x7 interior boundary shows color mismatch at the proposed seam. This is
    # a warning metric, not calibrated perceptual quality.
    inner = (mask != 0) & (cv2.erode(mask,np.ones((7,7),np.uint8)) != mask)
    channel_abs_delta = np.abs(original.astype(np.int16)
                               - shifted.astype(np.int16)).mean(axis=2)
    seam = channel_abs_delta[inner]
    return {
        "source_sha256": hashlib.sha256(source_path.read_bytes()).hexdigest(),
        "candidate_sha256": hashlib.sha256(candidate_path.read_bytes()).hexdigest(),
        "source_dimensions": [width, height],
        "candidate_dimensions": [other_width, other_height],
        "candidate_resized_for_diagnostic": changed_size,
        "resize_was_not_a_registered_output": True,
        "edit_rect_source_px": list(edit_rect),
        "edit_region_was_operator_specified": True,
        "feature_matches": len(matches),
        "ransac_inliers": count,
        "inlier_median_reprojection_px": float(np.median(residual[inliers])),
        "candidate_to_source_partial_affine": matrix.tolist(),
        "protected_pixel_count": int(protected.sum()),
        "protected_changed_pixels_in_candidate_after_estimated_affine": before,
        "protected_changed_pixels_after_local_copy": after,
        "seam_inside_mean_abs_channel_diff": float(seam.mean()),
        "seam_inside_p95_abs_channel_diff": float(np.percentile(seam,95)),
        "real_registration_or_visual_quality_certified": False,
        "output_media_saved": False,
        "outside_file_transmission": False,
    }


if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--source", required=True, type=Path)
    p.add_argument("--candidate", required=True, type=Path)
    p.add_argument("--rect", nargs=4, type=int, metavar=("X1","Y1","X2","Y2"), required=True)
    p.add_argument("--diagnostic-resize", action="store_true",
                   help="Explicit exploratory pixel resizing; NOT verified registration")
    p.add_argument("--report", type=Path, help="Optional output of metrics only, no image data")
    args = p.parse_args()
    result = inspect(args.source, args.candidate, tuple(args.rect),
                     diagnostic_resize=args.diagnostic_resize)
    serial = json.dumps(result, indent=2) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        if args.report.exists():
            raise FileExistsError("Refusing to overwrite an existing metrics report")
        args.report.write_text(serial)
    print(serial)
