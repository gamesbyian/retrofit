# Retrofit interaction specification (proposed, Android-first)

**Status:** design candidate for G3, not owner-approved UI nor implemented. Date: 2026-11-06. Read [System Blueprint](SYSTEM-BLUEPRINT.md), [Discovery](DISCOVERY-RESEARCH.md), [Catalog Rights](CATALOG-SOURCING.md), [Gates](DISCOVERY-GATES.md). Field observations may change this flow. Current draft PR #2 implements a much smaller one-room photo/marker foundation.

## Interaction principles

- The **room photograph occupies the main surface**. Primary action is "specify what changes at this exact place"; AI scene survey, model inference and BIM controls are progressively disclosed rather than compulsory.
- All visual adjustments (fade, blur, zoom, wireframe) are local **view state**, never destructive changes to the source or normalized coordinates. Turn layers on/off individually, including inferred architecture, equipment and user labels. Save material work automatically with visible failure/retry, not a cosmetic Save button.
- Mode ambiguity must be removed: distinguish **inspect/select**, **place marker**, **draw region/mask** and **pan/zoom**. A normal tap should not unexpectedly create another letter.
- Allow all task-critical operations through readable controls, not just tap-and-hold or swipe gestures. Targets at least 48dp; accessible roles and element labels; talkback announces object, location, intent and verification state without color dependence.
- Source image remains visually authoritative. "AI suggested" means editable, not silently verified. Generated output carries clear **Concept Visualization** status.
- On a narrow phone, keep the photograph visible and use a short bottom sheet for the currently selected marker or object. Don't require permanent 3D panels or nested project navigation for the initial journey.

## Navigation and information hierarchy

**Initial version:** opening screen can be "Current room" with New/Import and prior draft resume, avoiding unnecessary navigation. **Multi-project version:** Projects -> Room Workspace -> (Catalog, Review, Proposals as contextual routes). Consistent Back goes to prior screen without losing drafts; top-level switching preserves source and selected target. Don't design separate systems for single room versus multi-room; progressive extension from the same IDs.

| Screen / sheet | Required default visible content | Primary action | Secondary route |
| --- | --- | --- | --- |
| Projects (later) | room/client title, source thumbnail, last edited, saved/unsaved and proposal count | Resume or new room | archive/export, duplicate |
| Capture/import | current source or choose image, privacy/local-copy text, import progress/error | Choose photo (later: camera) | cancel/restore previous draft |
| Workspace | original photo, marker overlays, current tool indicator, selected target, unobstructed controls | Mark/select target, configure change | layer viewer, catalog, review |
| Change detail bottom sheet | target label/thumbnail, selected operation, free text, model association and verification | Specify operation / pick item | move marker, outline region, protected target |
| Catalog search | category/search, clear manufacturer/model identity, actual image views & provenance | Select asset/view(s) | import owned references, compare variants |
| Layer inspector | photo opacity/blur, show architecture/equipment/labels, inferred/confirmed styling | Toggle or correct | import reference overlay, draw polygons |
| Handoff review | immutable source, chosen reference images, exact operation list, byte counts, recipient/privacy notice | Approve and invoke AYA | cancel / edit / select fewer |
| Proposal review | source vs returned image, origin request, alignment status, conceptual label | Accept as revision | reject, inspect change drift, export |

### Workspace compact-phone arrangement

1. **Top bar**: project/room short title, current draft status, overflow for room data. Do not consume much height.
2. **Photo canvas**: letterboxing is visible and non-interactive; whole imported photo shown on fit; pinch zoom pan; selected marker/object has visible outline and accessible description. A/B/C labels must remain distinguishable at zoom (keep touch target larger than visual marker).
3. **Compact mode row**: Select | Mark | Region; layer toggle and reset-fit actions remain reachable. Explicit mode state prevents accidental placement during pan.
4. **Context bottom sheet**: collapsed shows selected target and one-sentence operation; expanded exposes action, notes, catalog source, protect/keep and permitted edit region. When no target selected it offers "Tap a marker or select Mark to place one."
5. **Footer actions**: Catalog/References contextually, and Review/Share only once a valid intent exists; otherwise show missing prerequisites plainly. Unavailable AYA is a visible honest disabled/guide state.
6. **Keyboard and gesture conflict**: the sheet scrolls above IME, photo retains selection. Rotating does not lose draft, selection or screen context. A large font may shift the editor into a vertical sheet, not make controls inaccessible.

### Marker/target behavior

