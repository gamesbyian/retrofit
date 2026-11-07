#!/usr/bin/env python3
"""Real Android system Photo Picker -> Retrofit private copy -> restart proof.

Adapted conceptually from Glimpse's *closed* PR #21 native-picker QA. This is
not a mock picker or direct repository import. Runs only on disposable emulators.
It never touches private field photos, other MediaStore rows, or user data.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

APP = "dev.gamesbyian.retrofit"
MAIN = APP + "/.MainActivity"
FIXTURE = APP + "/.debug.PickerFixtureActivity"
PICKER_PACKAGES = {
    "com.android.providers.media.module",
    "com.google.android.providers.media.module",
}


def adb(*args: str, binary: bool = False, check: bool = True, timeout: int = 45):
    result = subprocess.run(
        ["adb", *args], stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        timeout=timeout, check=False
    )
    if check and result.returncode:
        raise RuntimeError(
            "adb " + " ".join(args) + " failed: " +
            result.stderr.decode(errors="replace")[-900:]
        )
    if binary:
        return result.stdout
    return result.stdout.decode(errors="replace").strip()


def private(path: str) -> bytes:
    return adb("exec-out", "run-as", APP, "cat", "files/" + path, binary=True)


def read_json(path: str, timeout: float = 15.0) -> dict:
    deadline = time.monotonic() + timeout
    last = ""
    while time.monotonic() < deadline:
        try:
            raw = private(path).decode("utf-8")
            if raw.startswith("{"):
                return json.loads(raw)
        except (RuntimeError, UnicodeError, ValueError) as error:
            last = str(error)
        time.sleep(0.5)
    raise RuntimeError("Missing private receipt " + path + ": " + last[:200])


def hierarchy() -> tuple[ET.Element, str]:
    adb("shell", "uiautomator", "dump", "/sdcard/retrofit-picker-ui.xml", timeout=50)
    xml = adb("exec-out", "cat", "/sdcard/retrofit-picker-ui.xml")
    return ET.fromstring(xml), xml


def bounds(node: ET.Element):
    values = [int(x) for x in re.findall(r"\d+", node.attrib.get("bounds", ""))]
    return values if len(values) == 4 else None


def tap(node: ET.Element):
    bb = bounds(node)
    if bb is None:
        raise RuntimeError("Cannot tap UI node without bounds")
    x, y = (bb[0] + bb[2]) // 2, (bb[1] + bb[3]) // 2
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(0.8)


def wait_ui(predicate, timeout: float = 25.0) -> tuple[ET.Element, str]:
    deadline = time.monotonic() + timeout
    latest = ""
    while time.monotonic() < deadline:
        try:
            root, xml = hierarchy()
            latest = xml
            if predicate(root):
                return root, xml
        except (RuntimeError, ET.ParseError) as error:
            latest = str(error)
        time.sleep(0.6)
    raise RuntimeError("UI state not reached: " + latest[-1500:])


def choose_photo(root: ET.Element) -> ET.Element:
    parent = {child: group for group in root.iter() for child in group}
    matches = [
        node for node in root.iter("node")
        if node.attrib.get("text") == "Choose photo" or
        node.attrib.get("content-desc") == "Choose photo"
    ]
    clickable = []
    for node in matches:
        ancestor = node
        while ancestor is not None and ancestor.attrib.get("clickable") != "true":
            ancestor = parent.get(ancestor)
        if ancestor is not None and ancestor not in clickable:
            clickable.append(ancestor)
    if len(clickable) != 1:
        raise RuntimeError("Expected a single visible Choose photo action; found " +
                           str(len(clickable)))
    return clickable[0]


def picker_nodes(root: ET.Element):
    return [node for node in root.iter("node")
            if node.attrib.get("package") in PICKER_PACKAGES]


def select_single_photo(root: ET.Element):
    entries = picker_nodes(root)
    if not entries:
        raise RuntimeError("Android system Photo Picker not detected")
    display_height = max(
        [bounds(n)[3] for n in root.iter("node") if bounds(n)] or [0]
    )
    candidates = []
    for node in entries:
        bb = bounds(node)
        if node.attrib.get("clickable") != "true" or not bb:
            continue
        if bb[2] - bb[0] < 45 or bb[3] - bb[1] < 45:
            continue
        if bb[1] < max(240, display_height * 0.22):
            continue
        tag = " ".join(node.attrib.get(k, "")
                       for k in ("resource-id", "content-desc", "text")).lower()
        if any(term in tag for term in ("photo", "thumbnail", "image", "media_grid", "item")):
            candidates.append(node)
    if len(candidates) != 1:
        descriptions = [
            {"id": n.attrib.get("resource-id"), "text": n.attrib.get("text"),
             "description": n.attrib.get("content-desc"), "bounds": bounds(n)}
            for n in candidates
        ]
        raise RuntimeError(
            "Need exactly one identifiable synthetic media tile; got " +
            str(len(candidates)) + " " + json.dumps(descriptions)[:1600]
        )
    tap(candidates[0])


def app_has_photo(root: ET.Element) -> bool:
    nodes = list(root.iter("node"))
    return any(n.attrib.get("package") == APP and (
        n.attrib.get("content-desc") == "Equipment-room photograph" or
        "Tap the photo to add" in n.attrib.get("text", "")
    ) for n in nodes)


def confirm_if_needed() -> None:
    try:
        wait_ui(app_has_photo, timeout=5)
        return
    except RuntimeError:
        pass
    root, _ = hierarchy()
    choices = [
        n for n in picker_nodes(root)
        if n.attrib.get("clickable") == "true" and
        n.attrib.get("resource-id", "").endswith(":id/button_add") and
        n.attrib.get("text", "") in ("Add", "Add (1)")
    ]
    if len(choices) != 1:
        raise RuntimeError("Photo selection did not return to Retrofit and native Add (1) was not unique")
    tap(choices[0])
    wait_ui(app_has_photo)


def screenshot(path: Path):
    path.write_bytes(adb("exec-out", "screencap", "-p", binary=True))


def main(out: Path):
    out.mkdir(parents=True, exist_ok=True)
    assert adb("shell", "getprop", "ro.kernel.qemu") == "1", "REFUSE physical hardware"

    # This whole test requires a clean disposable AVD; never erase user app data.
    if adb("shell", "run-as", APP, "ls", "files/active-room.json", check=False).find(
            "No such file") == -1:
        # A missing file can be reported in stdout or stderr on different Android releases.
        try:
            private("active-room.json")
        except RuntimeError:
            pass
        else:
            raise RuntimeError("Refusing test: an active room already exists")

    adb("shell", "am", "start", "-n", FIXTURE)
    record = read_json("retrofit-picker-fixture.json")
    assert record.get("status") == "READY" and record.get("emulatorOnly") is True, record
    original = private("retrofit-picker-fixture-original.jpg")
    sha = hashlib.sha256(original).hexdigest()
    assert len(original) == record["sourceBytes"] and sha == record["sourceSHA256"]
    (out / "synthetic-source.jpg").write_bytes(original)

    adb("shell", "am", "start", "-n", MAIN)
    root, _ = wait_ui(lambda r: any(n.attrib.get("text") == "Choose photo"
                                    for n in r.iter("node")))
    tap(choose_photo(root))
    root, xml = wait_ui(lambda r: len(picker_nodes(r)) > 0)
    (out / "01-real-system-picker.xml").write_text(xml)
    screenshot(out / "01-real-system-picker.png")
    packages = sorted({n.attrib.get("package") for n in picker_nodes(root)})
    select_single_photo(root)
    confirm_if_needed()

    root, xml = wait_ui(app_has_photo)
    (out / "02-retrofit-after-picker.xml").write_text(xml)
    screenshot(out / "02-retrofit-after-picker.png")
    draft = read_json("active-room.json")
    assert draft["id"].startswith("room_") and re.fullmatch(r"room_[a-f0-9]{32}", draft["id"])
    assert draft["width"] == record["sourceWidth"]
    assert draft["height"] == record["sourceHeight"]
    assert draft["originalSha256"] == sha
    original_path = "rooms/" + draft["id"] + "/original.bin"
    oriented_path = "rooms/" + draft["id"] + "/oriented.jpg"
    assert private(original_path) == original, "Private original differs from picker source"
    oriented = private(oriented_path)
    assert hashlib.sha256(oriented).hexdigest() == draft["orientedSha256"]

    # Delete *only* the single exact synthetic source inserted by our debug-only fixture.
    adb("shell", "am", "start", "-n", FIXTURE, "--ez", "cleanup", "true")
    cleanup = read_json("retrofit-picker-cleanup.json")
    assert cleanup.get("status") == "DONE" and cleanup["deleted"] == 1, cleanup
    assert cleanup.get("deletedOnlyOwnFixture") is True
    assert cleanup["sourceSHA256"] == sha

    adb("shell", "am", "force-stop", APP)
    adb("shell", "am", "start", "-n", MAIN)
    wait_ui(app_has_photo)
    after = read_json("active-room.json")
    assert after == draft, "Active draft identity changed across complete process restart"
    assert private(original_path) == original, "Offline original changed after source deletion"
    assert private(oriented_path) == oriented, "Offline oriented derivative changed"
    screenshot(out / "03-source-deleted-restarted.png")

    proof = {
        "status": "PASS",
        "nativePickerPackages": packages,
        "sourceSHA256": sha,
        "sourceBytes": len(original),
        "privateOriginalByteIdentical": True,
        "privateOrientedDerivativeStable": True,
        "actualMediaStoreSourceDeleted": 1,
        "fullAppProcessRestart": True,
        "noPersonalPhotosUsed": True,
        "emulatorOnly": True,
        "pickerGrantAndHostImportProven": True,
        "activityMarkerGestureProvenHere": False,
        "physicalDeviceProven": False,
        "AYARecipientProven": False,
    }
    (out / "real-picker-proof.json").write_text(json.dumps(proof, indent=2) + "\n")
    print("PASS real Android Photo Picker, private SHA/bytes, source deletion and restart", flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default="app/build/reports/real-picker")
    args = parser.parse_args()
    destination = Path(args.out).resolve()
    try:
        main(destination)
    except Exception as error:
        destination.mkdir(parents=True, exist_ok=True)
        (destination / "real-picker-failure.txt").write_text(str(error)[:5000] + "\n")
        try:
            screenshot(destination / "real-picker-failure.png")
            _, xml = hierarchy()
            (destination / "real-picker-failure.xml").write_text(xml)
        except Exception:
            pass
        print("FAIL real Android Photo Picker acceptance:", error, file=sys.stderr, flush=True)
        sys.exit(1)
