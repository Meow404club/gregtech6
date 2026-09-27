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
  * v2 角色硬闸：command_mode 判全量（gradlew+裸 test/cleanTest 无 --tests）；
    full+coder 立即拒（exit 2，子进程未执行）；full+review 放行持 flock
    全局锁；filtered（--tests）+coder 放行；双 full 第二个排队（打印
    queueing 提示，锁释放后才准入）；resolve_role CLI>env>默认 coder，
    非法值回退 coder（fail-closed）
"""

import fcntl
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


class FullGateTest(unittest.TestCase):
    """v2 角色硬闸：full 仅 review；coder 拒；filtered 放行；双 full 互斥。"""

    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.tmp = Path(self.dir.name)
        self.slot_dir = self.tmp / "slots"
        self.full_lock = self.tmp / "full.lock"
        self.log_file = self.tmp / "gate.log"
        self.lines = []
        # 内存闸钉到必过：本类只测角色/锁语义，不排队真 /proc/meminfo
        self._mem_env = os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "999999"
        self._role_env = os.environ.pop(gate.ROLE_ENV, None)  # 默认 coder 确定
        self._stub_n = 0

    def tearDown(self):
        self.dir.cleanup()
        if self._mem_env is None:
            os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        else:
            os.environ["GT6_GATE_MEM_LIMIT_MIB"] = self._mem_env
        if self._role_env is not None:
            os.environ[gate.ROLE_ENV] = self._role_env

    def gradlew(self, body):
        """Executable stub named exactly `gradlew` (mode detection keys on it).

        Each call gets its own subdirectory — the mutex test needs two stubs
        with different bodies alive at once.
        """
        self._stub_n += 1
        d = self.tmp / f"stub{self._stub_n}"
        d.mkdir()
        stub = d / "gradlew"
        stub.write_text(f"#!/bin/sh\n{body}\n")
        stub.chmod(0o755)
        return str(stub)

    def gated(self, cmd, role):
        return gate.run_gated(cmd, poll=0.01, slot_dir=self.slot_dir,
                              log_file=self.log_file, log=self.lines.append,
                              role=role, full_lock=self.full_lock)

    def journal(self):
        return self.log_file.read_text(encoding="utf-8")

    def test_resolve_role_precedence_and_fail_closed(self):
        old = os.environ.get(gate.ROLE_ENV)
        try:
            os.environ.pop(gate.ROLE_ENV, None)
            self.assertEqual(gate.resolve_role(), "coder")      # default
            self.assertEqual(gate.resolve_role("review"), "review")  # CLI wins
            os.environ[gate.ROLE_ENV] = "review"
            self.assertEqual(gate.resolve_role(), "review")     # env honored
            self.assertEqual(gate.resolve_role("coder"), "coder")  # CLI > env
            os.environ[gate.ROLE_ENV] = "nonsense"
            self.assertEqual(gate.resolve_role(), "coder")      # fail-closed
        finally:
            if old is None:
                os.environ.pop(gate.ROLE_ENV, None)
            else:
                os.environ[gate.ROLE_ENV] = old

    def test_command_mode_table(self):
        full = [
            ["./gradlew", ":mdk:cleanTest"],
            ["./gradlew", ":test"],
            ["./gradlew", ":mdk:1.20.1-forge:cleanTest",
             ":mdk:1.20.1-forge:test", "--no-build-cache"],
            ["gradle", "cleanTest"],
        ]
        other = [
            ["./gradlew", ":mdk:cleanTest", "--tests", "gregtech.FooTest"],
            ["./gradlew", ":mdk:test", "--tests=gregtech.FooTest"],
            ["./gradlew", "compileTestJava"],                 # 非 test 套件任务
            ["./gradlew", ":mdk:runData"],
            ["./gradlew", "build"],
            ["python3", "tools/rcon/sweep.py", "--group", "test"],  # 非 gradle
            ["sh", "-c", "./gradlew :test"],                  # 包一层=不识别（文档钉）
        ]
        for cmd in full:
            self.assertEqual(gate.command_mode(cmd), "full", cmd)
        for cmd in other:
            self.assertEqual(gate.command_mode(cmd), "other", cmd)

    def test_full_coder_rejected_before_any_gate(self):
        marker = self.tmp / "ran"
        stub = self.gradlew(f"touch {marker}")
        rc = self.gated([stub, ":mdk:cleanTest"], role="coder")
        self.assertEqual(rc, gate.FULL_DENY_EXIT)   # exit 非 0
        self.assertFalse(marker.exists())           # 子进程根本没跑
        self.assertIn("full-reject", self.journal())
        self.assertTrue(any("全量归审查席" in line and "--tests" in line
                            for line in self.lines), self.lines)

    def test_cli_default_role_is_coder_full_denied(self):
        # CLI 路径默认 coder（环境未设）：全量拒，exit 2，无需任何排队
        stub = self.gradlew("exit 0")
        rc = gate.main(["--", stub, ":mdk:cleanTest"])
        self.assertEqual(rc, gate.FULL_DENY_EXIT)
        rc = gate.main(["--role", "coder", "--", stub, ":test"])
        self.assertEqual(rc, gate.FULL_DENY_EXIT)

    def test_full_review_runs_and_lock_freed(self):
        marker = self.tmp / "ran"
        stub = self.gradlew(f"touch {marker}")
        rc = self.gated([stub, ":mdk:cleanTest"], role="review")
        self.assertEqual(rc, 0)                      # 放行且子进程真跑
        self.assertTrue(marker.exists())
        self.assertIn("full-admit", self.journal())
        probe = open(self.full_lock, "a+")           # 锁已释放：非阻塞可立即取
        try:
            fcntl.flock(probe, fcntl.LOCK_EX | fcntl.LOCK_NB)
            fcntl.flock(probe, fcntl.LOCK_UN)
        finally:
            probe.close()

    def test_filtered_coder_runs(self):
        marker = self.tmp / "ran"
        stub = self.gradlew(f"touch {marker}")
        rc = self.gated([stub, ":mdk:cleanTest", "--tests", "gregtech.FooTest"],
                        role="coder")
        self.assertEqual(rc, 0)
        self.assertTrue(marker.exists())
        self.assertNotIn("full-admit", self.journal())  # filtered 不走全量锁

    def test_second_full_queues_until_lock_freed(self):
        order = self.tmp / "order"
        slow = self.gradlew(f"echo A-start >> {order}; sleep 1; echo A-done >> {order}")
        fast = self.gradlew(f"echo B >> {order}")
        lock_path, slot_dir = self.full_lock, self.slot_dir
        log_file, lines = self.log_file, self.lines

        def run_a():
            gate.run_gated([slow, ":test"], poll=0.01, slot_dir=slot_dir,
                           log_file=log_file, log=lines.append, role="review",
                           full_lock=lock_path)

        th_a = threading.Thread(target=run_a)
        th_a.start()
        deadline = time.monotonic() + 10   # 等 A 真持锁（子进程已起）
        while not (order.exists() and "A-start" in order.read_text()):
            self.assertLess(time.monotonic(), deadline, "A never started")
            time.sleep(0.02)

        b_lines, b_log = [], self.tmp / "b.log"
        rc_b = []

        def run_b():
            rc_b.append(gate.run_gated(
                [fast, ":test"], poll=0.01, slot_dir=slot_dir,
                log_file=b_log, log=b_lines.append, role="review",
                full_lock=lock_path))

        th_b = threading.Thread(target=run_b)
        th_b.start()
        th_b.join(timeout=15)
        self.assertFalse(th_b.is_alive(), "second full run never admitted")
        th_a.join(timeout=15)
        self.assertEqual(rc_b[0], 0)
        # B 在 A 持锁期间到达 → 打印排队提示；A 子进程收尾后 B 才准入
        self.assertTrue(any("queueing" in line for line in b_lines), b_lines)
        self.assertIn("full-queue", b_log.read_text(encoding="utf-8"))
        self.assertEqual(order.read_text().splitlines(),
                         ["A-start", "A-done", "B"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
