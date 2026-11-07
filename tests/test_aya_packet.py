import copy
import importlib.util
import tempfile
import unittest
from pathlib import Path

PROOF = Path(__file__).parents[1] / "experiments" / "aya_packet" / "generate_packet.py"
spec = importlib.util.spec_from_file_location("aya_packet", PROOF)
module = importlib.util.module_from_spec(spec)
import sys
sys.modules[spec.name] = module
spec.loader.exec_module(module)


class SyntheticPacket(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.packet = module.create_packet(self.root)

    def tearDown(self):
        self.tmp.cleanup()

    def test_real_bytes_and_oriented_dimensions_match(self):
        self.assertEqual([], module.verify_payload_files(self.root, self.packet))
        self.assertEqual(5, len(list(module.iter_images(self.packet))))
        self.assertEqual(3, len(self.packet["annotations"]))
        self.assertEqual(3, len(self.packet["operations"]))
        self.assertTrue(self.packet["constraints"]["preserveUnmarked"])
        self.assertFalse(self.packet["constraints"]["engineeringVerified"])

    def test_corrupted_original_is_detected_even_if_json_is_unchanged(self):
        path = self.root / "source" / "room.png"
        path.write_bytes(path.read_bytes()[:17])
        errors = module.verify_payload_files(self.root, self.packet)
        self.assertTrue(any("sha256 mismatch" in e for e in errors))
        self.assertTrue(any("unreadable image" in e for e in errors))

    def test_missing_reference_is_detected(self):
        (self.root / "references" / "boiler-front.png").unlink()
        self.assertTrue(any("missing regular image" in e for e in module.verify_payload_files(self.root, self.packet)))

    def test_declared_geometry_of_an_image_must_match_decoded_bytes(self):
        change = copy.deepcopy(self.packet)
        change["source"]["width"] += 1
        self.assertTrue(any("decoded size mismatch" in e for e in module.verify_payload_files(self.root, change)))

    def test_untrusted_path_and_duplicate_are_rejected(self):
        change = copy.deepcopy(self.packet)
        change["source"]["path"] = "source/../references/boiler-front.png"
        self.assertTrue(module.verify_payload_files(self.root, change))
        change = copy.deepcopy(self.packet)
        change["overlay"]["path"] = change["source"]["path"]
        self.assertTrue(any("duplicate" in e for e in module.verify_payload_files(self.root, change)))

    def test_symlink_is_rejected(self):
        target = self.root / "references" / "boiler-front.png"
        other = self.root / "references" / "original-copy.png"
        target.rename(other)
        target.symlink_to(other)
        self.assertTrue(any("symlink denied" in e for e in module.verify_payload_files(self.root, self.packet)))


if __name__ == "__main__":
    unittest.main()
