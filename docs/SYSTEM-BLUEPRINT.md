# Retrofit system blueprint: proposed product and implementation contracts

**Status:** proposed design, subject to [discovery gates](DISCOVERY-GATES.md), **not an implementation claim**. Date: 2026-11-06. Product-specific requirements already approved in the draft [PR #2 contract](https://github.com/gamesbyian/retrofit/blob/feat/photo-annotation-foundation-20261106/docs/PRODUCT-CONTRACT.md) remain the owner-derived baseline until consolidated. This blueprint expands that baseline without silently granting permission to use third-party assets or making untested AI capabilities mandatory.

## Product purpose and boundary

Android-first, local-first field tool to prepare **conceptual** visual proposals for building-services renovations. User imports or captures a mechanical-room photograph, identifies features/positions, chooses real reference equipment, describes a controlled renovation, explicitly approves its outgoing content via AYA, imports the result only by explicit user action, and presents a traceable before/after comparison.

Retrofit owns projects, local source/reference files, catalog selection, annotations, editable scene proposals, validation, frozen share snapshots, locally imported results, and provenance. AYA owns optional reviewed assistant/attachment handoff. External assistant owns generative image creation. Engineering review and installation feasibility are outside the application's authority. No implicit server, model-training permission, measured-clearance claim, auto-upload, shared accounts or compulsory spatial scanning.

## Capability levels and release boundaries

| Capability | User-visible contract | Delivery status |
| --- | --- | --- |
| Photo/marker foundation | Import one existing image privately, label A/B/C, edit operations/text, zoom/pan, persist/restores source and draft | Draft PR #2, [API 28 and 35 installed smoke passes](https://github.com/gamesbyian/retrofit/actions/runs/42327983645) including API 35 picker/process-kill. Physical device, full UX/accessibility and AYA remain unverified |
| Catalog-backed handoff | Find/choose real equipment and multi-angle references, review exact bytes/identities, share through verified AYA adapter | Planned after installed foundation; AYA PR #151 unmerged |
| Revisions and comparison | Import user-selected result without changing original, distinguish revisions, inspect before/after | Required upcoming, not shipped |
| Presentation layers | Local adjustable fade/blur, overlay visibility, text labels, hand-edited polygons/regions | Recommended next; no need for AI |
| AI scene survey | Optional agent-produced geometry/object suggestions with correction, provenance, structured validation | Experimental, not assumed automatic |
| Region editing | Editable masks/soft edges, tightly bounded compositing with identity and drift review | Experimental; provider mask capability must be proven |
| True 3D/BIM | Calibrated camera, metric room model, registration across views, engineering tools | Deferred; separate evidence, cost and user need |

Detailed proposed mobile interactions, modes, failure and screen-reader states are documented in [UX Interactions](UX-INTERACTIONS.md); the provenance-first source/catalog path is in [Catalog Sourcing](CATALOG-SOURCING.md) and the [five-model source qualification](CATALOG-FIVE-MODEL-SCOUT.md).

## Field journey and screen map

1. **Projects**: current/draft rooms and recent proposals, clear local status; first prototype may open directly into a single room. Later project list must not replace the current draft silently.
2. **Capture/import**: Photo Picker, optionally camera after feasibility; show privacy notice where appropriate. Copy selected bytes to app-private storage before grant expiry. On cancellation/invalid import, leave draft unchanged.
3. **Inspect/photo editor**: full photo is the canvas; pinch zoom/pan; add/remove/move A/B/C markers; optional object/region selection; on-screen controls always provide non-gesture equivalents. Display layers independently, photo fading/blur as non-destructive preview. A tap in image letterbox must not create geometry.
4. **Specify change**: for each target, select action, optional catalog asset/view choices, free text and preservation constraints; explicit item identity and target. Show overlap/conflict before share. Do not require an AI scene survey or 3D model.
5. **Catalog**: category/type search, manufacturer/model/variant identity, reference photos with angle labels and source rights, physical envelope when supported by a source. Missing reference cannot be filled with a model hallucination; generic item must be explicitly designated as generic.
6. **Review handoff**: frozen revision, exact oriented original, optional user-approved presentation overlays, selected references, complete operation inventory, byte counts/hash/source and privacy disclosure. Share only on explicit user action. Accessible cancel/back never silently submits.
7. **External agent**: system chooser / AYA transport is an attempt; recipient ingestion/generation unverified until separately observed. No background polling, automatic retries or implicit return contract.
8. **Proposals**: user explicitly chooses generated image to import, verify supported bytes, compare source and proposed image using aligned slider/zoom, review localized and unrelated differences, add optional note, accept or reject. Record originating share snapshot.
9. **Presentation/export**: source versus labelled Concept Visualization; preserve provenance and non-engineering disclaimer. Export policy, formats and client workflow require field research.

**Minimum states:** empty, loading, dirty but saved/pending save, save failure, invalid/revoked image, selecting source, canceled, catalog reference unavailable, unverified identification, share review, share canceled, share attempted, recipient unknown, result pending user import, result rejected, result accepted. Never label a launched chooser "image delivered."

## Canonical data model (typed and provenance-aware)

The following is a **logical model**, not an instruction to create all tables now:

```text
Project[id] 1 --- * Room[id, optional location label]
Room 1 --- * Capture[id, originalBlobHash, orientedDerivativeHash,
                      orientedWidth, orientedHeight, exifPolicy]
Capture 1 --- * ViewLayer[id, kind, source, opacity, blurPx, visible]
Capture 1 --- * SceneObject[id, type, geometry, provenance, reviewStatus]
Capture 1 --- * Annotation[id, label, geometry, explicitTargetOrRegion]
Annotation/SceneObject 1 --- * ChangeIntent[id, operation, assetId?,
                                              instructions, protectedTargets]
CatalogAsset[id, canonicalManufacturer?, model?, variant?, category]
CatalogAsset 1 --- * AssetReference[id, angle, mime, sha256, dimensions?,
                                     originURI, rights, redistribution?,
                                     externalAiTransferAllowed?]
Room 1 --- * FrozenHandoff[id, sourceRevisionHash, manifestHash, assetHashes,
                           instructionSnapshot, consentRecord, attemptedAt?]
FrozenHandoff 1 --- * Proposal[id, bytesHash, importedAt, reviewState, resultNotes]
```

The logical field `externalAiTransferAllowed?` is a proposed downstream source-specific authorization claim, **not** a grant inferred from a URL or downloadable CAD file. The manufacturer catalog scout also demonstrates that model identity requires family + exact variant + lifecycle period + source revision, with technical-claim provenance distinct from media-rights evidence; see [five-model source qualification](CATALOG-FIVE-MODEL-SCOUT.md). Do not silently choose one conflicting dimension or assume unknown rights permit bundling or AI transfer. A project can contain multiple photos/views without declaring them spatially registered; cross-view links are explicitly tentative until calibrated or confirmed.

### Provenance and geometry

- Immutable **original**: exact original bytes + hash and import source metadata, kept privately. **Oriented derivative**: user-visible coordinate authority, with explicit transform/source relation and its own hash. Never conflate original hash, transformed derivative or low-resolution preview.
- All 2D geometry uses the **oriented image's complete pixel rectangle**, normalized (x,y) in [0,1], (0,0) upper-left; independent of viewport, fit, pan/zoom, decoded thumbnail and blur. Points, rects, polygons and masks must share reproducible transforms. A 2D pixel address is not a world coordinate.
- **Scene objects** have stable IDs and geometry (polygon/mask/bounds + optional occlusion order), semantic category/labels, source evidence, and independent `identificationStatus` (unknown, suggested, user-confirmed). Add `geometryStatus` (visible-observed, inferred-occluded, user-confirmed) and `measurementStatus` (none, approximate, calibrated/externally measured) separately. Use explicit unknown rather than invented numeric confidence.
- Inferred room planes may have dashed lines; keep architecture and equipment overlays separable. A flattened AI wireframe is a **reference image**, not a trusted vector scene. Only import structured data after validating schema, source dimensions, object IDs, geometry bounds, relation consistency and user corrections.
- A catalog model identity requires sourced manufacturer/model/variant and verified references. A visual resemblance does not establish a product SKU. Catalog dimensions have units, **applicable product variant, document revision, physical envelope definition, evidence source and conflict status**. Historical and newer product generations may use related names but incompatible geometry. A vendor CAD/marketing view is not automatically a redistributable photo or AI-shareable reference. Do not assert installation viability from product envelope alone.
- **ChangeIntent** references an existing object or designated free-placement region; supports desired `keep/add/replace/remove/move/clean/modify` in the *internal* roadmap. `keep` is usually preservation, not a mutation. See the AYA v1 operation limitation below.
- Frozen handoffs reference exact source and selected reference file bytes, schema version and instruction text, never the live editable fields. Returned image is its own user-accepted proposal and cannot overwrite source.

## Architectural modules and boundaries (recommended)

```text
Android UI (Compose)
  Project/Capture | Photo Canvas | Catalog/Intent | Review | Proposal Compare
       |                    |                |
   Project State ------ Scene Editor ------ Catalog Repository
       |              (pure geometry)            |
   Local media / persistence ---------- Asset provenance/rights
       |
   Frozen Composition Packet Builder (validation + SHA-256 + review)
       |
   AYA spatial composition adapter (separate project, reviewed approval)
       |
   Android chooser -> external assistant [recipient ingestion unverified]
       |
   explicit user file import -> Revision Repository -> Compare/Export
```

- Use Android Photo Picker and app-private immutable copy; no broad photo permission for the first slice. Use standard Android persistence/serialization. Draft PR #2 uses a one-room atomic JSON snapshot; migrate to Room only when actual multi-project/catalog/revision relations demand it, with migration acceptance. No second simultaneous mutable authority.
- Canvas: Compose transforms shared by image + annotation primitives; retain unzoomed image geometry. Local blur/fade layer should be generated deterministically and cached or displayed as GPU effect without reducing recognition authority; image ML uses original oriented derivative. **Android API caveat:** Compose uniform `Modifier.blur` requires API 31+; PR #2 minSdk is 28, so a deterministic preblurred bitmap/alternative must be used on older supported Android versions or the support policy intentionally changed, not silently display an unblurred image.
- Keep scene editing separated from AYA packet serialization, provider mechanics, catalog scraping/import and model inference. Service boundary for optional AI scene survey is opt-in reviewed handoff, not invisible network access.
- Defer on-device ML runtime selection until segmentation performance/rights tests. No automatic model download or GPU dependency in base photo/marker flow. Evaluate Google ML Kit on-device **OCR of actual nameplate photos** as an optional model-identity suggestion; require confirmation and manufacturer/variant evidence before catalog linking, and do not silently accept OCR as a verified product name.
- Treat record updates and file operations atomically: canceled/failed import leaves old revision; no dead or half-imported media references; handle process death, missing/changed files and stale pending work.
- Support readable labels, 48dp tap targets or greater, large text, TalkBack, non-color-only status, keyboard accessibility, narrow phones and landscape/rotation. Prove against installed UI rather than relying only on Compose previews.

## AYA compatibility and unresolved extensions

Normative [AYA spatial-composition/v1](https://github.com/gamesbyian/AYA/blob/main/docs/spatial/PACKET.md) accepts a source photograph, optional overlay, reference assets, point/rect/polygon annotations, and operations `replace/add/remove/clean/modify`. It **does not promise** structured scene survey return, measured geometry, mask-encoded edits, or move/keep as distinct actions. Preserve a lossless internal model. Map `keep` to constraints/protected annotations only after review; do **not** silently encode `move` as another operation with changed meaning. A meaningful movement operation requires an explicitly reviewed instruction or an AYA contract extension. Version extensions with AYA, not a Retrofit-only invented protocol.

Packet validation: paths stay under private staging root, regular allowed files, dimensions/orientation accurate, exact SHA-256/byte count/media type, unique stable IDs, operation references and review of overlapping target regions. Staging/share is only approved when user sees the actual image/reference inventory and contents. AYA Android multi-image adapter [PR #151](https://github.com/gamesbyian/AYA/pull/151) must be merged/qualified before Retrofit claims the transport works. The external agent's actual support for multi-image / ZIP and attachment interpretation is separately tested. No unsupported silent fallback.

### Concrete AYA Android adapter integration contract (inspected against AYA PR #151, 2026-11-06)

The *unmerged* [adapter implementation](https://github.com/gamesbyian/AYA/blob/1f082ab74362a952f4d92d571b00051ee5de1649/aya/src/main/java/dev/aya/android/AyaSpatialHandoff.kt) exposes `AyaSpatialHandoff.launch(activity, AyaSpatialShareTask(manifestJson, images))`; each `AyaSpatialImage(path, contentUri)` must reference a validated manifest path and a readable **`content:` URI**, not an app-private `file:` URI. Retrofit will need a documented host-owned read grant/URI strategy, or a retained FileProvider for private original/derived/reference media, when it implements packet assembly. Do not invent an API key, file-path passthrough or AYA-hosted media store.

The adapter requires an installed-version-verified `aya-guide.md` bundled asset (or validated explicit guide path) and checks manifest shape, image hashes, byte lengths, magic signatures and dimensions. It presents a second explicit user confirmation before staging any bytes, and returns `AyaBundledLaunchResult.PreviewOpened` merely when its local review dialog opens. The host must show **actual thumbnails/visual source and privacy/redaction review** independently: the AYA dialog currently lists paths, hash prefixes and operations and allows full manifest inspection, but does not preview image pixels. Any edited directions in AYA's review are incorporated into the reviewed manifest, so Retrofit's frozen pre-share intent is potentially different from the recipient-facing approved manifest. **The current adapter API exposes no approval callback or effective-manifest receipt**, so Retrofit cannot claim it has stored the exact user-edited outgoing instructions or even observed Share being pressed. This has been raised on [AYA PR #151](https://github.com/gamesbyian/AYA/pull/151). Before provenance-backed proposal linkage ships, obtain an AYA-approved callback/event or other verified receipt. Until then record the local pre-share snapshot, mark final outgoing content/recipient effects **unverified**, and never infer final approval from `PreviewOpened`.

A confirmed share constructs `ACTION_SEND_MULTIPLE` with a JSON manifest plus selected image `content:` streams and temporary read-only grants via FileProvider. The current implementation has no ZIP fallback, no observed AI-recipient ingestion guarantee and no automatic return. Host acceptance therefore separates: **preview opened -> user approved -> Android sharesheet launched -> independent receiver observed exact bytes -> third-party AI actually consumed all references -> user explicitly imported result**. No step can silently inherit the next step's evidence. Before adopting, confirm [AYA PR #151](https://github.com/gamesbyian/AYA/pull/151) is merged, released and tested under the exact Retrofit package/version and real target Android recipients.

### Local pixel protection is a bounded demonstrated primitive, not completed editing

The [G6 synthetic locality experiment](PIXEL-LOCALITY-SPIKE.md) proves an **E1 deterministic host-side** proposition: when the candidate, original and user-authorized L-mode mask share an already oriented/registered pixel grid, lossless local compositing preserves every zero-mask decoded source pixel exactly. Seven local tests pass, including rejected mismatches; a deliberately globally drifted synthetic candidate produced **zero** protected-pixel changes after local compositing. This is **not Android code** and does not perform geometric registration or decide a correct equipment/pipe/shadow footprint. Mask review, native performance, provider ingestion and seam quality remain separate obligations. Do not silently resize candidates or conflate a local allow-change mask with a provider's alpha mask.

## Mask-based preservation and AI generation boundary

User chooses an edit region around equipment, plus permitted pipe paths, needed shadows/occlusion/reflection areas. Store mask in oriented image space, with explicit allowed transitions; preview against original. If provider supports mask constraints, pass those; if not, prompt restrictions are *aspirational*. Independently local-composite the output into the **original oriented derivative** with unchanged outside pixels, soft boundaries only where the user permitted them. Require output registration/canvas alignment first, and flag mismatched geometry.

Define outcome separately: requested replacement recognizable; placement and camera plausible; all protected objects/labels maintained; unchanged pixels verified outside mask when that guarantee is claimed. If editing across pipe junctions cannot be safely expressed with a tight mask, widen with explicit approval or disclose that preservation is not enforceable. Never infer that a technically correct installation has been achieved.

## Storage, privacy, asset rights and trust

- Local default: private media, catalog assets, project data, reversible edit metadata and frozen snapshots. Explicit export/import controls. No embedded API keys, telemetry, auto-cloud account or unsolicited background uploads.
- Client-site photos may contain names, faces, labels, alarm panels or access details. Surface concrete preview and optional redaction before transfer. Avoid inferring permission to share from user's possession of the image.
- Catalog asset provenance record: `originalPublisher`, `sourceUrl`, `retrievalDate`, `manufacturer/model`, `licenseEvidence`, `localUseRights`, `redistributionRights`, `thirdPartyAiRights`, `credit/notice`, `version`, `checksum`; unknown rights block bundling/redistribution until resolved. User-provided assets remain user-controlled and subject to their rights.
- Manufacturer BIM/CAD can be a dimension/geometry reference but may require conversion tools and licensing. No assumption that an IFC/RVT asset can be rendered in Compose or that permission to download equals license to ship.
- Don't expose an installable app as an engineering inspection/certification tool. All proposals carry clear conceptual status.

## Design reversals and remaining decisions

1. **Photo-first vs BIM-first:** choose photo-first as the reversible default. Reverse if field observation shows measured engineering layout or exact 3D placement is routinely required to produce a credible proposal.
2. **Local scene selection vs remote model:** prefer local deterministic markup and evaluate on-device segmenter first. Reverse if quality, device latency or model rights fail and explicit approved remote analysis demonstrably improves user outcome.
3. **Curated bundled catalog vs user-import:** start with user-import/licensed small manifests. Reverse once lawful catalog acquisition, distribution and updates are independently established.
4. **AYA-only handoff vs bespoke engine:** preserve AYA authority. Reconsider only after documented recipient requirements prove AYA cannot support a necessary reviewed handoff, and coordinate protocol changes with AYA.
5. **A single photo vs multi-view:** ensure data model can hold more than one photo without assuming registration. Require multiple angles only after field study or known failure cases justify the burden.
6. **Non-engineering concept vs accurate retrofit:** no reversal without actual calibrated measurements and independent professional validation, a different product responsibility.

Next steps, gates and actual evidence tiers are [here](DISCOVERY-GATES.md) and [here](EXPERIMENTS-AND-QUALITY.md).
