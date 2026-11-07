# G4 synthetic AYA v1 packet and independent byte-inventory proof

**Status:** bounded offline **E1** interoperability spike. Not a Retrofit shipping UI, not an AYA Android adapter invocation, not a real recipient test, no external AI call and no user-approved site media. Source of truth is [AYA `aya-spatial-composition/v1`](https://github.com/gamesbyian/AYA/blob/cdb43b45f14541603947dc13e00666e5602fb298/docs/spatial/PACKET.md), including its original [offline validator](https://github.com/gamesbyian/AYA/blob/cdb43b45f14541603947dc13e00666e5602fb298/tools/validate_spatial_composition.py). Do not fork the AYA manifest format.

## Question / alternatives / falsifier

**D-G4-PACKET:** Can Retrofit represent its owner-provided A/B/C mechanical-room job as an AYA v1 manifest with **real corresponding source, marker overlay and selected reference bytes**, without depending on any OEM rights, cloud account or sending operation?

- A: host generates deterministic source/reference/overlay files, geometries and operation-to-asset associations, and validates original media hashes/size/MIME/dimensions with AYA schema. Small, reversible authoring adapter is viable.
- B: owner operations require an extra unsupported field or a v2 geometry, or selected attachments cannot be retained/validated. **Do not add a second parser or silently omit content; report version gap upstream.**

**Falsifiers:** upstream AYA validator rejects the manifest; missing, altered, symlinked, duplicate or escape-path attachment is accepted as genuine; an operation selects a nonexistent asset; protected annotation is also modified; input count/hash readback differs from outgoing inventory. These are distinct from recipient support or granted permissions.

## Reproducible synthetic experiment

- [`experiments/aya_packet/generate_packet.py`](../experiments/aya_packet/generate_packet.py) draws a **fictional** 320 × 240 source mechanical-room sketch, transparent A/B/C overlay, two fictional boiler angles and one fictional heat-exchanger angle. It computes each real PNG's SHA-256, length, MIME and dimensions and records exactly five attachments in a v1 `manifest.json`. Operations: `A clean`, `B replace FictionBoiler`, `C add FictionHX`. `engineeringVerified=false`.
- [Six independent host byte-integrity tests](../tests/test_aya_packet.py) check real decoded bytes, corruption with unchanged JSON, missing reference, forged dimensions, duplicate/traversal path and symlink rejection.
- [CI workflow](../.github/workflows/aya-packet-proof.yml) checks out the **canonical AYA validator at immutable commit `cdb43b45f`**, runs its actual structural validation against the generated packet and expects it to reject a contradictory `B` protected+replace operation. The AYA schema/validator is *not* duplicated locally.
- Uses [the existing pinned Pillow dependency](../experiments/pixel_locality/requirements.txt) only to generate and decode fictitious image media. Does not require the Android AYA adapter, a proprietary OEM dataset or Android running hardware.

Local repeat:

```sh
python -m pip install -r experiments/pixel_locality/requirements.txt
python -m unittest discover -s tests -p test_aya_packet.py -v
python experiments/aya_packet/generate_packet.py --out-dir build/aya-packet
# When the pinned AYA source is checked out at ./aya-protocol:
python aya-protocol/tools/validate_spatial_composition.py build/aya-packet/manifest.json
```

Local tests prior to PR: **6 tests passed**, generated **5** synthetic PNG attachments totalling **4,971 bytes**, and the local host byte-integrity verifier reported no errors. **Canonical AYA structural validation passed independently in [GitHub run 42338039789](https://github.com/gamesbyian/retrofit/actions/runs/42338039789) at PR #12 commit `f0945c50e058631e79c13b3ab6638f8fa0f287a9`**. Six host tests passed, real five-file packet passed upstream validator at pinned `cdb43b45f`; the deliberate protected-target/replace conflict was correctly rejected (`INVALID: operations[1]: protected target cannot be changed`). AYA's structural validator explicitly does **not** verify bytes or permissions; those were separately checked by the host test. Negative structural conflict is not inferred to pass before CI.

## Boundary map for a real Android host

| Identity / authority | Producer | Verified here | Required for shipping |
| --- | --- | --- | --- |
| Unmarked original/derivative bytes and coordinate frame | Retrofit Photo Picker and private repository | **Synthetic bytes only**; live import is separately qualified in PR #2 | Exact source revision hash, EXIF normalization, no crop drift, private grants |
| Annotation A/B/C IDs and operations | Retrofit editor | Synthetic normalized points/rect and distinct operations | User-confirmed targets; overlapping regions and preservation conflicts reviewed |
| Manufacturer/model and real multi-angle photos | Retrofit catalog with source/rights | **Fictional unbranded references only** | Real exact model identity, per-view provenance and explicit local storage/AI transfer permissions under G5 |
| Packet structure and relative paths | AYA v1 validator | Canonical validator in CI, separate from image verifier | Version-pinned schema/guide agreement and content-grant staging |
| Media bytes/hashes/size/magic | Host-owned byte verifier | **E1 synthetic verifier** plus six hostile cases | Android content URI read grants, transaction lifetime, source not revoked, SHA readback |
| Outgoing intent and user consent | Retrofit UI + AYA review | **No review/consent performed** | Exact thumbnails/recipient disclosure, approval, post-edit effective-manifest receipt |
| Android Sharesheet / independent receiver | AYA adapter and recipient | **Not exercised** | AYA [PR #151](https://github.com/gamesbyian/AYA/pull/151) merge and independent receiving-app complete-count/SHA proof |
| Result selection and proposal revision | Retrofit user-initiated import | **Absent** | User-selected returned bytes, originating approved manifest hash and visible Concept Visualization status |

### Known AYA host gap retained

The [unmerged Android sharing adapter PR #151](https://github.com/gamesbyian/AYA/pull/151) allows the user to edit instructions at its review stage, while the immediate `PreviewOpened` return does **not** provide the host a final approved-manifest receipt or evidence that Share was chosen. This was [reported upstream](https://github.com/gamesbyian/AYA/pull/151). This packet spike cannot settle that runtime contract and must never label a chooser, hash validation or local review as external delivery.

## Readiness and stop rule

If the canonical AYA validator accepts both structure and a separately byte-verified inventory, **G4 static packet formation advances to E1 SUPPORTED**. The authorized Android implementation should then reuse the validator/adapter rather than rebuilding Share. G4 **recipient interoperability remains BLOCKED** until adapter approval receipt and an independent receiver are observed; one passing synthetic manifest does not satisfy G4 as a whole.

Stop before any real AYA share, recurring background task, generic vendor catalog, scene-recognition pipeline or image return. The next non-human E2 step, if justified, is a dedicated disposable Android host/receiver using synthetic same-SHA attachments and explicit review, never real client-site images. That test must state exact APK/source, destination package, accepted count and received hashes.
