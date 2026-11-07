# G6 image registration and boundary-quality diagnostic

**Status:** privately executed exploratory evaluation on the owner-supplied original room image and two independent earlier first-attempt results, accompanied by a **public metrics-only reusable diagnostic** and synthetic tests. This is **not** a licensed public image corpus, verified camera calibration, approved edit mask or aesthetically accepted final composite. Date 2026-11-06. Source file bytes remain outside GitHub.

## Question and falsifier

**D-G6-REGISTER:** Can Retrofit safely assume that an assistant-produced concept is in the same oriented pixel grid as the original, and can local protected-pixel copying alone guarantee a convincing edge around the changed machinery?

**Answer:** No. Even two visually similar independent outputs were **not the same pixel height** as the original. Feature-based registration can suggest a near-identity transform from stable architecture, but matching sparse keypoints does **not** prove geometric/semantic alignment or an aesthetically clean seam. The [G6 exact-pixel compositor](PIXEL-LOCALITY-SPIKE.md) should still refuse to composite before an independent registration-and-mask review.

- **Counterexample to 'visual similarity implies identical raster geometry':** original JPEG 1536×1023, generated PNG A 1536×1022, generated PNG B 1536×1024. Exact-pixel preservation requires bringing them into a controlled common *decoded* pixel frame. Simply resizing changes/interpolates the candidate and does not certify a shared camera.
- **Counterexample to 'protected pixels imply seamless image':** a user-provisional tight rectangle around only the boiler puts the old photograph outside the rectangle and the generated content inside. Differences in lighting, finish, shape and pipework can leave discontinuities along the boundary, especially if replacement geometry exceeds the chosen region.
- **Negative case:** an incorrect alignment assertion or too small an edit region may produce **0 protected-pixel drift** and still be a visibly wrong replacement. The mask, registration and output must be reviewed independently before any user/client presentation.

## Local exploratory pilot (no media published)

| Property | First attempt | Independent strict-prompt attempt |
| --- | ---: | ---: |
| Candidate SHA-256 prefix | `7cd8dbdb836f610e` | `b4da0beb33f9d395` |
| Original image SHA-256 prefix | `ed8411cda133cfbe` | same |
| Candidate size | 1536 × 1022 | 1536 × 1024 |
| SIFT good matches outside boiler area | 547 | 855 |
| RANSAC partial-affine inliers | 486 | 774 |
| Median inlier keypoint residual | 0.76 px | 0.61 px |
| Protected pixels initially differing after exploratory registration | 1,511,355 | 1,509,243 |
| Protected pixels after *local copy from original* | **0** | **0** |
| Diagnostic inner-mask-boundary mean absolute per-channel difference (0..255) | 25.2 | 17.4 |
| Inner-mask-boundary 95th percentile per-channel difference | 75.9 | 55.7 |

**Method:** read the already-uploaded private original and independent PNG results with OpenCV 4.13.0. Original used as 1536×1023 decoded pixel authority; resize each candidate for *diagnostic analysis only*, then SIFT matching outside a broad excluded boiler/pipe region (x≈230..590, y≈445..840), Lowe ratio < 0.7, RANSAC partial-affine threshold 3 px. For an unapproved **diagnostic** edit rectangle (x=300..458, y=533..797), use estimated registration, report protected RGB changes and evaluate 7px-thick inner boundary channel differences, then copy exact original decoded pixels outside the rectangle. The source/candidate images were not modified or uploaded. These rough values depend on OpenCV version, operator mask and excluded features. The generalized script below uses a width-dependent clearance rather than this initial broader manual exclusion, so its match counts and estimates can differ; do not present them as a certification or an exact cross-implementation golden vector.

**Key interpretation:** the inlier residual is on **matched features**, not a scene-wide error bound, and the seam metric is an exploratory color difference, not a perceptual MOS score. Neither proves that pipes, equipment details or labels are correct. Original, generation and mask approvals remain separate.

## Reusable source-free diagnostic

- [`experiments/pixel_locality/registration_diagnostic.py`](../experiments/pixel_locality/registration_diagnostic.py) accepts source, candidate, operator-defined rectangle and an *explicit* `--diagnostic-resize` for exploratory size mismatch. It estimates only a partial affine transformation using outside-region SIFT keypoints, enforces feature/inlier thresholds, and reports counts, residuals and inner-boundary color mismatch. It **does not output images** or write descriptors; optional report is JSON measurements and SHA hashes.
- [Synthetic positive and hostile tests](../tests/test_registration_diagnostic.py) exercise controlled translation, inadequate features, out-of-bounds mask, size mismatch and explicit diagnostic resizing. Pinned [OpenCV headless + Pillow](../experiments/pixel_locality/requirements.txt) used only for host research, not introduced into the Android APK.
- [Pixel locality CI](../.github/workflows/pixel-locality.yml) runs this test alongside the original seven host compositing tests. Independent GitHub result is pending until the PR CI passes. Real private photos are not in CI.

Example for privately stored, authorized images in the user's own environment:

```sh
python -m pip install -r experiments/pixel_locality/requirements.txt
python -m unittest discover -s tests -p test_registration_diagnostic.py -v
python experiments/pixel_locality/registration_diagnostic.py \
  --source ORIGINAL.jpg --candidate INDEPENDENT-RESULT.png \
  --rect 300 533 458 797 --diagnostic-resize
```

**Safety/interpretation:** `--diagnostic-resize` explicitly permits exploratory interpolation but does **not** authorize final composite or imply camera registration. The operator-selected rectangle is not inferred geometry or rights approval. Reproduced values may vary by OpenCV version and mask/exclusion, but protected-pixel copying is mechanically exact for the decoded original after same-frame validation.

## Decision and cheapest next independent evidence

**G6 verdict:** exact pixel preservation remains a supported E1+experimental native E2 primitive; **registration and visual seams remain SPIKE NEXT**. The next valid test requires an owner-approved region around a known replacement and two actual image outputs in a common registered frame, with separate evaluation of edge placement, protected equipment, pipe continuity, shadows and whether generated equipment remains plausible. A purely automated low residual is insufficient.

No need for client-site upload to continue regression tests with synthetic translated/rotated fixtures. No move to a cloud service, proprietary vision model, automatic rerender or bulk licensed catalog is justified by this experiment.
