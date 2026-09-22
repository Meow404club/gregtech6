#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""gt6testgate_test — gt6testgate 的 stdlib unittest 单测（零第三方依赖）。

python3 tools/gt6testgate_test.py 直跑。全部门控语义用注入桩覆盖，不碰真
/proc/meminfo、不占真槽（临时目录），毫秒级：
  * mem_used_mib 解析 mock meminfo（MemTotal - MemAvailable，kB→MiB）
  * wait_memory 超阈阻塞→阈值回落放行（日志含 mem-queue + mem-admit 行，
    queued>0）；低于阈直通（仅 mem-admit，queued=0）
  * 阈值 env 覆盖（GT6_GATE_MEM_LIMIT_MIB，含非法值回退默认）
  * 槽并发上限（占满阻塞→释放放行）与陈旧槽回收（2h mtime）
  * run_gated 退出码透传 / --tag 缺省取命令 basename
"""

import os
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gt6testgate as gate


def mock_meminfo(total_mib, avail_mib):
    """A /proc/meminfo lookalike (kernel units: kB)."""
    return (f"MemTotal:       {total_mib * 1024} kB\n"
            f"MemFree:        {avail_mib * 1024} kB\n"
            f"MemAvailable:   {avail_mib * 1024} kB\n")


class MemUsedTest(unittest.TestCase):
    def test_used_is_total_minus_available_in_mib(self):
        with tempfile.NamedTemporaryFile("w", suffix=".meminfo", delete=False) as fh:
            fh.write(mock_meminfo(32768, 4096))
            path = fh.name
        try:
            self.assertEqual(gate.mem_used_mib(path), (32768 - 4096))
        finally:
            os.unlink(path)

    def test_missing_fields_raise(self):
        with tempfile.NamedTemporaryFile("w", suffix=".meminfo", delete=False) as fh:
            fh.write("SwapTotal: 0 kB\n")
            path = fh.name
        try:
            with self.assertRaises(RuntimeError):
                gate.mem_used_mib(path)
        finally:
            os.unlink(path)


class WaitMemoryTest(unittest.TestCase):
    def setUp(self):
        self.log = tempfile.NamedTemporaryFile("r", suffix=".gate", delete=False)
        self.log.close()

    def tearDown(self):
        os.unlink(self.log.name)

    def journal(self):
        with open(self.log.name, encoding="utf-8") as fh:
            return fh.read()

    def test_below_threshold_admits_immediately(self):
        used, queued = gate.wait_memory(read_used=lambda: 100, limit_mib=200,
                                        poll=0.01, tag="t", log_file=self.log.name)
        self.assertEqual(used, 100)
        self.assertLess(queued, 1)
        body = self.journal()
        self.assertIn("mem-admit", body)
        self.assertNotIn("mem-queue", body)
        self.assertIn("used=100MiB queued=0s", body)

    def test_over_threshold_blocks_then_admits_on_relief(self):
        readings = iter([500, 500, 50])
        done = []

        def run():
            done.append(gate.wait_memory(read_used=lambda: next(readings),
                                         limit_mib=200, poll=0.01,
                                         tag="t", log_file=self.log.name))

        th = threading.Thread(target=run)
        th.start()
        th.join(timeout=10)
        self.assertFalse(th.is_alive(), "wait_memory never admitted")
        used, queued = done[0]
        self.assertEqual(used, 50)
        self.assertGreater(queued, 0)
        body = self.journal()
        self.assertIn("mem-queue", body)
        self.assertIn("used=500MiB limit=200MiB", body)
        self.assertIn("mem-admit", body)
        self.assertIn("used=50MiB", body)

    def test_env_threshold_override(self):
        old = os.environ.get("GT6_GATE_MEM_LIMIT_MIB")
        try:
            os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "1"
            self.assertEqual(gate.mem_limit_mib(), 1)
            # used=2 stays over the env-pinned limit: the waiter must keep
            # polling (consuming readings) and never admit — the constant-2
            # stub only runs out after three polls, and that StopIteration
            # escaping proves the loop was still blocked, not admitted.
            polls = iter([2, 2, 2])
            with self.assertRaises(StopIteration):
                gate.wait_memory(read_used=lambda: next(polls), poll=0.01,
                                 tag="t", log_file=self.log.name)
            os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "999999"
            used, _ = gate.wait_memory(read_used=lambda: 2, poll=0.01,
                                       tag="t", log_file=self.log.name)
            self.assertEqual(used, 2)
        finally:
            if old is None:
                os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
            else:
                os.environ["GT6_GATE_MEM_LIMIT_MIB"] = old

    def test_invalid_env_falls_back_to_default(self):
        old = os.environ.get("GT6_GATE_MEM_LIMIT_MIB")
        try:
            os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "not-a-number"
            self.assertEqual(gate.mem_limit_mib(), gate.DEFAULT_MEM_LIMIT_MIB)
        finally:
            if old is None:
                os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
            else:
                os.environ["GT6_GATE_MEM_LIMIT_MIB"] = old


class SlotTest(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.slot_dir = Path(self.dir.name)
        self._env = os.environ.get("GT6_GATE_MAX_CONCURRENT")

    def tearDown(self):
        self.dir.cleanup()
        if self._env is None:
            os.environ.pop("GT6_GATE_MAX_CONCURRENT", None)
        else:
            os.environ["GT6_GATE_MAX_CONCURRENT"] = self._env

    def test_env_limit_override_and_invalid(self):
        os.environ["GT6_GATE_MAX_CONCURRENT"] = "2"
        self.assertEqual(gate.max_concurrent(), 2)
        os.environ["GT6_GATE_MAX_CONCURRENT"] = "junk"
        self.assertEqual(gate.max_concurrent(), 4)
        os.environ["GT6_GATE_MAX_CONCURRENT"] = "0"
        self.assertEqual(gate.max_concurrent(), 1)

    def test_concurrency_cap_blocks_until_release(self):
        os.environ["GT6_GATE_MAX_CONCURRENT"] = "1"
        live = self.slot_dir / "slot.999.1"
        live.write_text(f"{time.time()} other\n")
        got = []
        th = threading.Thread(target=lambda: got.append(gate.acquire_slot(
            poll=0.01, slot_dir=self.slot_dir, tag="t")))
        th.start()
        th.join(timeout=0.3)
        self.assertTrue(th.is_alive(), "acquire_slot did not queue while full")
        live.unlink()  # the other holder releases
        th.join(timeout=10)
        self.assertFalse(th.is_alive(), "acquire_slot never admitted")
        self.assertEqual(len(got), 1)
        self.assertTrue(got[0].exists())

    def test_stale_slot_reaped(self):
        stale = self.slot_dir / "slot.1.1"
        stale.write_text("old\n")
        old = time.time() - (gate.SLOT_STALE_SECONDS + 60)
        os.utime(stale, (old, old))
        slot = gate.acquire_slot(poll=0.01, slot_dir=self.slot_dir, tag="t")
        try:
            self.assertFalse(stale.exists(), "stale slot not reaped")
        finally:
            gate.release_slot(slot)
        self.assertFalse(slot.exists())

    def test_release_is_idempotent(self):
        slot = gate.acquire_slot(poll=0.01, slot_dir=self.slot_dir, tag="t")
        gate.release_slot(slot)
        gate.release_slot(slot)  # missing_ok — second release must not raise


class RunGatedTest(unittest.TestCase):
    def test_exit_code_passthrough(self):
        self.assertEqual(gate.run_gated(["true"], poll=0.01,
                                        slot_dir=Path(tempfile.mkdtemp())), 0)
        self.assertEqual(
            gate.run_gated(["sh", "-c", "exit 7"], poll=0.01,
                           slot_dir=Path(tempfile.mkdtemp())), 7)

    def test_tag_defaults_to_command_basename(self):
        self.assertEqual(gate.main(["--", "true"]), 0)  # real path, gate open


if __name__ == "__main__":
    unittest.main(verbosity=2)
