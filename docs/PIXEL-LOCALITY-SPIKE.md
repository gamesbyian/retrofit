# G6 local pixel-preservation feasibility experiment

**Status:** bounded E1 deterministic experiment, not a feature in the Android APK, not a real AI-recipient test and not a proof of photo registration or engineering correctness. Investigated 2026-11-06. Evidence authority: [Experiments and Quality](EXPERIMENTS-AND-QUALITY.md), [System Blueprint](SYSTEM-BLUEPRINT.md), [Discovery Gates](DISCOVERY-GATES.md), [issue #1](https://github.com/gamesbyian/retrofit/issues/1).

## Decision and falsifier

**D-G6-LOCALITY:** When a model rewrites regions outside the intended mechanical equipment replacement, can a completely local image operation guarantee that the *decoded oriented original* remains unchanged outside a user-authorized pixel region?

**A. Prompt/mask alone:** Ask the assistant to preserve all other elements, perhaps send a mask. Less app processing, but the provider's [image-edit guidance](https://developers.openai.com/api/docs/guides/image-generation) explicitly says the edit mask guides generation and may not be followed precisely. No exact-pixel guarantee.

**B. Original canvas + local compositing:** Keep the intact oriented source image, a user-authorized allow-change mask and a same-coordinate generated candidate. Replace only pixels inside the mask, copying source pixels outside. This creates a verifiable zero-drift guarantee *only on protected decoded pixels*, if registration is correct.

**Critical falsifiers:** any changed decoded pixel where the mask is zero, source mutation, lost original after serialization, or silently resized/reoriented candidate. Reject an unregistered, different-size or incompatible image instead of claiming success. A 0-pixel report on a *mistaken region* is a technically valid but semantically wrong edit; the mask requires separate user review.

## Independent script and repeatable tests

Code: [`experiments/pixel_locality/preserve.py`](../experiments/pixel_locality/preserve.py) with [seven unit and negative-control tests](../tests/test_preserve.py), [synthetic room-like demonstration](../experiments/pixel_locality/demo.py) and [separate small CI job](../.github/workflows/pixel-locality.yml). Uses Pillow 12.3.0, a documented [`Image.composite` mask primitive](https://pillow.readthedocs.io/en/stable/reference/Image.html#PIL.Image.composite), and 8-bit **L-mode local allow-change mask** (0=protected, 255=editable). **Do not confuse this with a provider's transparency/alpha edit-mask convention.**

To run from repo root in a disposable environment:

```sh
python -m pip install -r experiments/pixel_locality/requirements.txt
python -m unittest discover -s tests -p test_preserve.py -v
python experiments/pixel_locality/demo.py --out-dir build/locality
```

The third command generates **only synthetic images**, a deliberately globally drifted candidate, explicit binary mask, locally composited lossless PNG, and `locality-report.json`. No OEM photos, customer-site images, API keys, external AI calls or model weights.

For an already oriented, verified registered real *authorized* set, the disposable CLI is:

```sh
python experiments/pixel_locality/preserve.py \
  --source ORIGINAL.png --candidate EDIT.png --mask LOCAL-ALLOW-MASK.png \
  --registered --out SAFE-OUTPUT.png
```

`--registered` is an **operator assertion**, not an image-alignment algorithm or proof. It must only be used after confirming dimensions **and** common camera frame. Output must be **PNG** to retain the decoded pixel equality. The script refuses overwriting an input, refuses an existing output without explicit `--replace`, and uses a same-directory temporary file plus atomic rename.

## Executed local evidence before GitHub submission

Local environment: Python 3, Pillow **12.3.0**. Seven unit tests passed: RGB binary mask, RGBA alpha preservation, explicit opt-in soft-edge blend, unregistered/size/mode errors, empty/all mask and EXIF orientation rejection, PNG roundtrip/overwrite/JPEG safeguards, and negative same-size shifted candidate without a registration assertion.

Deterministic synthetic 512 × 320 RGB demonstration:

| Assertion | Observed |
| --- | ---: |
| Total decoded pixels | 163,840 |
| Protected (mask value 0) | 154,930 |
| Editable (mask value above 0) | 8,910 |
| Drifted protected pixels in deliberately damaged input | 154,930 |
| Drifted protected pixels in composited output | **0** |
| Source decoded-pixel SHA-256 | `020ec30f2376ce8cf19fa773c9fb34f0f70bd01e6158cb6885e94fed58892ae1` |
| Output decoded-pixel SHA-256 | `03120b7d38ac9706b6387aef02ec0e9307f3541b2b9a5124c95e19d8c8660e4d` |

These numbers describe a **synthetic** fixed input, not model performance. Source hash and output hash differ, as they should: changes *inside* the mask remain in the output.

**Evidence tiers:** E1 local deterministic test **executed and independently repeated in GitHub Actions** at [run 42337186590](https://github.com/gamesbyian/retrofit/actions/runs/42337186590), PR #10 head `a9e1f4fd39de4b182c957c34bc5843a44cc8abc9`: **7 tests passed**, synthetic output matched 154,930 protected pixels after deliberately changing all of them in the candidate, and the generated artifact was retained. **Native Android experimental E2** has since been qualified separately in [draft PR #2](https://github.com/gamesbyian/retrofit/pull/2) via [API 28/35 installed CI 42337618234](https://github.com/gamesbyian/retrofit/actions/runs/42337618234) and subsequent [EXIF/geometry regression run 42339019851](https://github.com/gamesbyian/retrofit/actions/runs/42339019851): two native Bitmap tests on each emulator checked exact opaque protected-pixel copying/PNG roundtrip and rejection of invalid mask/frame/alpha. The tests use synthetic 196×128 pixels; no real equipment, streaming performance, external AI, AYA receiver or human fidelity acceptance is implied.

## Why this does and does not solve Retrofit's problem

**Supported in principle:** A protected original pixel can be preserved exactly if the aligned candidate/allow-change mask has the same oriented pixel grid and output is losslessly encoded. This makes a useful **product invariant** and test oracle instead of trusting a preservation prompt.

**Not supported by this result:**

- A generator's candidate may be cropped, zoomed, shifted, or have a slightly changed camera projection **despite the same width/height**. This script detects dimensions and requires an explicit registration assertion, but cannot detect all misalignments. Real registration must be measured or user-reviewed.
- The user-defined allow-change region may omit the necessary new shadows, vents, pipes, occlusions and boiler footprint. Local protection forces a visible seam or leaves part of an old object if the mask is too narrow. Expand the mask only with explicit authorization; local quality/alpha feathering is a separate visual test.
- Image equality is on decoded **oriented source pixels**, not a claim that a newly exported PNG has identical bytes to the original JPEG. Saving the output as JPEG would change protected pixels and is rejected.
- A high score for protected pixels does **not** validate replacement equipment identity, scale, real wiring, hydraulic connections, code compliance, or safe clearances.
- The original host test uses Pillow; a separate experimental Kotlin `LocalPixelProtection.kt` native implementation now has API 28/35 installed evidence. It **does not** establish large-image Android CPU/RAM/thermal behavior, AYA receiver mask support, model semantics, visual seam acceptability or owner-device usability. Its current deliberate cap is **2 million opaque ARGB_8888 pixels**, while the photo-import prototype permits up to 24 million pixels. A working 12 MP edit may need a different, tiled/storage-aware design, not an arbitrary silent downsample. `LocalPixelProtection` currently copies two source IntArrays plus output and readback arrays, with additional original/candidate/result bitmaps: an order-of-magnitude **at least 58 MB** of raw pixel/mask storage at 2 MP and **348 MB** at 12 MP before JVM/native overhead, if scaled naively.
- A blend with soft-edge values 1..254 is opt-in, and **only zero-valued pixels are covered by the exact-preservation assertion**. Feathered pixels are authorized to change.

### Product architecture implication

Retain the immutable oriented original, edit-region definition, its reviewed revision and a separate generated candidate. On result import, validate dimensions/orientation and **registration** before any local composite. Show **concept-only** result, allow a human to expand/refine the editable boundary if needed, and always keep the unfused original and generated candidate for undo/audit. Do not silently auto-resize or pass the local L-mask to AYA as if it were a supported generative mask.

This is an **E1 host + bounded E2 native Bitmap SUPPORTED primitive** transition, while full-resolution memory/tiling, registration, user-approved masks, visual seam quality and real AI-recipient response remain **SPIKE/RESEARCH NEXT**. The [private owner-image registration diagnostic](G6-REGISTRATION-DIAGNOSTIC.md) shows why similar-looking outputs need alignment and seam review, including source/candidate dimension mismatches. Next independent discriminators: (1) tiled/performance behavior and resource budget at 12–24 MP on an actual target Android device, (2) explicitly user-approved edit-region and visually assessed real image composite, and (3) per-recipient mask/markup semantics after AYA's host-manifest receipt is available. These do not justify a large catalog or cloud architecture.
