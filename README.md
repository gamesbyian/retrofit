# Retrofit

**Android-first, local-first** conceptual renovation visualization for mechanical rooms and building-services equipment.

The intended user journey is photograph -> mark objects/locations -> choose verified equipment references -> describe proposed changes -> explicitly review and share through [AYA](https://github.com/gamesbyian/AYA) to a chosen assistant -> manually import and compare a generated **Concept Visualization**. The result is *not* a validated engineering design.

## Current project state (2026-11-06)

- [Draft Retrofit photo/marker prototype PR #2](https://github.com/gamesbyian/retrofit/pull/2) remains **unmerged**. At head `d27ebd13`, the [debug/JVM run](https://github.com/gamesbyian/retrofit/actions/runs/42327983542) and [installed API 28/35 run](https://github.com/gamesbyian/retrofit/actions/runs/42327983645) passed. API 35 exercised real Photo Picker cancellation/reentry, exact private original bytes after public-source deletion, full restart, committed-edit survival and abrupt-kill recovery; API 28 exercised installed marker/Activity-recreation/invalid-source behavior. The prototype now displays **Saving… / Saved locally / Not saved**, with a retry for storage errors. Recent uncommitted characters can be lost if the process dies immediately (0/20 durably observed in the crash test). A physical phone, accessibility, multi-room behavior, catalog, AYA handoff, result import and external image generation remain unverified or absent.
- [AYA spatial composition v1](https://github.com/gamesbyian/AYA/blob/main/docs/spatial/PACKET.md) is the proposed reviewed attachment contract. Its [Android adapter PR #151](https://github.com/gamesbyian/AYA/pull/151) is not assumed merged, installed or proven with real assistants.
- Owner image experiments suggest that independent architecture/equipment survey overlays can be useful, but strict prompt constraints alone did not prevent unrelated pixel/text changes.

## Research-first product development

The repository's next priority is **comprehensive project/market/technology discovery and a coherent specification before broad substantial coding**. Small bounded prototypes and tests may continue to resolve architectural questions.

- [Discovery research and prior art](docs/DISCOVERY-RESEARCH.md): comparable products, source evidence, unknowns, catalog rights.
- [G2 field-photo interaction comparison](docs/G2-FIELD-PHOTO-WORKFLOWS.md): contractor photo-first, task-first, room-first, product-design and asset-first patterns, with current limits and tests.
- [Proposed system blueprint](docs/SYSTEM-BLUEPRINT.md): screens, journeys, spatial concepts, source/reference identity, AYA boundary and modules.
- [Concrete Android interaction specification](docs/UX-INTERACTIONS.md): screen layouts, explicit placement modes, error states and accessibility.
- [Catalog source and rights specification](docs/CATALOG-SOURCING.md): manufacturer assets, reference angles, provenance and permission boundaries.
- [Five-model manufacturer-source scout](docs/CATALOG-FIVE-MODEL-SCOUT.md): specific equipment sources, model-generation/dimension conflicts, view and license blockers, reusable sourcing alternatives.
- [One-job field-sales observation protocol](docs/FIELD-WORKFLOW-PROTOCOL.md): brief privacy-respecting process to replace user-workflow assumptions with evidence.
- [G6 segmentation resource audit](docs/G6-SEGMENTATION-RESOURCE-AUDIT.md): compare native manual regions, OpenCV GrabCut and current/legacy MediaPipe APIs before any Android model dependency.
- [Private-image registration and seam diagnostics](docs/G6-REGISTRATION-DIAGNOSTIC.md): source hashes and bounded metrics only; exact pixel preservation does not establish image alignment or seam quality.
- [Executed synthetic pixel-locality experiment](docs/PIXEL-LOCALITY-SPIKE.md): repeatable local exact-pixel preservation outside a reviewed mask, with falsifiers and explicit registration/AI limits.
- [Synthetic AYA v1 packet contract experiment](docs/AYA-PACKET-E1-PROOF.md): five fictional media attachments and independently checked hash/MIME/dimensions; canonical AYA structural test, distinct from actual delivery.
- [Controlled experiments](docs/EXPERIMENTS-AND-QUALITY.md): wireframes, independent first attempts, segmentation, local compositing, image fidelity and real-device/provider acceptance.
- [Discovery readiness gates](docs/DISCOVERY-GATES.md): explicit research decisions and entry criteria before broad implementation.
- [Single execution issue #1](https://github.com/gamesbyian/retrofit/issues/1): current priorities/evidence and implementation tracking.
- [Agent instructions](AGENTS.md), [local skills architecture](skills/CAPABILITY-ARCHITECTURE.md), [Project Inception](skills/project-inception/SKILL.md), [Evidence Acquisition](skills/evidence-acquisition/SKILL.md), [Engineering Resource Discovery](skills/engineering-resource-discovery/SKILL.md), [Product Specification Engineering](skills/product-specification-engineering/SKILL.md), and [Android skill](skills/android-app-development/SKILL.md).
- [Portable skills installation and verification](skills/PORTABILITY.md): 167 locally copied source files with relative links and supporting examples/scripts, independently versioned from the canonical library.

All discovery documents distinguish observed evidence from proposed or experimental features. No client-site photograph should be shared externally without a specific reviewed authorization.
