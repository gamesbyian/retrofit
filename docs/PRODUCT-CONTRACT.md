# Retrofit product and interaction contract

Status: initial owner-authorized engineering baseline; 2026-11-06. This document governs Retrofit's product-specific interpretation. The authoritative execution queue is [issue #1](https://github.com/gamesbyian/retrofit/issues/1). AYA transport lives in AYA, not here.

## Job and core journey

A building-services salesperson photographs or selects an existing mechanical/equipment room, marks specific positions or regions, chooses known equipment models and reference angles, writes plain-language changes, and uses the preferred AI agent to produce a photorealistic **concept proposal** for a client. Nothing in that proposal constitutes mechanical/electrical/plumbing design approval, measurement, safety clearance, code compliance, or vendor-certified installation instructions.

First installable slice: choose an existing room photograph; add/inspect/move/remove lettered point markers; attach per-marker operation and notes; edit global directions; close/reopen the app without losing the room. Subsequent slice adds the real catalog and AYA packet. Do not simulate a completed AYA handoff while its adapter is absent.

## Entities and relationships

- **Room/project** has a stable opaque ID, owner-entered title (later), a private source image, an orientation-normalized working derivative, annotations, editable instructions and proposal revisions.
- **Original image** is immutable app-private copied source bytes, including its EXIF data. Its working derivative is transformed into orientation-normalized pixels and has explicit width/height. Markers refer to the derivative's full pixel space; never to a zoom level, crop, or viewport.
- **Annotation** has a stable ID, display label A/B/C…, and geometry. V1 point is a normalized (x,y) in [0,1]. Future rectangles/polygons use the same orientation-normalized coordinate space. Markers remain attached under fit, pinch zoom and pan.
- **Change/operation** targets exactly one annotation with add, replace, remove, clean or modify. An operation may have additional user text. Add/replace require a selected catalog asset and real user-selected reference image(s) for an AYA export; a free-typed model name alone is not proof of product identity.
- **Catalog asset** identifies manufacturer/model/category and one or more actual sourced, licensed reference views. Display aliases are searchable but do not replace canonical identity. Catalog is local-first, imported from verified manifests, and independent of UI. Never invent model photography to pad a collection.
- **Handoff packet** is a snapshot, not the mutable current editor. It includes exactly the original unannotated orientation-normalized source, an optional annotated overlay, selected references and editable instructions with hashes, lengths and dimensions. Every attachment is approved by the user.
- **Proposal revision** is a separately imported, user-selected image linked to the source draft and handoff snapshot. An assistant cannot silently replace the original, certify work, or insert results into Retrofit.

## AYA contract and boundary

Use the existing [AYA Spatial Composition v1 contract](https://github.com/gamesbyian/AYA/blob/main/docs/spatial/PACKET.md), format aya-spatial-composition/v1. AYA PR [#151](https://github.com/gamesbyian/AYA/pull/151) proposes the Android source-preview adapter, but it is **not** assumed merged, released or recipient-qualified as of this baseline. Do not copy its draft transport code into Retrofit.

Required manifest fields: format, source, annotations, assets, operations, constraints and instructions. Each image has relative path under source/, references/ or overlays/, MIME, SHA-256, bytes, width and height; annotation IDs and labels unique; operation points to existing target; asset IDs match real selected references. constraints.preserveUnmarked is true; constraints.engineeringVerified is false. The host resolves visual overlap and source orientation before handoff. The app requires explicit user review of exact photos, model identities, regions, instructions and privacy disclosure before invoking AYA. AYA chooser launch is not receipt of images by an external agent. Text-only delivery is not equivalent to full packet delivery.

Conservative initial authoring budgets following AYA: <=20 MiB per image, <=100 MiB total, <=24 attachments. Preserve originals internally; reject unsupported/broken images with a visible actionable error. Do not silently drop references to meet budgets. If a recipient doesn't take multi-images, offer honest manual document/archive fallback when implemented.

## First Android implementation choices

- Kotlin + Jetpack Compose; native platform Photo Picker for a single existing photo (camera capture is upcoming, not assumed implemented).
- Immediately copy picker-granted content bytes into app-private storage. No blanket media permission or unapproved upload. Decode an orientation-normalized private derivative and retain the unchanged original separately.
- One canonical draft state; a small atomic local JSON snapshot is acceptable **only for the first one-room slice**. Revisit a conventional Room database when multi-project, catalog and revision relationships are actually implemented; do not have two concurrent authorities.
- Viewport fit and letterbox math are a pure mapping function with JVM tests. Use Compose transformable input so image and markers share one zoom/pan transform.
- All marker actions have accessible 48dp targets and non-gesture alternate controls. Include narrow viewport and large system-text installed checks in qualification.
- No telemetry, AI secret, hosted image store, network requirement or image generation inside Retrofit.

## Validation plan

E0: verify contract and geometry reasoning; E1: executed deterministic JVM mapping cases. E2: installed photo picker -> private bytes -> marker and note edit -> relaunch -> same marker/image mapping, including cancel/no replacement and failure recovery on an Android emulator. E3: independent foreign receiving Android app verifies approved AYA attachment count, byte count and hashes. E4: owner field test on a physical Android device and real external assistant, with honest limits. E5: stable signing plus update preserving existing room data, only if shipping warrants it.

First positive oracle: from the actual installed activity choose an image, place A/B/C, attach notes, restart, see identical coordinates/contents and unchanged original bytes. First negative oracle: cancel the picker or select invalid image and retain the existing draft; a letterboxed margin tap must create no marker. Never claim an APK is usable merely because it compiles.

## Decisions intentionally deferred

- Source/licensing/quality standards for the hundreds of equipment views; model metadata and any product-detail scraping.
- Camera capture workflow and image-correction/masking UI, after the picker journey works.
- OCR/detection, depth/3D estimation, multi-photo spatial calibration, engineering integration and automatic scene changes.
- Preferred client presentation style, brand colors and export format; these can be owner-judged against a real installed screen.
- Real external AI recipient matrix: multi-file acceptance is device/app-version-dependent and must be tested; AYA cannot guarantee ingestion.

## Prior art and platform evidence

- [Fieldwire photo markup](https://help.fieldwire.com/hc/en-us/articles/360000217443-How-to-add-Markups-to-Photos-on-Mobile-and-Web): task-associated photo drawing, arrows, text and measurements. Transferable lesson: markups should belong to a named context and be editable, but Retrofit's core canvas is the photograph rather than a construction plan.
- [magicplan project/room/object photos](https://help.magicplan.app/add-information-and-photos-on-floor-level): photographs and notes attached at multiple levels of a project. Transferable lesson: retain the source-to-room association when exporting proposals. This does not establish that floor-plan authoring is required for Retrofit.
- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker): scoped URI grants; a selected URI is not a filesystem path or a permanent permission.
- [Compose multitouch](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch): transformable detects pan/zoom but does not itself transform the pixels. Both image and markers require the same transform.
- [AYA packet](https://github.com/gamesbyian/AYA/blob/main/docs/spatial/PACKET.md): coordinates, identity, attachment integrity, privacy, engineering disclaimers and manual return.

No external user studies or claims about majority preferences are implied by this targeted prior-art review. Stop researching this first journey unless an observed usability problem changes the choice.
