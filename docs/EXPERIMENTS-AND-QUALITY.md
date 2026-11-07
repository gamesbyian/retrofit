# Retrofit evaluation, experiments and evidence

**Status:** executable evaluation protocol; tests are *designed*, not claimed to have run. Date: 2026-11-06. Single authoritative queue [#1](https://github.com/gamesbyian/retrofit/issues/1); product contract in draft PR #2; proposed design [System Blueprint](SYSTEM-BLUEPRINT.md).

## Truth and evidence tiers

- **E0 requirement / direct source:** owner statement, screenshot, source code, published API terms. Identifies a claim, not working behavior.
- **E1 static/deterministic:** schema validation, unit tests for coordinate transforms, file hashes or pixel compositing.
- **E2 installed Android:** Photo Picker -> imported bytes -> annotation -> relaunch -> identical source/coordinates and all required recovery paths.
- **E3 independent receiver:** separate Android receiver checks AYA-approved share bytes/hashes and attachment count; exact source SHA/APK checksum recorded.
- **E4 real third-party assistant:** on the actual handset/app version, recipient demonstrably ingests *all* selected images and follows intended operation; resulting image manually imported and compared.
- **E5 owner field trial:** on authorized mechanical-room images, user completes an actual sales presentation journey; report usability and uncorrected failure.
- **E6 distribution/update:** stable signing and upgrades retain source and existing projects.

A green CI build is not E2; a chooser launch is not E3; a photorealistic result is not evidence of correct piping, safe clearances or system design.

## Exploratory uncontrolled owner-image fidelity audit (2026-11-06)

The two previously supplied independent *first-attempt* boiler proposals were checked against the same original. This is a **diagnostic observation**, not a head-to-head causal prompt evaluation and not a qualified edit-mask oracle. No images or customer-site photographs were added to the public repository.

| Artifact (private owner upload) | Original pixel size | SHA-256 |
| --- | --- | --- |
| Original photograph (JPEG) | 1536 × 1023 | `ed8411cda133cfbeb55133b1d397a17e0d2ba7862330c1c59c2d55ee914354c1` |
| First generated proposal (PNG) | 1536 × 1022 | `7cd8dbdb836f610ef933f7119367152b8be31d8790f76bcbcca4f04c3074bccf` |
| Strict-prompt independent proposal (PNG) | 1536 × 1024 | `b4da0beb33f9d395a161c818748e403393a655085e77a67a2e9aa8d14800703f` |

For a coarse exploratory calculation only: convert all to 8-bit RGB, bicubic-resize to 1536 × 1024, then compare visually *unmodified* fixed crops with scikit-image SSIM (Pillow 12.3.0, scikit-image 0.26.0). Coordinates below are `x_min,y_min,x_max,y_max` in the resized image, and the two SSIM values correspond to first / strict-prompt results:

| Region | Crop | First / strict SSIM |
| --- | --- | --- |
| Central structural column | (690,245,754,906) | 0.977 / 0.979 |
| Rear manifold | (780,370,935,560) | 0.740 / 0.814 |
| Unmodified right-side floor | (830,850,1120,978) | 0.194 / 0.207 |
| Controls at left wall | (150,400,220,600) | 0.735 / 0.742 |

**Interpretation:** the column appearance survives rather well, while some seemingly untouched textures and details do not, consistent with regenerated image content outside the requested boiler replacement. The original/output dimensions differ; bicubic resizing, JPEG-vs-PNG decoding, slight registration shifts, selected crop boundaries and generative changes all affect these values. SSIM neither identifies *why* an edit drifted nor establishes engineering correctness. It does not prove that the strict prompt or wireframes improved results (different first-attempt agent contexts and no held-constant variables). The future **exact preservation** gate must use a registered, common oriented pixel canvas with deliberately composited unmasked regions and compare decoded pixels from that local output to its exact source pixel array, not these resized files.

## Independently executed bounded locality primitive (E1, synthetic)

A separate [G6 pixel-locality spike](PIXEL-LOCALITY-SPIKE.md) tests exact preservation on *decoded oriented pixels* by compositing a same-frame generated candidate into a synthetic source through an explicit local allow-change mask. Seven local unit/negative-control tests passed. The 512 × 320 synthetic experiment deliberately drifted **154,930 protected pixels** in its candidate; the locally composited lossless PNG had **0 protected pixel differences** and retained changes in **8,910 allowed pixels**. Run source, tests and artifacts are in [`experiments/pixel_locality`](../experiments/pixel_locality/); a small independent CI job checks reproducibility.

**Scope:** deterministic host-side Python E1, with an *assumed aligned* synthetic candidate. No actual image model, Android app, physical equipment, client photo, segmentation or human visual judgement was involved. A zero protected-pixel difference does not validate camera registration, material/lighting continuity, correct equipment, pipe connections or engineering feasibility. PNG output is required; unknown or visually wrong masks can preserve the wrong scene. The local L mask is not automatically a provider-compatible API mask. Treat result registration, mask review, shadow/connection allowances, feathering and Android-memory performance as distinct next tests.

## Source assets and fixture protocol

1. **Preserve the provided original image** in app-private/project storage with SHA-256, oriented dimensions, verified source/provenance and explicit permission to reuse it in evaluations. Keep original unmarked image separate from faded, wireframe, equipment callout and generated result. Avoid adding unlicensed real client-site photos to the public repo.
2. Record capture ID, category (simple/cluttered/occluded), source image orientation and hashes, any hidden ground truth, equipment labels known vs guesses and authorized transfer destinations.
3. Build a small deliberately diverse corpus before broad model comparison: original mechanical-room example plus a second layout, one confined/cluttered angle, one with partly hidden equipment, one with readable tiny text/signage, and one with unusual lighting. No invented precision ground truth: use verified physical measurements where real measurement error is scored.
4. Maintain clearly separable **internal private corpus** and **synthetic/permission-cleared public fixtures**. Test report may store hashes and metrics without leaking original images.
5. Do not submit owner photos to a remote model without explicit review of destination and exact bytes, even during research.

## Exact owner three-stage prompts (verbatim baseline)

```text
Please substantially fade and mildly blur this image
```

```text
Please generate a wireframe of this room's floor, walls and ceiling and support beam, even where they are obscured by equipment or objects, and overlay it in black onto the faded version of the image
```

```text
Please generate a wireframe of this room's equipment and large objects, with identification labels or guesses where needed, and overlay it in black, onto the faded version of the image
```

The owner also tested a short boiler-replacement request and a much longer strict-preservation prompt on **different first-attempt agents**. Both produced convincing concepts, both also changed unrelated details including text. Preserve the actual source prompts and results if permission allows, separately from any retrospective prompt revisions. An analysis about an error in one output must not contaminate the wording of a supposedly independent first-attempt test.

## Controlled composition design

Keep independent variables orthogonal.

**Survey input experiment (one instruction text held constant):**
- A: original image + requested replacement only.
- B: same original + architectural wireframe + identical replacement instructions.
- C: same original + architectural and equipment wireframes + identical instructions.

**Preservation instruction experiment (same attachments held constant):**
- S: short, concrete replacement request with explicit unmodified-area preservation.
- T: long strict-preservation instructions as previously authored.

**Optional mask experiment (separate):**
- U: prompt-only boundary.
- M: genuinely provider-enforced editable mask (if supported).
- L: output registered and locally composited against the original with user-approved region.

Don't compare A+S against C+T and attribute the difference to wireframes. Use independent fresh contexts, randomized run order, repeated first attempts (suggest at least five per condition for an initial directional signal, not power/clinical significance), identical model/version/settings and source assets where technically available. If seed control or provider non-determinism differs, record it; do not invent reproducibility. No corrective second prompts. If constraints prevent a full factorial, report the confounding clearly.

### Measurement / scoring

For each output store `experimentId`, fixture hash, source dimensions, reference hashes, prompt ID/content hash, provider name/version/date, settings, recipient attachment inventory, output bytes hash and evaluator. Score each criterion separately:

| Measure | Direct observable oracle | Typical failure |
| --- | --- | --- |
| Target success | Requested old item replaced and new one distinguishable, in correct region | Wrong target, partial replacement |
| Source fidelity | Difference outside authorized region (with registered images), protected object ID/location, source labels/text | Changed signs, manifold, floor texture |
| Geometry | Fixed camera framing, column/wall/ceiling alignment, size and occlusion | Perspective drift, misplaced column |
| Equipment identity | Catalog model matches supplied references; no invented brand or markings | Hallucinated model/nameplate |
| Connection restraint | Only allowed pipe/vent changes; no disconnected or unexplained paths | Invented or impossible pipework |
| Local edit compliance | Exact pixel equality outside permitted mask when **local compositing** claims it | Global repainting or mismatched output canvas |
| User work | Correction time, clicks, ambiguity, successful completion | More effort to correct than manually annotate |
| Disclosure | Image labelled conceptual and unverified, project/source/revision traceable | Proposal appears as an engineered drawing |

Automatic pixel differences require aligned/oriented images with known interpolation and compression characteristics. Unmasked-pixel **identity** is checkable only on the final locally composited pixel array against the same decoded oriented source, not arbitrary recompressed files. Human visual inspections should be blinded to condition where practical and separately document crisp text and piping; no single aesthetic score substitutes for fidelity.

**Decision rule:** Keep wireframe-assisted generation as an optional aid unless repeated, source-controlled experiments improve preservation/placement enough to justify the added waiting, correction and disclosure burden. If masks/local compositing are the only reliable fidelity mechanisms, make them first-class rather than relying on ever-longer prompts. Negative result may justify wireframes for the **human editor** even if generator quality is unchanged.

## Scene extraction and selection comparison

**Candidates:** manual polygon/box baseline; Google MediaPipe Interactive Segmenter on Android; SAM 3/3.1 off-device feasibility only if license/hardware allow; optional depth (Depth Anything V2 Small / ARCore on supported live capture). Compare on the exact fixture set.

For each: measure object boundary quality (manual review and mask overlap where ground-truth region exists), boiler/piping interference, failure on partial occlusion, user correction burden, on-target device CPU/RAM/battery/latency, offline behavior, model size, redistribution/AI-sharing rights and failure recovery. A clean mask is not evidence the object is correctly classified. A model-suggested name is unverified until owner confirmation or supporting nameplate/manufacturer evidence.

**Exit:** choose minimum viable selection mechanism. Manual regions are a supported fallback even if every model fails. Do not ship remote inference or bundled weights on the basis of a desktop demo.

## AYA full-path acceptance

- Freeze exact draft revision and reviewed outgoing attachment inventory with file name, MIME, length, SHA-256, dimensions and selected reference angles; verify schema and local bytes.
- Invoke released/qualified AYA adapter after explicit consent; do not duplicate AYA's transport.
- With independent external test receiver, assert actual received image count and exact hashes/content for all approved attachments, including a large but supported packet; reject missing or substituted images.
- Test cancel, navigation, process death before/during/after share, revoked grants, invalid/deleted media, oversized source, stale review, missing/duplicated asset and hash mismatch; no auto retry.
- Characterize each external assistant separately: app/version/device, supported attachments, whether it actually ingests source and references, whether it respects coordinates, whether usable file can be saved. Text-only copy/ZIP ignored by recipient is not a successful AYA photo handoff.
- Import the real resulting media only through user-approved picker; preserve original and identify originating snapshot; before/after shows correct image/orientation.

## Installed editor and usability acceptance

Photo Picker and private copy must produce immutable source bytes; cancellation preserves old draft; letterbox tap ignored; marker anchors survive fit/zoom/pan, orientation change, launch termination and re-open; same app screen is operable at narrow width and 200% system text with accessible labels, large targets and non-gesture alternatives. Test alternate image ratios, EXIF rotations, memory pressure and 20 MiB budget. Hash failure must be actionable and must not destroy active room.

Once catalog exists: detect model ambiguity, wrong angle, unsourced photo, incompatible rights, duplicate model variants and missing image bytes. Once layers exist: visibility/fading/blur must not change coordinate authority; inferred walls visibly distinct; user correction persisted. Once revisions exist: before/after alignment verified; rejecting a proposal cannot overwrite accepted source or alter underlying catalog identities.

## Report template and stopping rule

Record **Outcome | inputs and SHA | implementation/provider revision | executed cases | real evidence tier | measurements / failure examples | cost & latency | decision changed? | remaining unknowns**.

Stop a research experiment when it distinguishes the live architectural alternatives or reveals a clear blocker; don't endlessly optimize image prompts without an enforceable editing boundary. An unexplained good-looking image alone does not pass any functional stage.
