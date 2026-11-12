import datetime
import importlib.util
import pathlib
import unittest
from dataclasses import replace


MODULE_PATH = pathlib.Path(__file__).resolve().parents[1] / "deploy" / "preflight.py"
SPEC = importlib.util.spec_from_file_location("device_preflight", MODULE_PATH)
PREFLIGHT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PREFLIGHT)


class PreflightTest(unittest.TestCase):
    def setUp(self):
        self.snapshot = PREFLIGHT.Snapshot("ubuntu", "24.04", 700, 10.0, [22], True, False, True, True)
        self.today = datetime.date(2026, 11, 12)

    def assess(self, **changes):
        return PREFLIGHT.assess(replace(self.snapshot, **changes), self.today)

    def test_supported_roomy_host_has_no_static_blockers(self):
        self.assertEqual([], self.assess())

    def test_existing_vm_is_blocked_without_esm_and_headroom(self):
        blockers = self.assess(os_version="20.04", available_mib=504)
        self.assertEqual(2, len(blockers))
        self.assertTrue(any("security coverage" in message for message in blockers))
        self.assertTrue(any("600 MiB" in message for message in blockers))

    def test_esm_allows_supported_legacy_host_review(self):
        self.assertEqual([], self.assess(os_version="20.04", esm_enabled=True))

    def test_existing_proxy_or_unknown_ports_block_binding(self):
        self.assertTrue(self.assess(listening_ports=[22, 443]))
        self.assertTrue(self.assess(ports_known=False))

    def test_missing_tooling_low_disk_and_unknown_os_fail_closed(self):
        self.assertTrue(self.assess(docker_present=False))
        self.assertTrue(self.assess(compose_present=False))
        self.assertTrue(self.assess(free_disk_gib=1.0))
        self.assertTrue(self.assess(os_id="ol", os_version="8"))

    def test_expired_standard_support_requires_new_review(self):
        snapshot = replace(self.snapshot, os_version="22.04")
        self.assertTrue(PREFLIGHT.assess(snapshot, datetime.date(2028, 1, 1)))


if __name__ == "__main__":
    unittest.main()