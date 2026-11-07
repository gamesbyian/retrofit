"""Independent synthetic source + hostile candidates. No OEM or client media."""

import importlib.util
import tempfile
import unittest
from pathlib import Path

from PIL import Image

PROOF = Path(__file__).parents[1] / "experiments" / "pixel_locality" / "preserve.py"
spec = importlib.util.spec_from_file_location("preserve", PROOF)
preserve = importlib.util.module_from_spec(spec)
import sys
sys.modules[spec.name] = preserve
spec.loader.exec_module(preserve)


def frames(mode="RGB", size=(14, 9)):
    source = Image.new(mode, size)
    candidate = Image.new(mode, size)
    mask = Image.new("L", size, 0)
    for y in range(size[1]):
        for x in range(size[0]):
            v = (x * 23 + y * 31) % 256
            source.putpixel((x, y), (v, 100, 200) if mode == "RGB" else (v, 100, 200, 140))
            candidate.putpixel((x, y), (255-v, 90, 180) if mode == "RGB" else (255-v, 90, 180, 25))
            if 3 <= x < 9 and 2 <= y < 7:
                mask.putpixel((x, y), 255)
    return source, candidate, mask


class PixelPreservation(unittest.TestCase):
    def test_binary_mask_preserves_every_protected_pixel(self):
        src, cand, mask = frames()
        original = src.tobytes()
        out, report = preserve.preserve(src, cand, mask, registered=True)
        self.assertEqual(report.allowed_pixels, 30)
        self.assertEqual(report.protected_pixels, 96)
        self.assertEqual(report.drifted_protected_pixels_after, 0)
        self.assertGreater(report.drifted_protected_pixels_before, 0)
        for y in range(9):
            for x in range(14):
                expected = cand if mask.getpixel((x, y)) else src
                self.assertEqual(out.getpixel((x, y)), expected.getpixel((x, y)))
        self.assertEqual(src.tobytes(), original)

    def test_rgba_preserves_alpha_in_protected_region(self):
        src, cand, mask = frames("RGBA")
        out, report = preserve.preserve(src, cand, mask, registered=True)
        self.assertEqual(out.getpixel((0, 0)), src.getpixel((0, 0)))
        self.assertEqual(out.getpixel((3, 2)), cand.getpixel((3, 2)))
        self.assertEqual(report.mode, "RGBA")

    def test_soft_edge_is_opt_in_and_zero_mask_is_still_exact(self):
        src, cand, mask = frames()
        mask.putpixel((3, 2), 127)
        with self.assertRaisesRegex(ValueError, "explicit"):
            preserve.preserve(src, cand, mask, registered=True)
        out, report = preserve.preserve(src, cand, mask, registered=True, allow_soft_mask=True)
        self.assertEqual(report.feathered_pixels, 1)
        self.assertEqual(report.drifted_protected_pixels_after, 0)
        self.assertNotEqual(out.getpixel((3, 2)), src.getpixel((3, 2)))
        self.assertNotEqual(out.getpixel((3, 2)), cand.getpixel((3, 2)))

    def test_rejects_registration_dimension_and_mode_mismatch(self):
        src, cand, mask = frames()
        with self.assertRaisesRegex(ValueError, "Registration"):
            preserve.preserve(src, cand, mask, registered=False)
        with self.assertRaisesRegex(ValueError, "Dimensions"):
            preserve.preserve(src, Image.new("RGB", (15, 9)), mask, registered=True)
        with self.assertRaisesRegex(ValueError, "Candidate mode"):
            preserve.preserve(src, cand.convert("RGBA"), mask, registered=True)
        with self.assertRaisesRegex(ValueError, "must be 8-bit L"):
            preserve.preserve(src, cand, mask.convert("RGB"), registered=True)

    def test_rejects_empty_full_and_unoriented_inputs(self):
        src, cand, mask = frames()
        with self.assertRaisesRegex(ValueError, "both protected"):
            preserve.preserve(src, cand, Image.new("L", src.size), registered=True)
        with self.assertRaisesRegex(ValueError, "both protected"):
            preserve.preserve(src, cand, Image.new("L", src.size, 255), registered=True)
        src.getexif()[274] = 6
        with self.assertRaisesRegex(ValueError, "EXIF orientation"):
            preserve.preserve(src, cand, mask, registered=True)

    def test_file_png_roundtrip_refuses_lossy_and_overwrite(self):
        src, cand, mask = frames("RGBA")
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            a, b, m, output = [base/x for x in ("source.png", "candidate.png", "mask.png", "out.png")]
            src.save(a); cand.save(b); mask.save(m)
            report = preserve.run_files(a, b, m, output, registered=True)
            self.assertEqual(report.drifted_protected_pixels_after, 0)
            with Image.open(output) as loaded:
                for y in range(9):
                    for x in range(14):
                        if mask.getpixel((x, y)) == 0:
                            self.assertEqual(loaded.getpixel((x, y)), src.getpixel((x, y)))
            with self.assertRaises(FileExistsError):
                preserve.run_files(a, b, m, output, registered=True)
            with self.assertRaisesRegex(ValueError, "PNG"):
                preserve.run_files(a, b, m, base/"out.jpg", registered=True)
            with self.assertRaisesRegex(ValueError, "overwrite source"):
                preserve.run_files(a, b, m, a, registered=True)

    def test_unregistered_same_size_shift_not_automatically_fixed(self):
        src, cand, mask = frames()
        shifted = cand.transform(cand.size, Image.Transform.AFFINE, (1,0,1,0,1,0))
        # Equal dimensions alone are *not* evidence of alignment. The operator
        # explicitly bears this responsibility, and a failed assertion stops.
        with self.assertRaisesRegex(ValueError, "Registration"):
            preserve.preserve(src, shifted, mask, registered=False)


if __name__ == "__main__":
    unittest.main()
