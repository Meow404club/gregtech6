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
  * v3 预测闸（ops-testgate-v3-memory-predict）：classify 任务分类表；
    台账读取容错（损坏 JSON 重建/坏条目剔除）/冷启动默认/14 天半衰衰减/
    max 合成/30G 边界整；wait_memory 预测排队（est 叠加）；run_gated 作
    runner——退出码透传、峰值采样后写台账（运行后非运行前）、GITHUB_ACTIONS
    零门槛透传；dry_run 三场景（冷启动 ADMIT/台账超限 QUEUE/full+coder
    REJECT）；`run` 子命令与旧 CLI 形态等价；防旁路 hook 判定表
    （带/不带 gate 前缀/CI/域外/坏 JSON）
"""

import fcntl
import json
import os
import subprocess
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
    def setUp(self):
        # v3 预测闸按 class 估算叠加到真 /proc 占用上：钉高阈值，本类只测
        # 退出码透传/tag 缺省，不排队真内存
        self._mem_env = os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "999999"

    def tearDown(self):
        if self._mem_env is None:
            os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        else:
            os.environ["GT6_GATE_MEM_LIMIT_MIB"] = self._mem_env

    def test_exit_code_passthrough(self):
        ledger = Path(tempfile.mkdtemp()) / "ledger.json"  # 不污染真台账
        self.assertEqual(gate.run_gated(["true"], poll=0.01,
                                        slot_dir=Path(tempfile.mkdtemp()),
                                        ledger_path=ledger), 0)
        self.assertEqual(
            gate.run_gated(["sh", "-c", "exit 7"], poll=0.01,
                           slot_dir=Path(tempfile.mkdtemp()),
                           ledger_path=ledger), 7)

    def test_tag_defaults_to_command_basename(self):
        old = gate.LEDGER_PATH
        gate.LEDGER_PATH = Path(tempfile.mkdtemp()) / "ledger.json"
        try:
            self.assertEqual(gate.main(["--", "true"]), 0)  # real path, gate open
        finally:
            gate.LEDGER_PATH = old


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
                              role=role, full_lock=self.full_lock,
                              ledger_path=self.tmp / "ledger.json")

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
                           full_lock=lock_path,
                           ledger_path=self.tmp / "ledger.json")

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
                full_lock=lock_path, ledger_path=self.tmp / "ledger.json"))

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


class ClassifyTest(unittest.TestCase):
    """v3 任务分类表：CLI 形态 → 估算 class（--class 显式覆盖在 main 层）。"""

    def test_table(self):
        table = {
            "full-test": [("./gradlew", ":mdk:cleanTest"),
                          ("gradle", "cleanTest"),
                          ("./gradlew", ":test", "--no-build-cache")],
            "filtered-test": [("./gradlew", ":mdk:test", "--tests", "gregtech.Foo"),
                              ("./gradlew", "--tests=gregtech.Foo", "test")],
            "rundata": [("./gradlew", ":mdk:1.20.1-forge:runData"),
                        ("gradle", "runData")],
            "compile": [("./gradlew", "compileJava"),
                        ("./gradlew", ":mdk:compileTestJava"),
                        ("./gradlew", "classes"), ("./gradlew", "jar")],
            "rcon-boot": [("python3", "tools/rcon/sweep.py", "--group", "test"),
                          ("python3", "tools/rcon/gt6server.py", "boot")],
            "other": [("./gradlew", "build"),          # 依赖链不过闸（v2 声明）
                      ("sh", "-c", "echo hi"),
                      ("python3", "tools/datagen_tree_check.py")],
        }
        for want, cmds in table.items():
            for cmd in cmds:
                self.assertEqual(gate.classify(list(cmd)), want, cmd)

    def test_class_choices_cover_table(self):
        self.assertEqual(set(gate.CLASSES), set(gate.COLD_ESTIMATE_MIB))


class LedgerTest(unittest.TestCase):
    """v3 峰值台账：读容错/冷启动/衰减/max 合成。"""

    def setUp(self):
        # 独占临时目录：/tmp 扫描 .tmp 残留会撞上别的进程的文件
        self.dir = tempfile.TemporaryDirectory()
        self.path = os.path.join(self.dir.name, "ledger.json")

    def tearDown(self):
        self.dir.cleanup()

    def test_load_missing_corrupt_and_malformed_entries(self):
        self.assertEqual(gate.load_ledger(self.path), {})          # 缺文件
        with open(self.path, "w") as fh:
            fh.write("{corrupt json!!")
        self.assertEqual(gate.load_ledger(self.path), {})          # 损坏重建
        with open(self.path, "w") as fh:
            json.dump({"good": {"estimate": 5, "updated": 1.0},
                       "str": "nope",
                       "half": {"estimate": "nan", "updated": 1.0},
                       "nodate": {"estimate": 9}}, fh)
        self.assertEqual(gate.load_ledger(self.path),
                         {"good": {"estimate": 5, "updated": 1.0}})
        with open(self.path, "w") as fh:
            fh.write("[1, 2]")
        self.assertEqual(gate.load_ledger(self.path), {})          # 非对象

    def test_cold_default_table(self):
        self.assertEqual(gate.COLD_ESTIMATE_MIB,
                         {"full-test": 12000, "filtered-test": 6000,
                          "compile": 4000, "rundata": 6000,
                          "rcon-boot": 5000, "other": 8000})
        for cls, cold in gate.COLD_ESTIMATE_MIB.items():
            self.assertEqual(gate.estimate_for(cls, {}), cold)

    def test_ledger_overrides_and_decays_with_cold_floor(self):
        now = time.time()
        # 台账值覆盖冷启动：compile 记录 16000，14 天前 → 衰减 8000 > 冷 4000
        self.assertEqual(gate.estimate_for(
            "compile", {"compile": {"estimate": 16000, "updated": now - 14 * 86400}},
            now=now), 8000)
        # 衰减到冷默认之下时被冷默认托底：full-test 12000@14d → 6000，托到 12000
        self.assertEqual(gate.estimate_for(
            "full-test",
            {"full-test": {"estimate": 12000, "updated": now - 14 * 86400}},
            now=now), 12000)
        # 新鲜记录原样生效
        self.assertEqual(gate.estimate_for(
            "rcon-boot", {"rcon-boot": {"estimate": 7000, "updated": now}},
            now=now), 7000)
        # 未记录的 class 落冷启动
        self.assertEqual(gate.estimate_for(
            "compile", {"full-test": {"estimate": 20000, "updated": now}},
            now=now), 4000)

    def test_record_folds_max_of_decayed_and_observed(self):
        now = time.time()
        # 新鲜历史 12000 > 本次 9000 → 保持 12000
        with open(self.path, "w") as fh:
            json.dump({"full-test": {"estimate": 12000, "updated": now}}, fh)
        gate.record_observation("full-test", 9000, self.path, now=now)
        entry = gate.load_ledger(self.path)["full-test"]
        self.assertEqual(entry["estimate"], 12000)
        self.assertEqual(entry["last_peak"], 9000)
        self.assertEqual(entry["updated"], now)
        # 陈旧历史 12000@28d（衰减 3000）< 本次 4000 → 4000
        old = {"full-test": {"estimate": 12000, "updated": now - 28 * 86400}}
        with open(self.path, "w") as fh:
            json.dump(old, fh)
        got = gate.record_observation("full-test", 4000, self.path, now=now)
        self.assertEqual(got, 4000)
        # 原子写：无 .tmp 残留
        self.assertEqual([f for f in os.listdir(os.path.dirname(self.path))
                          if f.endswith(".tmp")], [])

    def test_record_creates_ledger_and_lock_file(self):
        gate.record_observation("compile", 1234, self.path)
        self.assertTrue(os.path.exists(self.path))
        self.assertTrue(os.path.exists(self.path + ".lock"))


class WaitMemoryPredictTest(unittest.TestCase):
    """v3 预测闸：admit 条件 used+est ≤ limit，含 30G 边界整。"""

    def setUp(self):
        fd, self.path = tempfile.mkstemp(suffix=".gate")
        os.close(fd)

    def tearDown(self):
        os.unlink(self.path)

    def journal(self):
        with open(self.path) as fh:
            return fh.read()

    def test_estimate_pushes_over_then_admits_on_relief(self):
        readings = iter([100, 40])
        used, queued = gate.wait_memory(read_used=lambda: next(readings),
                                        limit_mib=200, poll=0.01, tag="t",
                                        log_file=self.path, estimate_mib=150)
        self.assertEqual((used, queued > 0), (40, True))
        body = self.journal()
        self.assertIn("mem-queue", body)
        self.assertIn("est=150MiB", body)
        self.assertIn("predicted=250MiB", body)
        self.assertIn("mem-admit", body)

    def test_boundary_at_limit_admits_immediately(self):
        # 20000 + 10000 == 30000 == limit：整好压线 → 不排队（≤30G 语义）
        used, queued = gate.wait_memory(read_used=lambda: 20000,
                                        limit_mib=30000, poll=0.01, tag="t",
                                        log_file=self.path, estimate_mib=10000)
        self.assertEqual(used, 20000)
        self.assertLess(queued, 0.01)
        self.assertNotIn("mem-queue", self.journal())

    def test_one_mib_over_limit_queues(self):
        readings = iter([20001, 0])
        gate.wait_memory(read_used=lambda: next(readings), limit_mib=30000,
                         poll=0.01, tag="t", log_file=self.path,
                         estimate_mib=10000)
        self.assertIn("predicted=30001MiB", self.journal())

    def test_zero_estimate_keeps_v2_semantics(self):
        readings = iter([500, 100])
        used, queued = gate.wait_memory(read_used=lambda: next(readings),
                                        limit_mib=200, poll=0.01, tag="t",
                                        log_file=self.path)
        self.assertEqual(used, 100)   # est=0 时行为与 v2 相同（gt6server 路径）
        self.assertIn("est=0MiB", self.journal())


class TreeRssTest(unittest.TestCase):
    """进程树 RSS 采样：真进程 >0；假 /proc 目录精确定和。"""

    def test_self_tree_positive(self):
        self.assertGreater(gate.tree_rss_mib(os.getpid()), 0)

    def test_fake_proc_tree_sum(self):
        page = os.sysconf("SC_PAGE_SIZE")
        mib = 1024 * 1024
        with tempfile.TemporaryDirectory() as tmp:
            proc = Path(tmp)
            # pid 30(root,30000p) → 31(20000p) → 32(10000p)；33 是旁支(999p)
            stats = {
                30: (0, 30000, "30 (init) S 0 0 0 0 -1 0"),   # ppid=0
                31: (30, 20000, "31 (gradle with space) S 30 0 0 0 -1 0"),
                32: (31, 10000, "32 (java) S 31 0 0 0 -1 0"),
                33: (1, 999, "33 (unrelated) S 1 0 0 0 -1 0"),  # 树外旁支
            }
            for pid, (ppid, rss_pages, stat_line) in stats.items():
                d = proc / str(pid)
                d.mkdir()
                (d / "stat").write_text(stat_line)
                (d / "statm").write_text(f"77 {rss_pages} 0 0 0 0 0\n")
            # pid 34 只有 stat 没有 statm（读取中途死亡）→ 跳过不炸
            d34 = proc / "34"
            d34.mkdir()
            (d34 / "stat").write_text("34 (gone) S 32 0 0 0 -1 0")
            want = (30000 + 20000 + 10000) * page // mib   # 不含 33/34
            self.assertEqual(gate.tree_rss_mib(30, proc_dir=str(proc)), want)
            self.assertEqual(gate.tree_rss_mib(32, proc_dir=str(proc)),
                             10000 * page // mib)          # 子树从 32 起
            self.assertEqual(gate.tree_rss_mib(99999, proc_dir=str(proc)), 0)


class RunGatedV3Test(unittest.TestCase):
    """v3 runner 语义：台账运行后才写、CI 零门槛透传。"""

    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.tmp = Path(self.dir.name)
        self.ledger = self.tmp / "ledger.json"
        self._mem_env = os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "999999"
        self._ci = os.environ.pop("GITHUB_ACTIONS", None)

    def tearDown(self):
        self.dir.cleanup()
        if self._mem_env is None:
            os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        else:
            os.environ["GT6_GATE_MEM_LIMIT_MIB"] = self._mem_env
        if self._ci is not None:
            os.environ["GITHUB_ACTIONS"] = self._ci

    def test_ledger_written_after_run_not_before(self):
        self.assertFalse(self.ledger.exists())            # 运行前无台账
        rc = gate.run_gated(["true"], poll=0.01, slot_dir=self.tmp / "slots",
                            ledger_path=self.ledger)
        self.assertEqual(rc, 0)
        entry = gate.load_ledger(self.ledger)["other"]    # 运行后按 class 落账
        self.assertIn("last_peak", entry)
        self.assertGreaterEqual(entry["last_peak"], 0)
        self.assertLess(abs(entry["updated"] - time.time()), 60)

    def test_ledger_estimate_wired_into_admission(self):
        # 接线钉：台账里 compile 的估算值原样传给 wait_memory 的 estimate_mib
        # （排队/放行的数学本身由 WaitMemoryPredictTest 覆盖）
        gate.record_observation("compile", 26000, self.ledger)
        captured = []

        def fake_wait(**kw):
            captured.append(kw.get("estimate_mib"))
            return 0, 0.0

        real_wait = gate.wait_memory
        gate.wait_memory = fake_wait
        try:
            rc = gate.run_gated(["true"], poll=0.01, slot_dir=self.tmp / "slots",
                                ledger_path=self.ledger, cls="compile")
        finally:
            gate.wait_memory = real_wait
        self.assertEqual(rc, 0)
        self.assertEqual(captured, [26000])

    def test_ci_env_bypasses_all_gates(self):
        os.environ["GITHUB_ACTIONS"] = "true"
        rc = gate.run_gated(["sh", "-c", "exit 5"], poll=0.01,
                            slot_dir=self.tmp / "slots",
                            ledger_path=self.ledger)
        self.assertEqual(rc, 5)
        self.assertFalse(self.ledger.exists())            # 不采样不落账
        self.assertFalse((self.tmp / "slots").exists())   # 不占槽


class DryRunTest(unittest.TestCase):
    """v3 --dry-run 三场景：冷启动 ADMIT / 台账超限 QUEUE / full+coder REJECT。"""

    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.tmp = Path(self.dir.name)
        self.ledger = self.tmp / "ledger.json"

    def tearDown(self):
        self.dir.cleanup()

    def report(self, cmd, **kw):
        lines = []
        word = gate.dry_run(cmd, ledger_path=self.ledger, log=lines.append, **kw)
        return word, lines

    def test_cold_start_admits(self):
        word, lines = self.report(["./gradlew", "compileJava"],
                                  read_used=lambda: 8000, limit_mib=30720)
        self.assertEqual(word, "ADMIT")
        body = "\n".join(lines)
        self.assertIn("class=compile", body)
        self.assertIn("cold default", body)
        self.assertIn("8000 MiB", body)          # 当前占用
        self.assertIn("4000 MiB", body)          # 冷启动估算
        self.assertIn("12000 MiB", body)         # 预测 = 8000+4000

    def test_ledger_over_limit_queues(self):
        gate.record_observation("full-test", 26000, self.ledger)
        word, lines = self.report(["./gradlew", ":mdk:cleanTest"],
                                  role="review", read_used=lambda: 8000,
                                  limit_mib=30720)
        self.assertEqual(word, "QUEUE")
        body = "\n".join(lines)
        self.assertIn("ledger", body)            # 来源=台账非冷启动
        self.assertIn("26000 MiB", body)
        self.assertIn("34000 MiB", body)         # 8000+26000 > 30720
        self.assertIn("QUEUE", body)

    def test_full_coder_rejects_before_prediction(self):
        word, lines = self.report(["./gradlew", ":mdk:cleanTest"],
                                  read_used=lambda: 100, limit_mib=999999)
        self.assertEqual(word, "REJECT")
        body = "\n".join(lines)
        self.assertIn("exit 2", body)
        self.assertIn("review-seat", body)

    def test_main_dry_run_flag_and_class_override(self):
        old = gate.LEDGER_PATH
        gate.LEDGER_PATH = self.ledger
        try:
            rc = gate.main(["run", "--dry-run", "--class", "full-test",
                            "--", "./gradlew", "compileJava"])
            self.assertEqual(rc, 0)
            # --class 显式覆盖推断（compileJava 本应是 compile）
            # run 子命令形态 + 旧形态（无 run 前缀）等价：
            rc2 = gate.main(["--dry-run", "--class", "full-test",
                             "--", "./gradlew", "compileJava"])
            self.assertEqual(rc2, 0)
        finally:
            gate.LEDGER_PATH = old

    def test_main_bad_class_rejected(self):
        with self.assertRaises(SystemExit):
            gate.main(["run", "--class", "bogus", "--", "true"])


class GuardHookTest(unittest.TestCase):
    """防旁路 PreToolUse hook：带/不带 gate 前缀判定表 + CI 放行。

    钩子脚本随本卡第二提交落地（.githooks/guard-heavy-ops.sh）；脚本缺席
    （分叉/旧检出）时整类跳过，不影响门禁本体测试。
    """

    @classmethod
    def setUpClass(cls):
        cls.script = (Path(__file__).resolve().parents[1]
                      / ".githooks" / "guard-heavy-ops.sh")
        if not cls.script.exists():
            raise unittest.SkipTest(f"hook script absent: {cls.script}")
        cls.cwd = str(Path(__file__).resolve().parents[1])   # worktree 根

    def hook(self, command, cwd=None, extra_env=None, stdin=None):
        env = dict(os.environ)
        env.pop("GITHUB_ACTIONS", None)
        if extra_env:
            env.update(extra_env)
        payload = stdin if stdin is not None else json.dumps(
            {"tool_input": {"command": command}})
        proc = subprocess.run([str(self.script)], input=payload, cwd=cwd or self.cwd,
                              capture_output=True, text=True, env=env, timeout=10)
        return proc

    def test_bare_gradle_denied_with_guidance(self):
        proc = self.hook("./gradlew :mdk:test")
        self.assertEqual(proc.returncode, 0)
        out = json.loads(proc.stdout)
        self.assertEqual(out["hookSpecificOutput"]["permissionDecision"], "deny")
        self.assertIn("gt6testgate.py run", out["hookSpecificOutput"]
                      ["permissionDecisionReason"])

    def test_gradle_word_without_gate_prefix_denied(self):
        # 子串语义（含 echo/grep 提到 gradle）——fail-closed 有意，判定表钉死
        out = json.loads(self.hook("echo gradle").stdout)
        self.assertEqual(out["hookSpecificOutput"]["permissionDecision"], "deny")

    def test_gated_invocation_allowed(self):
        proc = self.hook("python3 tools/gt6testgate.py run -- ./gradlew :mdk:test")
        self.assertEqual(proc.returncode, 0)
        self.assertEqual(proc.stdout, "")

    def test_ci_env_passes_through(self):
        proc = self.hook("./gradlew :mdk:test", extra_env={"GITHUB_ACTIONS": "true"})
        self.assertEqual(proc.returncode, 0)
        self.assertEqual(proc.stdout, "")

    def test_non_gradle_allowed(self):
        self.assertEqual(self.hook("python3 -m unittest discover tools").stdout, "")

    def test_invalid_json_allowed(self):
        proc = self.hook(None, stdin="garbage{{{")
        self.assertEqual(proc.returncode, 0)
        self.assertEqual(proc.stdout, "")

    def test_out_of_scope_cwd_allowed(self):
        proc = self.hook("./gradlew test", cwd=tempfile.mkdtemp())
        self.assertEqual(proc.returncode, 0)
        self.assertEqual(proc.stdout, "")


if __name__ == "__main__":
    unittest.main(verbosity=2)
