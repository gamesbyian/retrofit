"""Synthetic registration positive and negative controls. No field photographs."""

import importlib.util
import tempfile
import unittest
from pathlib import Path

import cv2
import numpy as np

PROOF = Path(__file__).parents[1] / "experiments" / "pixel_locality" / "registration_diagnostic.py"
spec = importlib.util.spec_from_file_location("registration_diagnostic", PROOF)
module = importlib.util.module_from_spec(spec)
import sys
sys.modules[spec.name] = module
spec.loader.exec_module(module)


class RegistrationDiagnostic(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        random = np.random.default_rng(43)
        # Repeatable high-detail RGB fixture with a broad editable central box.
        self.source = random.integers(32, 230, (256, 384, 3), dtype=np.uint8)
        cv2.rectangle(self.source, (90, 70), (210, 190), (80, 80, 80), 3)
        self.source_path = self.root / "source.png"
        self.candidate_path = self.root / "candidate.png"
        cv2.imwrite(str(self.source_path), self.source)
        transform = np.array([[1., 0., 3.], [0., 1., -2.]], dtype=np.float32)
        self.candidate = cv2.warpAffine(
            self.source, transform, (384, 256), borderMode=cv2.BORDER_REFLECT
        )
        cv2.rectangle(self.candidate, (95, 76), (205, 185), (5, 215, 170), -1)
        cv2.imwrite(str(self.candidate_path), self.candidate)

    def tearDown(self):
        self.temp.cleanup()

    def test_detects_translation_and_maintains_protected_pixels(self):
        before = self.source_path.read_bytes()
        result = module.inspect(self.source_path, self.candidate_path, (80, 50, 220, 205))
        self.assertGreater(result["feature_matches"], 40)
        self.assertGreater(result["ransac_inliers"], 30)
        self.assertLess(result["inlier_median_reprojection_px"], 1.5)
        self.assertGreater(result["protected_changed_pixels_in_candidate_after_estimated_affine"], 0)
        self.assertEqual(0, result["protected_changed_pixels_after_local_copy"])
        self.assertFalse(result["real_registration_or_visual_quality_certified"])
        self.assertFalse(result["output_media_saved"])
        self.assertEqual(before, self.source_path.read_bytes())

    def test_dimensions_require_explicit_exploratory_resize(self):
        cv2.imwrite(str(self.candidate_path), self.candidate[:-3,:,:])
        with self.assertRaisesRegex(ValueError, "Different image dimensions"):
            module.inspect(self.source_path, self.candidate_path, (80,50,220,205))

    def test_invalid_mask_and_inadequate_features_refuse(self):
        with self.assertRaisesRegex(ValueError, "Edit rectangle"):
            module.inspect(self.source_path, self.candidate_path, (0,0,384,256))
        blank = np.zeros_like(self.source)
        cv2.imwrite(str(self.source_path),blank)
        cv2.imwrite(str(self.candidate_path),blank)
        with self.assertRaisesRegex(ValueError, "Insufficient"):
            module.inspect(self.source_path,self.candidate_path,(80,50,220,205))

    def test_different_size_positive_requires_diagnostic_resize_disclaimer(self):
        resized = cv2.resize(self.candidate,(384,253),interpolation=cv2.INTER_LINEAR)
        cv2.imwrite(str(self.candidate_path),resized)
        # The warp may still be measurable, but a resizing result is never
        # claimed to establish true registration.
        result=module.inspect(self.source_path,self.candidate_path,(80,50,220,205),
                              diagnostic_resize=True)
        self.assertTrue(result["candidate_resized_for_diagnostic"])
        self.assertTrue(result["resize_was_not_a_registered_output"])
        self.assertEqual(0,result["protected_changed_pixels_after_local_copy"])


if __name__ == "__main__":
    unittest.main()
