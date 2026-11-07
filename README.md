# Retrofit

**Android-first, local-first** conceptual renovation visualization for mechanical rooms and building-services equipment.

The intended user journey is photograph -> mark objects/locations -> choose verified equipment references -> describe proposed changes -> explicitly review and share through [AYA](https://github.com/gamesbyian/AYA) to a chosen assistant -> manually import and compare a generated **Concept Visualization**. The result is *not* a validated engineering design.

## Current project state (2026-11-06)

- [Draft Retrofit photo/marker prototype PR #2](https://github.com/gamesbyian/retrofit/pull/2) provides an unmerged Android foundation with local photo import and marker editing. Build/test CI passed on the draft head; installed workflow is not yet qualified. Catalog, AYA external handoff and image generation are not implemented.
- [AYA spatial composition v1](https://github.com/gamesbyian/AYA/blob/main/docs/spatial/PACKET.md) is the proposed reviewed attachment contract. Its [Android adapter PR #151](https://github.com/gamesbyian/AYA/pull/151) is not assumed merged, installed or proven with real assistants.
- Owner image experiments suggest that independent architecture/equipment survey overlays can be useful, but strict prompt constraints alone did not prevent unrelated pixel/text changes.

## Research-first product development

The repository's next priority is **comprehensive project/market/technology discovery and a coherent specification before broad substantial coding**. Small bounded prototypes and tests may continue to resolve architectural questions.

- [Discovery research and prior art](docs/DISCOVERY-RESEARCH.md): comparable products, source evidence, unknowns, catalog rights.
- [Proposed system blueprint](docs/SYSTEM-BLUEPRINT.md): screens, journeys, spatial concepts, source/reference identity, AYA boundary and modules.
- [Concrete Android interaction specification](docs/UX-INTERACTIONS.md): screen layouts, explicit placement modes, error states and accessibility.\n- [Catalog source and rights specification](docs/CATALOG-SOURCING.md): manufacturer assets, reference angles, provenance and permission boundaries.\n- [Controlled experiments](docs/EXPERIMENTS-AND-QUALITY.md): wireframes, independent first attempts, segmentation, local compositing, image fidelity and real-device/provider acceptance.
- [Discovery readiness gates](docs/DISCOVERY-GATES.md): explicit research decisions and entry criteria before broad implementation.
- [Single execution issue #1](https://github.com/gamesbyian/retrofit/issues/1): current priorities/evidence and implementation tracking.
- [Agent instructions](AGENTS.md), [local skills architecture](skills/CAPABILITY-ARCHITECTURE.md), and [Android skill](skills/android-app-development/SKILL.md).

All discovery documents distinguish observed evidence from proposed or experimental features. No client-site photograph should be shared externally without a specific reviewed authorization.
