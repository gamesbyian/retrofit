# Retrofit equipment catalog, sourcing and rights plan

**Status:** candidate catalog specification and rights checklist for G5, **not** an assertion that any asset is licensed to bundle or send to AI. Date: 2026-11-06. Applicable journey [UX Interactions](UX-INTERACTIONS.md), [System Blueprint](SYSTEM-BLUEPRINT.md), [AYA v1](https://github.com/gamesbyian/AYA/blob/main/docs/spatial/PACKET.md).

## User need and catalog scope

Target an eventual searchable local reference collection on the order of 400 distinct equipment items with multiple **real** views when legally available, not 400 fabricated visual identities. First proof requires five genuinely usable candidates spanning boiler, expansion/storage tank, air handler/rooftop unit, heat pump or heat exchanger, and controls. This is a sample diversity target, not a product/vendor endorsement.

**What the catalog is for:** visual identification, reference angle selection, approximate envelope and vendor/model provenance for a *conceptual* proposal. It is not a substitute for an approved submittal, equipment engineering, safety clearance or manufacturer sizing tool.

## Sources inspected and constraints

| Potential source | Evidence / examples | Opportunity | Unresolved rights |
| --- | --- | --- | --- |
| Weil-McLain | [BIM/Revit library](https://www.weil-mclain.com/bim-revit-library/) lists boilers, heat pump systems, controls and indirect water heaters | Real model families, drawings, manufacturer identity | Need explicit rights for storing in distributed app and transferring to a third-party image generator |
| Trane | [BIM and selection tools](https://www.trane.com/commercial/north-america/canada/en/support/resources/bim-and-selection-tools.html) offers commercial HVAC technical files | Configurable equipment, CAD/BIM dimensions for source-backed envelope | Download availability does not prove multi-angle photo licensing |
| Carrier | [Revit Configuration Portal](https://www.carrier.com/us/en/commercial/software/revit-3d-templates/revit-configurator/) offers configured product files and technical settings | Variant-aware data model and potential asset geometry | Must verify output format, identity, rights and rendering suitability |
| Navien | [Professional boiler downloads](https://www.navieninc.com/professional/boilers/downloads) includes brochures/specs/drawings/Revit | Manufacturer-linked boiler reference and product variants | Brochure images and generated views may carry different reuse terms |
| MEPcontent | [Manufacturer API](https://api.mepcontent.com/) describes authorized embedding/linking with an API key and downloaded files coming from MEPcontent | Consider manufacturer-authorized *links* rather than copying all CAD | API key/permission required; do not treat link mode as a redistribution license |
| BIMobject/Bim.com | [Terms](https://business.bimobject.com/terms-of-service-eula) distinguish professional project use from incorporation into third-party services; content ownership remains with originators | Useful catalogue of equipment *sources and potential partners* | Do **not** bulk-copy and package model/data/images into Retrofit under generic downloads permission |
| TraceParts | [API terms](https://developers.traceparts.com/docs/api-terms-of-use) restrict copying/distribution absent express written consent | Candidate authorized vendor data provider with explicit API contract | Require separate permission and potential user identification |
| User or organization | Their own authorized photographs or written vendor permission | Best first pilot for a private bespoke catalog | User must confirm right to use externally; app does not magically grant license |

**No OEM/manufacturer media permissions have been verified in this research pass.** Source links are lead lists, not import instructions. Do not scrape sites, redistribute CAD files, produce fake unobserved equipment angles, remove legal notices, or copy a catalog wholesale without permission.

## Separate rights dimensions

At minimum record independently:
- **View/reference right:** human may look up vendor data or use the manufacturer's site.
- **Private app storage:** permission to download and keep bytes in the owner's local project.
- **App distribution:** permission to package/copy those bytes to *other* installations.
- **Project/customer output:** permission to include content/derived render in a conceptual client proposal.
- **Third-party AI transfer:** permission to send exact reference bytes to an independently selected AI provider, with privacy/data-retention effects.
- **Transformation:** permission to crop, annotate or derive/render alternate views.
- **Attribution/terms:** retained copyright, credit, notices, revision limits and commercial context.

A source may allow professional project drawing inclusion but prohibit building a competing product catalog or distributing original media. A user-imported file may be stored privately while external sharing remains an explicit consent/rights decision. **Unknown is never silently treated as permitted** for bundling or external transfer. Record which rights were asserted by user vs verified by license/written permission.

## Candidate manifest representation

This is a logical **schema example**, not a real product, license grant or final file format:

```json
{
  "assetId": "catalog:example-boiler:generic-a",
  "sourceClass": "user_import",
  "displayName": "Example floor-standing boiler",
  "category": "boiler",
  "manufacturer": null,
  "model": null,
  "variant": null,
  "status": "generic_unbranded",
  "dimensions": {
    "widthMm": null,
    "heightMm": null,
    "depthMm": null,
    "evidence": null
  },
  "referenceViews": [
    {
      "id": "ref:front",
      "view": "front",
      "sha256": null,
      "originalUri": null,
      "mime": "image/jpeg",
      "orientedWidth": null,
      "orientedHeight": null,
      "evidenceSource": "user_supplied",
      "licenseState": {
        "privateStorage": "user_asserted",
        "redistribution": "unknown",
        "externalAiTransfer": "unknown",
        "transformation": "unknown"
      }
    }
  ],
  "rightsEvidence": {
    "sourceUrl": null,
    "retrievedAt": null,
    "writtenPermissionLocation": null,
    "notices": [],
    "licenseRevision": null
  }
}
```

Do not allow a null media hash as a *selected shareable reference*; null above means a draft entry not yet populated. Production import validates actual MIME magic, decoding, source ID, orientation/dimensions, bytes/hash and provenance. Manufacturer/model must be canonical if supplied; text/OCR aliases are suggestions requiring user confirmation.

### Model identity and matching

- Separate category (boiler), vendor, model family, exact SKU/configuration, alternate names, legacy/discontinued identity, and a specific **photo instance**. A generic boiler must remain visibly generic.
- Product imagery must be real: front/side/rear/three-quarter/detail labels only where an actual supported source view exists. A vendor drawing, BIM rendering, brochure marketing photo and field photograph are different reference types. No invented rear view from a front photo represented as source truth.
- Product dimension has unit, source document ID/version and whether it represents nominal bounding envelope versus installed clearance. An image alone cannot validate a width in millimetres.
- Optionally evaluate [ML Kit on-device OCR](https://developers.google.com/ml-kit/vision/text-recognition/v2/android) for equipment nameplate suggestions. OCR errors/misreads cannot silently select equipment or assert a certified product match. The user may photograph a separate close-up nameplate, but this is a *different photo view*, not automatically registered to the room panorama.

## Import / versioning / search architecture

**Initial prototype:** user-imported authorized photos and a tiny manifest, stored app-private. Catalog search on category, manufacturer, exact model and aliases. No cloud account or network required for existing assets. Image bytes and declared rights attached to immutable revision, not a mutable URL pointing to content that might change.

**Expansion:** a permitted manufacturer manifest may be downloaded or imported in a user-reviewed operation; verify checksums, track source version, and keep existing approved proposal snapshots unchanged. Consider a provenance-aware SQLite/Room repository when multi-model and multi-project queries justify it. Do not retrofit two data authorities around the current one-room JSON proof.

**Search states:** no results; ambiguous name; missing image; cached thumbnail but missing source; incomplete rights; stale/updated model; multiple variants; user-imported generic. Sorting can be category/model/name first; semantic image similarity is optional and requires actual task proof. Avoid auto-using a model whose name resembles the user's instruction without visual confirmation.

**Offline size:** do not pre-bundle thousands of high-resolution images without a device storage/bandwidth budget, rights and measured usage. Small local thumbnails plus user-selected full references are a plausible reversible choice, but no source files have been acquired here. Do not silently downsample source for AYA without new user-reviewed hash/quality or claim that selected angles were sent if they were omitted.

## AYA/share boundary

- Add/replace operation requires user-selected real catalog asset(s); embed only the reference views the user approved.
- Freeze exact asset identity, manifest revision, media MIME/dimensions, SHA-256, byte length, provenance/rights review and operation target before AYA. If the user changes asset or source, invalidate older review and re-review.
- AYA v1 suggests <=20MiB/image, <=100MiB total, <=24 attachments; these are authoring budgets, **not a guaranteed recipient capability**. If too many views, prompt user to pick a smaller set, never silently omit.
- User privacy approval is separate from third-party content licensing. They may be willing to share a building photograph but the manufacturer may still restrict republishing a CAD model. Store an auditable source/right record.
- Keep a "generic illustrative equipment" fallback with clear warning; never fabricate exact branded reference photos or quietly switch vendors.
- Generated outcomes may resemble a product but cannot certify electrical/thermal capacities, connection positions or manufacturer compliance.

## First five-asset sourcing experiment (not yet executed)

For each sample category, locate **one** exact product with at least two permitted reference views or record absent, official dimensions and variation, and explicit permissions to store and transfer those images through chosen AI recipient. Track cost, process, legal status, view coverage and whether source is a CAD model, marketing photograph, true field capture or user-owned image. If manufacturer sources don't grant rights, trial direct written authorization or user-imported images; do not use this study to scrape or copy samples into public GitHub.

**Exit:** at least one real complete, rights-cleared add/replace proof with identical received image hashes before claiming catalog-backed AYA sharing. Until then the catalog schema remains a proposed reusable design rather than licensed inventory.