- **Mark mode:** a valid tap inside the full oriented image creates the next label, stable ID and normalized coordinates. Tap outside image no-op. Cancel Mark returns to Select.
- **Select mode:** tap labeled marker or object; show identification and editable details. If an AI-detected shape is ambiguous, offer manual correction or "unidentified equipment" without inventing nameplates. A click on empty photo selects nothing, not a new marker.
- **Move location:** dedicated Move action then tap target point or drag with handles. Preview moved marker, maintain id/label and correct normalized coords; cancel restores earlier anchor.
- **Region:** draw coarse rectangle/polygon first; later guided mask refinement. Treat target region and allow-change region as distinct concepts. A mask may extend to pipes/shadows only after review.
- **Removal:** remove the *annotation* versus propose removing the *physical equipment* must never be confused. Label buttons clearly ("Delete marker", "Remove equipment in proposal"). Marker deletion requires only an ordinary reversible interaction; physical removal remains a proposed operation in the frozen snapshot.
- **Multiple targets:** if two operation regions overlap or a removal touches a protected asset, show a conflict to resolve; no silent overwrite. Provide stable object IDs separately from display letters.
- **Keep:** protect a specified item/region from proposed changes, including labels and equipment. AYA v1 uses protected annotations, not a fictitious `keep` mutation.
- **Move equipment:** store internal move intent if user wants it; before AYA v1 export explain unsupported structured move, and require explicitly reviewed natural-language change or future protocol. Never silently substitute another operation.

### Layer and AI survey behavior

**Base display presets:** Original (photo full fidelity), Survey (photo substantially faded/mildly blurred with black line overlays), Placement (photo legible with placement markers), Compare (only accepted revisions). Presets are initial values, not separate source images.

Each layer retains a stable identifier, source/provenance and independent visible/opacity status. If an AI agent returns only a flattened diagram, display it as a **non-authoritative reference overlay**; do not present guessed lines as editable room geometry unless a separately validated structured representation exists. Label every suggested room plane/object as inferred or observed and provide tap-to-correct UI. Never show a fabricated percent confidence.

For an AI Scene Survey request, show exact inputs/recipient/privacy review, then require manual import of resulting assets if AYA cannot return structured results. A request failure leaves all original work available.

### Before/after behavior

Use synchronized zoom and a draggable divider **only when the source and proposal are aligned on a documented image canvas**; otherwise offer side-by-side unlinked views, fit individually and warn "Perspective/alignment differs." Show a "show original" press-and-hold alternative as a shortcut, not the only comparison method. Provide a highlighted region for intended changes and independent review of untouched text/pipes, with a visible rejection path. Display source proposal revision ID and date, not invented engineering metrics.

The faded/outline alignment style in [CompanyCam](https://help.companycam.com/en/articles/6828372-creating-a-before-after-photo) is relevant precedent but represents aligning two photos, whereas Retrofit also needs evaluating synthetic replacement edits. Keep them separate.

## Operational state contracts

| Event | State / user feedback | Recovery acceptance |
| --- | --- | --- |
| Picker canceled | Existing draft and title unchanged | Back to previous screen; no empty duplicate |
| Invalid/oversize photo | Show reason, preserve old draft; imported temp data cleaned | Choose another; original hash remains |
| Permission revoked after selection | App-private copied source remains, otherwise report unavailable | User explicitly retries picker |
| Draft autosave fails | "Not saved" state remains visible with retry; do not claim persisted | Exit/relaunch reflects last committed state honestly |
| Photo tap outside image | No new target | Existing selection unchanged |
| App rotates/IME opens | Anchor coordinates tied to oriented image, selected object not lost | No accidental placement |
| Inference suggests wrong label | "Suggested" status and correction action | Owner correction survives relaunch |
| Catalog asset missing/rights unknown | Missing / unlicensed status, block affected export | Import permitted reference or remove selection |
| AYA user declines review | No staged external effects after decline | Resume editable draft |
| Chooser launches but recipient fails | "Share attempted; recipient unverified", no replay | Review and explicitly retry |
| Result aspect/camera differs | No false synchronized overlay; alignment warning | Inspect separately or reject |
| Generated result rejected | Original and existing proposals unaffected | Return to editor/retry explicitly |

## Accessibility and acceptance heuristics

- Evaluate at approximately 320dp width, large system font up to 200%, dark/light contrast, portrait and landscape, reduced motion and screen reader; include field-device physical reachability.
- Every marker has a spoken label ("Marker B, boiler replacement, unconfirmed") and a non-color-only visible status. Layer toggles and sliders have explicit values and accessibility actions.
- Confirm a salesperson can complete one ordinary room edit with less context switching than manually bouncing between photo gallery and chat. Measure correction time and friction, not a fabricated tap budget.
- For first prototype, accept text-only instructions with markers, but handoff not enabled until source/reference bytes, catalog rights and AYA acceptance are real. Do not substitute screenshot/UI preview as shipping proof.

## UX decisions still requiring discriminating evidence

- Does the salesperson prefer tapping freely to create markers (current PR #2) or an explicit Mark mode that avoids accidental points? Compare real handling on a phone, not just theoretical ergonomics.
- Is a room/project list necessary for typical field work or does one active draft cover the usual visit?
- Are real equipment model references generally chosen during a sales call or later when drafting the proposal?
- Should "Move" be commonly needed, and does it demand a structured AYA v2 operation?
- Is manual mask drawing faster than AI segmentation correction on representative machinery?
- Which privacy prompts are necessary when clients or buildings forbid images leaving the site?

Choose defaults provisionally, test with an actual field walkthrough and record owner authority before treating them as binding.
