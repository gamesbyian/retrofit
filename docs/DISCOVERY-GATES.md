# Retrofit discovery readiness and implementation-entry gates

**Status:** authoritative *discovery readiness reference*, not a parallel execution queue. Update the single [GitHub issue #1](https://github.com/gamesbyian/retrofit/issues/1) when decisions/proof change. Date: 2026-11-06. Read with [Discovery Research](DISCOVERY-RESEARCH.md), [System Blueprint](SYSTEM-BLUEPRINT.md), [UX Interactions](UX-INTERACTIONS.md), [Catalog Sourcing](CATALOG-SOURCING.md), and [Experiments and Quality](EXPERIMENTS-AND-QUALITY.md). The prototype's product contract is in [draft PR #2](https://github.com/gamesbyian/retrofit/pull/2); avoid treating proposed design as already merged/shipped.

## Current evidence snapshot

- `main` contains research-first specs, specialized skills and project instructions. Android code remains in draft [PR #2](https://github.com/gamesbyian/retrofit/pull/2) at `d27ebd1361cb358ac6276814bdecb85f62c30fe9`. [Debug/JVM run 42327983542](https://github.com/gamesbyian/retrofit/actions/runs/42327983542) passed. [Installed run 42327983645](https://github.com/gamesbyian/retrofit/actions/runs/42327983645) passed on **both API 28 and API 35**. API 35 covered native Photo Picker cancel/reopen/select, exact private source bytes after public deletion, full process restart, a fully saved edit surviving force-stop and a rapid 20-character edit killed immediately; **0/20** were durable but the prior complete draft recovered without corruption. API 28 passed one installed Compose marker/storage/Activity-recreation/failed-import test. The UI displays Saving… versus Saved locally and can retry a storage failure. This remains emulator-only proof, not owner-device or AYA evidence.
- PR #2 contains Kotlin/Compose photo picker, app-private original copy and oriented JPEG derivative, A/B/C markers, annotations/instructions and atomic one-room persistence. Equipment catalog, AYA packet, AI surveys, masks, before/after are **absent**.
- AYA `aya-spatial-composition/v1` [contract](https://github.com/gamesbyian/AYA/blob/main/docs/spatial/PACKET.md) exists; [Android sharing PR #151](https://github.com/gamesbyian/AYA/pull/151) is open and is not proof of foreign-recipient bytes. This dependency is an external gate for handoff implementation, not a reason to halt local UX/contract work.
- Owner demonstration provides conceptual images and independent AI generations with preservation failures. No blinded or controlled causal test has been executed here.
- Prior-art sources have been read but no competitor installed comparison, salesperson field observation, fully rights-cleared catalog or actual mobile segmentation benchmark has yet been performed.

## Mandatory before broad substantial implementation

Here, **broad substantial implementation** means starting parallel feature epics, committing to persistent multi-project schema/AI backends/catalog ingestion, or scaling out the architecture. Small reversible, contained prototype fixes, test infrastructure repairs and bounded feasibility spikes may proceed before all gates when their purpose is to acquire evidence.

| Gate | Needed deliverable and discriminating decision | Current state | Owner input? |
| --- | --- | --- | --- |
| G1 real job/UX | Observe or reconstruct the salesperson's real capture -> select -> propose -> review -> present flow; list normal and recovery paths, source permissions and acceptable delay. Determine photo-first vs measured/3D-first. | Owner goal recorded; observational evidence missing | Yes, brief real-world walkthrough or a permitted sample job |
| G2 competitor/reuse | Compare actual workflows for photo markup, 2D/3D scene selection, catalog and client presentation. Inventory mature platform primitives with licensing, costs and unworkable assumptions. | First-party desk research seeded; runtime comparisons incomplete | No, until choices change |
| G3 product scope | One approved feature/interaction specification with prioritized MVP, later features and exclusions; corrected objects, provenance, visible uncertainty and multi-photo boundaries. | Proposed system blueprint; confirm against G1, reconcile PR #2 product contract | Only for genuine product/authority choices |
| G4 data/AYA | Canonical IDs, immutable bytes, oriented coordinate transforms, project/revision relation and v1 packet integration mapping; explicit treatment of `move` and scene-return extensions. | Initial model proposed; AYA adapter PR unmerged | No for technical contracts; permission for real sharing |
| G5 catalog rights/proof | Verify exact source/variant and two permitted images per item; establish rights for local store, app redistribution, transformations, client output and selected AI recipient. | [Five-model source scout](CATALOG-FIVE-MODEL-SCOUT.md) found authoritative technical identities and **three material source-era/version hazards**. **0 of 5** have qualified multi-angle licensed media or AI-transfer grants; external sharing/bundling remains blocked. The user-provided authorized-reference pack is the candidate bounded pilot. | Yes for actual content owner permissions/purchases; field model choices |
| G6 decisive feasibility | Fixed photo corpus and a minimal manual-region prototype; compare visual vs structured overlays, local interactive segmentation, recipient multi-attachment ingestion, and prompt-only preservation vs constrained/local compositing. State reversible choices from results. | Owner visual demo; formal experiments not run | Only for consenting to third-party photo upload |
| G7 architecture/validation acceptance | Reconcile G1–G6 into data schema, state machines, UI screens, module/adapter ownership, fixed evidence gates and first release slices. Record explicit non-goals; PR #2 remains a prototype until reviewed. | Blueprint proposed, not validated | Only unresolved product behavior |
| G8 prototype proof | Exact-head installed normal/negative app journey and accessibility review, independent AYA receiver when integrated. Decide keep/refactor/replace prototype based on evidence. | **Partial E2, now API 28 and 35:** API 35 native Photo Picker cancel/reentry and exact source retention plus committed-edit and immediate-kill recovery; API 28 installed marker/edit/Activity-recreation/invalid-source path. No corrupt snapshot or photo observed. Rapid uncommitted edit **not guaranteed durable (0/20 observed)**; visible save status clarifies it. Physical device, TalkBack/large-font, camera/orientation edge cases, foreign AYA receiver and field acceptance still unproven. | Device/field sign-off eventually |

**Interpretation:** G1–G7 are preconditioned *understanding and choice* gates before scaling implementation. G8 proof should be obtained early for the existing bounded prototype and used to refine G3–G7; not every far-future image provider must be exhaustively verified before a working local annotation app is developed. Unknowns with no design impact are explicitly deferred.

## Immediate research / design actions, in order

1. **Verify and clean the existing draft contract:** compare each normative sentence in PR #2 against owner decisions; identify accidental claims about AYA delivery, exact camera geometry, model identity and inferred measurements. Resolve mismatches in a single reconciled product contract when merging docs; no forked normative specs.
2. **Conduct one field walkthrough:** gather a sample authorized job from photo capture to client presentation; define top five costly interactions and failed cases. Record whether private information may be transmitted and how specific replacement model selection occurs.
3. **Produce task-based competitive audit:** choose two photo-markup products and one capture/BIM comparator; annotate steps, default states, recovery and whether each feature actually exists in tested product/version, including nonuser complaints. First-party docs already list candidates.
4. **Build catalog rights sample:** for at least five representative devices (boiler, air handler, tank, controls, rooftop unit), locate legitimate reference views, official technical dimensions and written permissions/fallback, without scraping or bundling restricted content.
5. **Lock a small fixture/evaluation corpus:** retain owner original privately with explicit permission, create public synthetic assets, record hashes/geometry/known facts and run the [controlled experiments](EXPERIMENTS-AND-QUALITY.md).
6. **Qualify segmentation and local compositing:** try manual polygon first, Google Interactive Segmenter on actual Android model/device, then only if it changes the decision compare off-device SAM/depth. Verify true unchanged-pixel result with an independent pixel oracle.
7. **AYA decision spike:** verify whether PR #151 is merged and what it delivers; run an independent receiver on real Android, check per-file hashes, test representative recipients. Agree on v1 mapping or versioned extensions for typed scene/move/mask output.
8. **Reconcile architecture & UX:** one canonical project/data/operation/revision model, lifecycle/error behavior, screen flow/wireframes, rights model, test plan, MVP/later cutoff; review with owner only for consequential choices. Then split into small reviewable implementation slices.

Each action must close with evidence, what it changed and which blocker remains; do not expand the checklist because another tool/library seems interesting.

## Current conditional design defaults (proposed, reversible)

- Photo-first, offline-capable editor, optional AI survey; no mandatory ARCore/3D capture.
- Local source and changed-view layers; deterministic blur/opacity; explicit human corrections; immutable originals.
- Manual region markup is guaranteed fallback. Add interactive ML segmentation only after quality and latency proof on actual mechanical-room imagery.
- User-supplied or individually licensed catalog references first; no mass harvesting of asset websites. Keep rights provenance attached to every image.
- AYA v1 for explicitly reviewed share, no parallel networking/assistant infrastructure. Structured scene return or masks require separately versioned agreement and recipient proof.
- A generated result can be a persuasive concept while wrong on engineering; always label it and compare with original. Protect unmodified pixels with local compositing only when mask/registration makes it testable.

## Explicit decision forks worth discussing after evidence

- **Single image vs multi-view capture:** if accurate spatial anchoring fails on common obstructions, decide whether optional second photo is enough or calibration needed.
- **Image catalog vs BIM catalog:** if product photos suffice for visual replacement, do not require CAD conversion; if spec-compliant placement is required, define a separate engineering product boundary.
- **Manual vs AI survey:** if AI masks reduce correction time and preserve spatial features at acceptable privacy/cost, adopt as optional; if not, prefer manual geometry.
- **Generic vs exact model:** if vendor reference rights cannot be obtained, use explicit user-supplied image/generic unbranded substitutes, without claiming exact marketed equipment.
- **External sharing semantics:** if selected AI apps cannot receive multi-image via AYA, disclose compatible alternatives or build a versioned reviewed fallback with AYA; no fake success.

## Readiness report template

`Gate | direct evidence + exact source/version | completed / blocked / conditional | decision | rejected alternative | cheap disconfirming test | owner authority? | next observable action`.

**Green-light broad development** when G1–G7 have named accepted or explicitly deferred decisions, no unbounded licensing/privacy/protocol blocker threatens the chosen MVP, the canonical model and UX coverage are internally consistent, and G8 provides at least a real installed proof for the foundational path. This avoids both premature coding and an indefinite research embargo.
