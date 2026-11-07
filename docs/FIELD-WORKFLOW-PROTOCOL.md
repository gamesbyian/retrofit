# One-job mechanical-service sales workflow observation

**Status:** an executable G1 observation script, **not** an interview conducted, owner answer or UI approval. **Date:** 2026-11-06. Use [Project Inception](../skills/project-inception/SKILL.md), [Evidence Acquisition](../skills/evidence-acquisition/SKILL.md) and [Human-Agent Interaction Engineering](../skills/human-agent-interaction-engineering/SKILL.md) only insofar as this single field discriminator requires them. Record conclusions in [issue #1](https://github.com/gamesbyian/retrofit/issues/1) and reconcile [UX Interactions](UX-INTERACTIONS.md). Do not create a second active issue or a questionnaire campaign.

## G1 question and competing answers

**Q-FIELD-01:** For a representative commercial maintenance-sales proposal, where does a *faithful, conceptual before/after mechanical-room image with identified equipment and user-directed AI editing* add value, and what information is actually collected to prepare that proposal?

- **H-A (visual aid is central):** The salesperson selects exact replacements, identifies their physical locations and needs to show the client a credible renovated view. This supports direct photo -> position -> actual reference views -> reviewed AYA -> presentation.
- **H-B (visual aid is occasional):** Most service-contract proposals principally need existing equipment inventories, observed conditions, maintenance objectives and recommendations. A generated renovated view is useful only for a subset of sales; avoid making image generation, BIM capture or a 400-model catalog the mandatory base workflow.
- **H-C (technical feasibility is central):** Visual concept cannot be presented responsibly without professional engineering measurements/clearances and product configuration verification. Retrofit must disclose its limits more prominently or restrict the first release to illustration/previsualization.

The owner explicitly wants Retrofit to support **the conceptual visualization use case**; the question is how to implement that responsibly, not whether to abandon the owner's goal. An observation of one job cannot estimate frequency across all customers.

### Background, not testimony from this salesperson

- [ACCA commercial service sales guidance](https://hvac-blog.acca.org/commercial-service-sales-presenting-recommendations/) describes equipment lists/photos, maintenance objectives, recommendations, benefits and contract documents as parts of proposal presentation.
- [ACHR News service-agreement survey guidance](https://www.achrnews.com/articles/92321-selling-commercial-service-agreements-part-1) describes cameras, inspection, recorded equipment make/model and field forms. It is older trade guidance, **not** an observed present-day user behavior.
- [Bureau Veritas equipment-inventory service](https://www.bvna.com/needs/equipment-inventory-tagging) identifies make/model/serial/condition/photos as common asset-survey information, but inventory is a distinct job from a conceptual proposal.
- [Roth IAMS comparison](https://rothiams.com/the-difference-between-a-facility-condition-assessment-and-equipment-inventory-datasets-2/) distinguishes condition assessments from exhaustive equipment tagging; this is an important *counterexample* to assuming that all rooms must be fully inventoried before image work.

These source classes support a **discriminating observation**, not a claim that the intended user always follows one trade-paper workflow.

## Entry requirements and privacy

1. Use one real **recent, representative** sales job in recollection or a permitted already-completed synthetic/demo job. No client name, address, building access codes, room label/nameplate or unpublished bid terms are required. The salesperson chooses whether they can disclose any images; if not, observe verbally or with dummy schematics.
2. Obtain permission **before retaining** a company/client field image; do not put genuine site photos into a public repository or a third-party AI prompt on the assumption they are available. Use a coarse content summary instead if restricted.
3. Keep the observation brief, ordinarily **15–25 minutes**, in the person's own working sequence. Only ask a follow-up when the answer would change a concrete screen, identity, consent, catalog or acceptance choice. Do not demand a fabricated image output or CAD measurements.
4. Treat demonstrations as reported/observed for that **one** job only. Where details are unknown, record UNKNOWN rather than prompting for a guess.

## Five-step observation, in job order

| Step | Show/describe without inventing | What the observer records | Architecture decision affected |
| --- | --- | --- | --- |
| 1. **Trigger and deliverable** | What prompted this proposal? What did the client actually receive and discuss? Was it a routine maintenance agreement, a specific equipment replacement or both? | Delivered artifact type, source of requests, approval chain, whether a renovation view would have been useful, who sees it | Conceptual image required/optional; presentation format versus full quote builder |
| 2. **Survey and photographs** | Recreate the actual visit in order. What photo(s) were taken (room overview, nameplate, connectors, faults, wall/clearances)? What could not be photographed? | Observable photo count/type, camera, room/linking practice, poor connection, hazards, privacy and file restrictions | Single vs multi-view room; picker/camera; scene geometry and nameplate as separate view types |
| 3. **Equipment identity and sourcing** | Where did a candidate replacement model come from: own judgment, manager, engineer, supplier, customer specification or existing inventory? What files are normally available? | Exact source/authority of SKU, verified vs inferred model, supplier PDF/image access, dimensions, whether vendor usage rights can be checked | User-supplied reference pack versus managed OEM catalog, OCR suggest vs actual model selection |
| 4. **Visual changes and trust** | Pick one actual intended change: replace/remove/add/clean, identify its place in a photo, note anything that must stay exactly the same. Ask how errors would be noticed. | What needs preservation (labels, pipes, spacing, controls), minimum detail, when a conceptual disclaimer is needed, whether photorealism helps | Markers versus polygons/masks, local compositing, editable wireframes, change review |
| 5. **Prepare, review and deliver** | What happened between site visit and client presentation? Who revises/approves? How are images referenced, shared or rejected? | Latency tolerance, connectivity, device context, iteration count, recipients/attachment compatibility, client presentation behavior | Local-first save/revisions, AYA reviewed handoff, before/after, output provenance and export |

### Observe one interruption, where possible

Ask the person to describe the most common interruption (call, switching apps, low signal, photo permission, changing replacement model, confidentiality objection, client asks for different concept). Trace **what must remain preserved** and how the work resumes. If no meaningful interruption emerges in this one case, record NOT OBSERVED; don't fabricate an error case to satisfy a matrix.

## Minimal capture record

Use prose or an existing issue comment, not a separate user database:

```text
Observation id/date and consent scope:
Context/job type and artifact delivered:
Entry/source and completed steps (observed / participant reported / researcher inferred):
Original photo types, count, legal sharing constraints:
Exact example operation and protected parts:
Equipment manufacturer/model decision authority and usable references:
Visual fidelity/concept vs engineering requirement:
Client recipient and export format:
Normal continuation and one interruption:
Surprises or contradicting evidence:
Decisions changed? (PHOTO / CATALOG / AYA / EDIT / PRESENT):
Remaining unknowns, source restrictions, next cheapest verification:
```

**Avoid retained identifiers:** do not put nameplates, photos, serial numbers, property identifiers, contractual prices or other sensitive details in a public research issue. If the owner explicitly authorizes a private test corpus, store it outside the public repo and record only permission status/hashes in project planning.

## Decision thresholds, negative control and closure

- If the person shows even one realistic exact replacement task and says a before/after concept would help, note **supported for that observed task**, not "core requirement for all maintenance proposals".
- If no real OEM photos are usable or supplier identity is decided *after* surveying, an early mandatory 400-item catalog search would obstruct this job. Defer to user-supplied references or "reference pending" while maintaining source precision.
- If two room angles are used only to resolve obstructions, make the second view **optional** and explicitly unregistered unless geometry calibration is proven. Don't equate an extra photo with a surveyed 3D coordinate frame.
- If precision/clearances need engineering verification, preserve the **concept-only** contract and mark that decision for a different authority; don't fabricate approval from convincing imagery.
- If sales client has a no-upload policy, no agent handoff occurs. A local mark-and-review product may still be useful; never promote an AI provider workaround that ignores privacy.

**Independent check:** take one observed user action and trace it from evidence to entry control, media/model ID, persistence/consent boundary and independent acceptance test; reverse-trace one proposed layer or catalog capability to an observed goal. The observer's assumption must not become a participant quote.

**Stop rule:** After one authorized walkthrough, record one outcome-changing fact, one false assumption if encountered, and up to three still-unresolved decisions. Do not create a lengthy recurrent interview unless the first job reveals a consequential user cohort or contradiction. Readiness remains **G1 PARTIAL** until directly observed, **not SUPPORTED** merely because this protocol exists.
