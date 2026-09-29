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
  * v3c 共享 slice 硬顶：slice_wrap/slice_bootstrap 纯函数+幂等（重复调用
    属性不变）+--cap/--swap 透传；runner 两分支端到端（fake systemd-run
    包装退出码透传+采样照常 / bootstrap 失败降级直接 exec+stderr 警告，
    异常同降级）；GT6_GATE_SLICE_LIVE=1 才跑的真 systemd 探针（slow）
  * v3.1 残留清剿（ops-testgate-v3p1-reap，三层）：--no-daemon 注入判定表
    （gradlew 追加/幂等/显式 --daemon 不动/非 gradle 不动/basename 精确匹配/
    --keep-daemon 与 env GT6_GATE_KEEP_DAEMON 逃生/run_gated 端到端注入）；
    确定性 unit 名 gt6gate-run-<pid>-<ts>.scope+wrap 携带；杀序 TERM→宽限→
    KILL（全清只 TERM/无视 TERM 升 KILL/KILL 后仍活返回 False/systemctl
    缺席警告不抛）；unit_pids 读 ControlGroup→cgroup.procs（目录消失=空）；
    reap 三态（空 leader 残留→收/活跃 MainPID≠0→跳过/空壳→跳过）+
    --dry-run 只列+systemctl 缺席优雅；runner 接线（unit 形态/台账先写后杀/
    杀完才释放槽/降级分支不杀）
  * v3.2 信封内准入（取代 v3a 系统侧 30G 公式）+ v3.5 外压护栏退役：
    cgroup_usage_mib 读 memory.current（字节→MiB，缺席=0 即刻放行）；
    outside_limit 默认 None（护栏退役；env GT6_GATE_MEM_LIMIT_MIB 合法
    整数=ops opt-in 绝对阈，坏值仍禁用）；wait_admission 只由信封判定
    （高基线 used=15000 立即放行=v3.5 回归钉，gate-admit 行保留 outside
    信息项；env opt-in 恢复排队；envelope 超帽阻塞钉不变）；run_gated
    接线（estimate→wait_admission）；dry_run 五行报告（ADMIT/
    QUEUE-envelope/REJECT，outside 仅诊断信息）
  * v3.3 每任务预算+脚本看门狗（内核不强制 memory.max 的本机，脚本即
    执行者）：TASK_CAP_MIB 分级表+--task-cap 覆盖+wrap 携带 MemoryMax；
    watchdog_tick 判定表（自预算超→杀己/聚合超帽→最大者优先逐杀至帽内/
    平静不杀/usage≤0 跳过/slice 目录缺席不杀）；run_gated rc 语义
    （杀己→BUDGET_EXIT 97 专码≠普通失败/杀兄弟→自身透传继续/未包装不
    看门狗）；GT6_GATE_SLICE_LIVE=1 追加 malloc 膨胀真杀探针（slow）
"""

import contextlib
import fcntl
import io
import json
import os
import shutil
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

    def test_total_reader(self):
        with tempfile.NamedTemporaryFile("w", suffix=".meminfo", delete=False) as fh:
            fh.write(mock_meminfo(32768, 4096))
            path = fh.name
        try:
            self.assertEqual(gate.mem_total_mib(path), 32768)
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
        # v3.2 准入按信封（slice+est≤cap）与外压（used-slice≤limit）判定：
        # 外压阈值 env 钉高 + slice 读数密封 0——本类只测退出码透传/tag 缺省，
        # 不排队真内存；v3c slice 包装钉关（不碰 systemd）
        self._mem_env = os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "999999"
        self._slice_env = os.environ.pop(gate.SLICE_ENV, None)
        os.environ[gate.SLICE_ENV] = "0"
        self._slice_read = gate.slice_usage_mib
        gate.slice_usage_mib = lambda *a, **k: 0

    def tearDown(self):
        if self._mem_env is None:
            os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        else:
            os.environ["GT6_GATE_MEM_LIMIT_MIB"] = self._mem_env
        if self._slice_env is None:
            os.environ.pop(gate.SLICE_ENV, None)
        else:
            os.environ[gate.SLICE_ENV] = self._slice_env
        gate.slice_usage_mib = self._slice_read

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
        # 内存闸钉到必过：外压阈值 env 钉高 + slice 读数密封 0（v3.2 信封
        # 判据），本类只测角色/锁语义，不排队真 /proc/meminfo
        self._mem_env = os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "999999"
        self._role_env = os.environ.pop(gate.ROLE_ENV, None)  # 默认 coder 确定
        self._slice_env = os.environ.pop(gate.SLICE_ENV, None)  # 不碰 systemd
        os.environ[gate.SLICE_ENV] = "0"
        self._slice_read = gate.slice_usage_mib
        gate.slice_usage_mib = lambda *a, **k: 0
        self._stub_n = 0

    def tearDown(self):
        self.dir.cleanup()
        if self._mem_env is None:
            os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        else:
            os.environ["GT6_GATE_MEM_LIMIT_MIB"] = self._mem_env
        if self._role_env is not None:
            os.environ[gate.ROLE_ENV] = self._role_env
        if self._slice_env is None:
            os.environ.pop(gate.SLICE_ENV, None)
        else:
            os.environ[gate.SLICE_ENV] = self._slice_env
        gate.slice_usage_mib = self._slice_read

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
    """v3 遗留反应闸（estimate_mib 参数）——v3.2 起仅 gt6server boot 路径
    使用（签名不变约束）；门禁 runner 的准入由 EnvelopeAdmissionTest 覆盖。"""

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
        self._slice_env = os.environ.pop(gate.SLICE_ENV, None)
        os.environ[gate.SLICE_ENV] = "0"
        self._slice_read = gate.slice_usage_mib
        gate.slice_usage_mib = lambda *a, **k: 0
        self._ci = os.environ.pop("GITHUB_ACTIONS", None)

    def tearDown(self):
        self.dir.cleanup()
        if self._mem_env is None:
            os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        else:
            os.environ["GT6_GATE_MEM_LIMIT_MIB"] = self._mem_env
        if self._slice_env is None:
            os.environ.pop(gate.SLICE_ENV, None)
        else:
            os.environ[gate.SLICE_ENV] = self._slice_env
        gate.slice_usage_mib = self._slice_read
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
        # 接线钉：台账里 compile 的估算值原样传给 wait_admission（v3.2 信封
        # 判据用同一个 estimate；排队/放行的数学由 EnvelopeAdmissionTest 覆盖）
        gate.record_observation("compile", 26000, self.ledger)
        captured = []

        def fake_admit(est, **kw):
            captured.append(est)
            return None

        real_admit = gate.wait_admission
        gate.wait_admission = fake_admit
        try:
            rc = gate.run_gated(["true"], poll=0.01, slot_dir=self.tmp / "slots",
                                ledger_path=self.ledger, cls="compile")
        finally:
            gate.wait_admission = real_admit
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


class SliceWrapTest(unittest.TestCase):
    """v3c 共享 slice 硬顶：命令构造纯函数 + bootstrap 幂等 + CLI 透传。"""

    def test_wrap_shape(self):
        # --scope 前台运行：stdio/退出码透传保留（实现钉：argv 无 shell）
        self.assertEqual(
            gate.slice_wrap(["./gradlew", ":mdk:test"]),
            ["systemd-run", "--user", "--scope", "-p", "Slice=gt6gate.slice",
             "--", "./gradlew", ":mdk:test"])

    def test_bootstrap_idempotent_argv(self):
        calls = []

        def fake_run(argv):
            calls.append(argv)
            return 0

        self.assertTrue(gate.slice_bootstrap(25, 4, run=fake_run))
        self.assertTrue(gate.slice_bootstrap(25, 4, run=fake_run))
        self.assertEqual(len(calls), 2)
        self.assertEqual(calls[0], calls[1])      # 重复调用属性不变
        self.assertEqual(calls[0],
                         ["systemctl", "--user", "set-property",
                          "gt6gate.slice", "MemoryMax=25G",
                          "MemorySwapMax=4G", "--runtime"])
        # --cap/--swap 可调数值进属性
        calls.clear()
        gate.slice_bootstrap(30, 5, run=fake_run)
        self.assertIn("MemoryMax=30G", calls[0])
        self.assertIn("MemorySwapMax=5G", calls[0])

    def test_bootstrap_failure_is_false(self):
        self.assertFalse(gate.slice_bootstrap(run=lambda argv: 1))

    def test_slice_enabled_env(self):
        self.assertTrue(gate.slice_enabled({}))            # 缺省=开（自动降级兜底）
        self.assertTrue(gate.slice_enabled({gate.SLICE_ENV: "1"}))
        self.assertFalse(gate.slice_enabled({gate.SLICE_ENV: "0"}))

    def test_cli_cap_swap_plumbed(self):
        old, kw = gate.run_gated, {}

        def fake(cmd, **k):
            kw.update(k)
            return 0

        gate.run_gated = fake
        try:
            gate.main(["run", "--cap", "30", "--swap", "5", "--", "true"])
        finally:
            gate.run_gated = old
        self.assertEqual((kw["cap_gib"], kw["swap_gib"]), (30, 5))


class SliceRunTest(unittest.TestCase):
    """v3c runner 两分支端到端：fake systemd-run 包装 / bootstrap 失败降级。"""

    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.tmp = Path(self.dir.name)
        self._mem_env = os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "999999"
        os.environ[gate.SLICE_ENV] = "1"          # 本类强制走 slice 分支
        self._slice_read = gate.slice_usage_mib
        gate.slice_usage_mib = lambda *a, **k: 0  # 密封信封读数
        self._ci = os.environ.pop("GITHUB_ACTIONS", None)

    def tearDown(self):
        self.dir.cleanup()
        if self._mem_env is None:
            os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        else:
            os.environ["GT6_GATE_MEM_LIMIT_MIB"] = self._mem_env
        gate.slice_usage_mib = self._slice_read
        if self._ci is not None:
            os.environ["GITHUB_ACTIONS"] = self._ci

    def test_wrapped_run_via_fake_systemd_run(self):
        bin_dir = self.tmp / "bin"
        bin_dir.mkdir()
        stub = bin_dir / "systemd-run"
        argv_file = self.tmp / "sysrun-argv"
        # 记录收到的 flag（钉 --unit=gt6gate-run-*），跳到 "--" 后透传退出码
        stub.write_text('#!/bin/sh\n'
                        f'printf \'%s\\n\' "$@" > {argv_file}\n'
                        'while [ $# -gt 0 ] && [ "$1" != "--" ]; do shift; done\n'
                        '[ $# -gt 0 ] || exit 42\n'
                        'shift\nexec "$@"\n')
        stub.chmod(0o755)
        marker = self.tmp / "ran"
        old_path = os.environ["PATH"]
        os.environ["PATH"] = f"{bin_dir}:{old_path}"
        # v3.1 收尾杀接线：unit 形态 / 台账先写 / 槽还握着（杀完才释放）
        stops = []

        def spy_stop(unit, log=None):
            stops.append((unit, (self.tmp / "ledger.json").exists(),
                          list((self.tmp / "slots").iterdir())))

        try:
            rc = gate.run_gated(
                ["sh", "-c", f"touch {marker}; exit 5"], poll=0.01,
                slot_dir=self.tmp / "slots",
                ledger_path=self.tmp / "ledger.json",
                slice_bootstrap_fn=lambda c, s: True, stop_fn=spy_stop,
                watchdog_fn=lambda u, b, c, **k: (0, []))
        finally:
            os.environ["PATH"] = old_path
        self.assertEqual(rc, 5)                   # 退出码经 scope 透传
        self.assertTrue(marker.exists())          # 真跑在包装层内
        self.assertEqual(len(stops), 1)           # 恰一次收尾杀
        unit, ledger_first, slots_held = stops[0]
        self.assertRegex(unit, r"^gt6gate-run-\d+-\d+\.scope$")  # 可寻址全名
        self.assertTrue(ledger_first)             # 台账写完才杀
        self.assertTrue(slots_held)               # 杀完才释放并发槽
        self.assertFalse(list((self.tmp / "slots").iterdir()))  # 返回前已释放
        argv = argv_file.read_text().splitlines()
        self.assertIn(f"--unit={unit}", argv)     # systemd-run 真收到 unit
        entry = gate.load_ledger(self.tmp / "ledger.json")["other"]
        self.assertIn("last_peak", entry)         # 树采样照常（systemd-run→sh）

    def test_degrades_to_direct_exec_on_bootstrap_failure(self):
        marker = self.tmp / "ran"
        err = io.StringIO()
        stops = []
        with contextlib.redirect_stderr(err):
            rc = gate.run_gated(["sh", "-c", f"touch {marker}"], poll=0.01,
                                slot_dir=self.tmp / "slots",
                                ledger_path=self.tmp / "ledger.json",
                                slice_bootstrap_fn=lambda c, s: False,
                                stop_fn=lambda u, log=None: stops.append(u))
        self.assertEqual(rc, 0)
        self.assertTrue(marker.exists())          # 直接 exec，可用性优先
        self.assertIn("UNCAPPED", err.getvalue())  # stderr 一行降级警告
        self.assertEqual(stops, [])               # 未包装 → 无可收之尸

    def test_bootstrap_exception_degrades_too(self):
        marker = self.tmp / "ran"
        err = io.StringIO()

        def boom(c, s):
            raise FileNotFoundError("systemctl missing")

        with contextlib.redirect_stderr(err):
            rc = gate.run_gated(["true"], poll=0.01,
                                slot_dir=self.tmp / "slots",
                                ledger_path=self.tmp / "ledger.json",
                                slice_bootstrap_fn=boom)
        self.assertEqual(rc, 0)
        self.assertIn("UNCAPPED", err.getvalue())


class NoDaemonTest(unittest.TestCase):
    """v3.1 根治层：--no-daemon 注入判定表。"""

    def test_gradle_gets_flag_appended(self):
        self.assertEqual(gate.inject_no_daemon(["./gradlew", ":mdk:test"]),
                         ["./gradlew", ":mdk:test", "--no-daemon"])

    def test_idempotent_when_flag_present(self):
        cmd = ["./gradlew", ":test", "--no-daemon"]
        self.assertEqual(gate.inject_no_daemon(cmd), cmd)

    def test_explicit_daemon_wins_no_contradiction(self):
        cmd = ["./gradlew", "--daemon", ":test"]
        self.assertEqual(gate.inject_no_daemon(cmd), cmd)

    def test_non_gradle_untouched(self):
        for cmd in (["python3", "tools/rcon/sweep.py", "--group", "x"],
                    ["sh", "-c", "./gradlew :test"],   # 包一层不识别（文档钉）
                    ["true"]):
            self.assertEqual(gate.inject_no_daemon(cmd), cmd, cmd)

    def test_gradle_basename_exact_anywhere_in_argv(self):
        self.assertEqual(gate.inject_no_daemon(["/opt/g/bin/gradle", "build"]),
                         ["/opt/g/bin/gradle", "build", "--no-daemon"])
        # 子串不算：gradlew.sh 不是 gradlew（basename 精确匹配）
        self.assertEqual(gate.inject_no_daemon(["./gradlew.sh", "build"]),
                         ["./gradlew.sh", "build"])

    def test_keep_daemon_cli_escape(self):
        cmd = ["./gradlew", ":test"]
        self.assertEqual(gate.inject_no_daemon(cmd, keep_daemon=True), cmd)

    def test_keep_daemon_env_escape(self):
        old = os.environ.get(gate.KEEP_DAEMON_ENV)
        cmd = ["./gradlew", ":test"]
        try:
            os.environ[gate.KEEP_DAEMON_ENV] = "1"
            self.assertEqual(gate.inject_no_daemon(cmd), cmd)
            os.environ[gate.KEEP_DAEMON_ENV] = "0"     # 显式 0 = 照常注入
            self.assertEqual(gate.inject_no_daemon(cmd), cmd + ["--no-daemon"])
        finally:
            if old is None:
                os.environ.pop(gate.KEEP_DAEMON_ENV, None)
            else:
                os.environ[gate.KEEP_DAEMON_ENV] = old

    def test_run_gated_injects_into_child_argv(self):
        # 端到端：名为 gradlew 的 stub 把 "$@" 落盘，验证子进程真收到旗标；
        # keep_daemon=True 时原样透传。用 --tests 走 filtered 不触角色闸。
        d = Path(tempfile.mkdtemp())
        marker = d / "argv"
        stub = d / "gradlew"
        stub.write_text(f'#!/bin/sh\nprintf \'%s\\n\' "$@" > {marker}\n')
        stub.chmod(0o755)
        old = gate.LEDGER_PATH
        gate.LEDGER_PATH = d / "ledger.json"
        mem = os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "999999"
        sl = os.environ.pop(gate.SLICE_ENV, None)
        os.environ[gate.SLICE_ENV] = "0"
        sread = gate.slice_usage_mib
        gate.slice_usage_mib = lambda *a, **k: 0
        try:
            rc = gate.run_gated([str(stub), ":mdk:test", "--tests", "F"],
                                poll=0.01, slot_dir=d / "slots")
            self.assertEqual(rc, 0)
            self.assertEqual(marker.read_text().split(),
                             [":mdk:test", "--tests", "F", "--no-daemon"])
            rc = gate.run_gated([str(stub), ":mdk:test", "--tests", "F"],
                                poll=0.01, slot_dir=d / "slots",
                                keep_daemon=True)
            self.assertEqual(rc, 0)
            self.assertEqual(marker.read_text().split(),
                             [":mdk:test", "--tests", "F"])
        finally:
            gate.LEDGER_PATH = old
            gate.slice_usage_mib = sread
            if mem is not None:
                os.environ["GT6_GATE_MEM_LIMIT_MIB"] = mem
            else:
                os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
            if sl is not None:
                os.environ[gate.SLICE_ENV] = sl
            else:
                os.environ.pop(gate.SLICE_ENV, None)


class UnitStopTest(unittest.TestCase):
    """v3.1 兜底层：确定性 unit 名 + 杀序 TERM→宽限→KILL。"""

    def test_unit_name_shape_and_uniqueness(self):
        u1, u2 = gate.unit_name(), gate.unit_name()
        self.assertRegex(u1, r"^gt6gate-run-\d+-\d+\.scope$")
        self.assertNotEqual(u1, u2)

    def test_wrap_includes_unit(self):
        self.assertEqual(
            gate.slice_wrap(["./gradlew", "build"], unit="gt6gate-run-1-2"),
            ["systemd-run", "--user", "--scope", "--unit=gt6gate-run-1-2",
             "-p", "Slice=gt6gate.slice", "--", "./gradlew", "build"])
        # unit=None（旧形态）不携带 --unit
        self.assertEqual(gate.slice_wrap(["c"]),
                         ["systemd-run", "--user", "--scope",
                          "-p", "Slice=gt6gate.slice", "--", "c"])

    def setUp(self):
        self.calls = []

    def rec(self, argv):
        self.calls.append(argv)
        return subprocess.CompletedProcess(argv, 0, "", "")

    def stop(self, pids_seq, **kw):
        """pids_seq：每次 pids_fn 查询依序返回的组内进程快照。"""
        pids = iter(pids_seq)
        kw.setdefault("grace", 0.01)
        return gate.stop_unit("u.scope", run=self.rec,
                              pids_fn=lambda u, r: next(pids), **kw)

    def test_procs_die_on_term_single_call(self):
        # TERM 后宽限内清空：只有一记 kill 调用（SIGTERM）
        self.assertTrue(self.stop([[], []]))
        self.assertEqual(self.calls, [
            ["systemctl", "--user", "kill", "--signal=SIGTERM", "u.scope"]])

    def test_kill_order_term_grace_sigkill(self):
        # 进程无视 TERM：宽限耗尽 → 升 SIGKILL（杀序钉死）。
        # pids 快照序列=循环首轮/循环后终查/KILL 后复查（time 短路在前，
        # grace=0.01 时循环恰跑一轮真 sleep）
        self.assertTrue(self.stop([[7], [7], []]))
        self.assertEqual(self.calls, [
            ["systemctl", "--user", "kill", "--signal=SIGTERM", "u.scope"],
            ["systemctl", "--user", "kill", "--signal=SIGKILL", "u.scope"]])

    def test_kill_failure_returns_false(self):
        # KILL 后仍有残留：如实报 False（不假装清干净）
        self.assertFalse(self.stop([[7], [7], [7], [7]]))
        self.assertEqual(self.calls[1],
                         ["systemctl", "--user", "kill",
                          "--signal=SIGKILL", "u.scope"])

    def test_systemctl_failure_warns_not_raises(self):
        lines = []
        self.assertFalse(gate.stop_unit(
            "u.scope", run=self.boom, pids_fn=lambda u, r: [], log=lines.append))
        self.assertTrue(any("reap of u.scope failed" in l for l in lines))

    def boom(self, argv):
        raise FileNotFoundError("systemctl missing")


class UnitPidsTest(unittest.TestCase):
    """unit_pids：ControlGroup 属性 → cgroup.procs 读取；缺席=空。"""

    def test_reads_control_group_procs(self):
        calls = []

        def fake_run(argv):
            calls.append(argv)
            return subprocess.CompletedProcess(
                argv, 0, stdout="/user.slice/u/gt6gate.slice/u.scope\n")

        with tempfile.TemporaryDirectory() as tmp:
            cg = Path(tmp) / "user.slice/u/gt6gate.slice/u.scope"
            cg.mkdir(parents=True)
            (cg / "cgroup.procs").write_text("101\n102\n")
            self.assertEqual(gate.unit_pids("u.scope", fake_run,
                                            cgroup_root=tmp), [101, 102])
        self.assertEqual(calls, [["systemctl", "--user", "show", "u.scope",
                                  "-p", "ControlGroup", "--value"]])

    def test_missing_or_failed_means_empty(self):
        # show 失败 → []；ControlGroup 空（unit 已释放）→ []
        fail = lambda a: subprocess.CompletedProcess(a, 1, "", "err")
        self.assertEqual(gate.unit_pids("u.scope", fail), [])
        self.assertEqual(gate.unit_pids(
            "u.scope", lambda a: subprocess.CompletedProcess(a, 0, "", "")), [])
        # show 成功但 cgroup 目录已清 → []（systemd GC 完毕）
        gone = lambda a: subprocess.CompletedProcess(a, 0, stdout="/gone/u.scope\n")
        self.assertEqual(gate.unit_pids("u.scope", gone,
                                        cgroup_root=tempfile.mkdtemp()), [])


class ReapTest(unittest.TestCase):
    """--reap 选择逻辑三态：空 leader 残留→收 / 活跃→跳过 / 空壳→跳过。"""

    def fake_systemctl(self, units, slices, main_pids):
        """list-units + show -p Slice/MainPID 分发桩（show 值按 unit 查表）。"""
        calls = []

        def run(argv):
            calls.append(argv)
            if "list-units" in argv:
                body = "".join(f"{u} loaded active running -\n" for u in units)
                return subprocess.CompletedProcess(argv, 0, body, "")
            prop = argv[argv.index("-p") + 1]
            unit = argv[3]
            table = {"Slice": slices.get(unit, "other.slice"),
                     "MainPID": main_pids.get(unit, "")}
            return subprocess.CompletedProcess(argv, 0, f"{table[prop]}\n", "")

        return run, calls

    def reap(self, dry_run=False):
        run, calls = self.fake_systemctl(
            units=["gt6gate-run-1-1.scope", "gt6gate-run-2-2.scope",
                   "gt6gate-run-3-3.scope", "unrelated.scope"],
            slices={"gt6gate-run-1-1.scope": "gt6gate.slice",
                    "gt6gate-run-2-2.scope": "gt6gate.slice",
                    "gt6gate-run-3-3.scope": "gt6gate.slice"},
            main_pids={"gt6gate-run-1-1.scope": "",      # leader 已退（实测空串）
                       "gt6gate-run-2-2.scope": "4242",  # 活跃
                       "gt6gate-run-3-3.scope": "0"})    # leader 退且无残留
        pids = {"gt6gate-run-1-1.scope": [501, 502]}     # 唯一藏尸者
        stopped, lines = [], []
        got = gate.reap_scopes(run=run, pids_fn=lambda u, r: pids.get(u, []),
                               stop_fn=lambda u, **k: stopped.append(u) or True,
                               dry_run=dry_run, log=lines.append)
        return got, stopped, lines

    def test_dry_run_lists_without_stopping(self):
        got, stopped, lines = self.reap(dry_run=True)
        self.assertEqual(got, ["gt6gate-run-1-1.scope"])
        self.assertEqual(stopped, [])                 # 只列不杀
        self.assertTrue(any("would reap" in l for l in lines))

    def test_reaps_only_leader_gone_with_leftovers(self):
        got, stopped, lines = self.reap()
        # 只收 1-1：2-2 活跃（MainPID≠0）不碰、3-3 空壳跳过、域外不问
        self.assertEqual(got, ["gt6gate-run-1-1.scope"])
        self.assertEqual(stopped, ["gt6gate-run-1-1.scope"])
        self.assertTrue(any("reaping gt6gate-run-1-1.scope" in l
                            for l in lines))

    def test_unavailable_when_systemctl_missing(self):
        lines = []
        got = gate.reap_scopes(run=self.boom, log=lines.append,
                               pids_fn=lambda u, r: [],
                               stop_fn=lambda u, **k: True)
        self.assertEqual(got, [])
        self.assertTrue(any("reap unavailable" in l for l in lines))

    def boom(self, argv):
        raise FileNotFoundError("systemctl missing")

    def test_main_reap_subcommand_dispatch(self):
        old = gate.reap_scopes
        seen = {}
        try:
            gate.reap_scopes = lambda **kw: seen.update(kw) or []
            self.assertEqual(gate.main(["reap", "--dry-run"]), 0)
            self.assertTrue(seen.get("dry_run"))
            self.assertEqual(gate.main(["reap"]), 0)
            self.assertFalse(seen.get("dry_run"))
        finally:
            gate.reap_scopes = old


class SliceUsageTest(unittest.TestCase):
    """v3.2/v3.3 cgroup 读数：memory.current 字节→MiB、缺席=0、子层枚举。"""

    def make_tree(self, children):
        """user@<uid>.service 下的 gate.slice 假 cgroup 树；值=MiB。"""
        root = tempfile.mkdtemp()
        uid = os.getuid()
        base = Path(root) / "user.slice" / f"user-{uid}.slice" \
            / f"user@{uid}.service" / "gt6gate.slice"
        for name, mib in children.items():
            d = base / name
            d.mkdir(parents=True)
            (d / "memory.current").write_text(f"{mib * 1024 * 1024}\n")
        return root

    def test_usage_reads_bytes_as_mib(self):
        # ""=slice 根自身（rel 直拼 base），1536MiB 以字节落盘
        root = self.make_tree({"": 1536})
        self.assertEqual(gate.cgroup_usage_mib("gt6gate.slice",
                                               cgroup_root=root), 1536)
        self.assertEqual(gate.slice_usage_mib(cgroup_root=root), 1536)

    def test_absent_reads_zero(self):
        root = self.make_tree({})                     # slice 目录本身不在
        self.assertEqual(gate.slice_usage_mib(cgroup_root=root), 0)
        root2 = self.make_tree({"gt6gate.slice": 10})
        self.assertEqual(gate.cgroup_usage_mib("gt6gate.slice/nope.scope",
                                               cgroup_root=root2), 0)

    def test_children_enumeration(self):
        root = self.make_tree({"a.scope": 100, "b.scope": 20})
        (Path(root) / "unrelated").mkdir()            # slice 树外的旁支
        got = gate.slice_children_usage(cgroup_root=root)
        self.assertEqual(sorted(got), [("a.scope", 100), ("b.scope", 20)])

    def test_children_absent_slice_empty_list(self):
        self.assertEqual(gate.slice_children_usage(
            cgroup_root=tempfile.mkdtemp()), [])


class EffectiveUsageTest(unittest.TestCase):
    """v3.4 有效占用：memory.current − (memory.stat file+slab_reclaimable)；
    解析失败保守不减（原值直读）+stderr 只注记一次；下限钳 0。"""

    def setUp(self):
        self._note = gate._RECLAIM_NOTE_DONE
        gate._RECLAIM_NOTE_DONE = False
        self.err = io.StringIO()

    def tearDown(self):
        gate._RECLAIM_NOTE_DONE = self._note

    def make_tree(self, current_mib, stat_lines=None):
        """单 scope 假树：memory.current 必写，memory.stat 可选。"""
        root = tempfile.mkdtemp()
        uid = os.getuid()
        d = Path(root) / "user.slice" / f"user-{uid}.slice" \
            / f"user@{uid}.service" / "gt6gate.slice" / "t.scope"
        d.mkdir(parents=True)
        (d / "memory.current").write_text(f"{current_mib * 1024 * 1024}\n")
        if stat_lines is not None:
            (d / "memory.stat").write_text(
                "".join(f"{k} {v * 1024 * 1024}\n" for k, v in stat_lines))
        return root

    def test_effective_subtracts_file_and_slab_reclaimable(self):
        # 1000MiB current 里 700MiB file + 100MiB slab_reclaimable 都是
        # 可回收 page cache（gradle 文件 IO）——有效占用只有 200MiB。
        root = self.make_tree(1000, [("anon", 200), ("file", 700),
                                     ("slab_reclaimable", 100)])
        with contextlib.redirect_stderr(self.err):
            self.assertEqual(gate.cgroup_effective_usage_mib(
                "gt6gate.slice/t.scope", cgroup_root=root), 200)
            self.assertEqual(gate.slice_children_usage(cgroup_root=root),
                             [("t.scope", 200)])
        # slice 根自身也走有效占用：根目录自带 current+stat 时同式扣减。
        root2 = self.make_tree(800, [("file", 600), ("slab_reclaimable", 50)])
        d = Path(root2) / "user.slice" / f"user-{os.getuid()}.slice" \
            / f"user@{os.getuid()}.service" / "gt6gate.slice"
        (d / "memory.current").write_text(f"{800 * 1024 * 1024}\n")
        (d / "memory.stat").write_text(
            f"file {600 * 1024 * 1024}\nslab_reclaimable {50 * 1024 * 1024}\n")
        with contextlib.redirect_stderr(self.err):
            self.assertEqual(gate.slice_usage_mib(cgroup_root=root2), 150)
        self.assertEqual(self.err.getvalue(), "")   # 正常解析不注记

    def test_parse_failure_reads_raw_conservatively_and_notes_once(self):
        root = self.make_tree(1000)                 # 无 memory.stat
        with contextlib.redirect_stderr(self.err):
            self.assertEqual(gate.cgroup_effective_usage_mib(
                "gt6gate.slice/t.scope", cgroup_root=root), 1000)
            self.assertEqual(gate.cgroup_effective_usage_mib(
                "gt6gate.slice/t.scope", cgroup_root=root), 1000)
        self.assertEqual(self.err.getvalue().count("memory.stat unreadable"),
                         1, "conservative note fires exactly ONCE per process")

    def test_garbage_stat_also_conservative(self):
        root = self.make_tree(500, [("anon", "junk")])
        with contextlib.redirect_stderr(self.err):
            self.assertEqual(gate.cgroup_effective_usage_mib(
                "gt6gate.slice/t.scope", cgroup_root=root), 500)

    def test_floors_at_zero(self):
        # current 小于自身可回收缓存是合法态（回收在途）——钳 0 不出负数。
        root = self.make_tree(50, [("file", 70), ("slab_reclaimable", 30)])
        with contextlib.redirect_stderr(self.err):
            self.assertEqual(gate.cgroup_effective_usage_mib(
                "gt6gate.slice/t.scope", cgroup_root=root), 0)


class OutsideLimitTest(unittest.TestCase):
    """v3.5 外压护栏语义反转：默认退役（None）；env 合法整数=ops opt-in
    绝对阈；坏值仍走禁用路径。（旧 v3.2 公式钉随护栏一并退役）"""

    def setUp(self):
        self._env = os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)

    def tearDown(self):
        if self._env is None:
            os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        else:
            os.environ["GT6_GATE_MEM_LIMIT_MIB"] = self._env

    def test_retired_by_default(self):
        # 无 env → None（退役）。本机基线死锁（基线 15069 > 公式上限
        # 12451）后用户裁定「系统侧不管」，护栏默认关。
        self.assertIsNone(gate.outside_limit_mib())

    def test_env_opt_in_restores_guard_as_absolute_threshold(self):
        os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "8000"
        self.assertEqual(gate.outside_limit_mib(), 8000)

    def test_invalid_env_stays_disabled(self):
        os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "junk"
        self.assertIsNone(gate.outside_limit_mib())


class EnvelopeAdmissionTest(unittest.TestCase):
    """v3.2 信封判定 + v3.5 外压退役：准入只看 slice+est≤cap；outside
    沦为信息项（gate-admit 行）；env opt-in 才恢复外压排队。读数全注入，
    毫秒级。"""

    def setUp(self):
        fd, self.path = tempfile.mkstemp(suffix=".gate")
        os.close(fd)

    def tearDown(self):
        os.unlink(self.path)

    def journal(self):
        with open(self.path) as fh:
            return fh.read()

    def admit(self, readings, estimate=4000, cap_gib=25, poll=0.01, **kw):
        """readings: (slice, used) 二元组序列，每轮轮询消费一个。"""
        slices = iter([r[0] for r in readings])
        useds = iter([r[1] for r in readings])
        return gate.wait_admission(
            estimate, cap_gib=cap_gib, poll=poll, tag="t",
            log_file=self.path, read_slice=lambda: next(slices),
            read_used=lambda: next(useds),
            **kw)

    def test_empty_slice_admits_instantly(self):
        self.admit([(0, 8000)])
        self.assertNotIn("gate-queue", self.journal())
        self.assertIn("gate-admit", self.journal())

    def test_high_baseline_admits_without_guard(self):
        # v3.5 回归钉（旧行为=永久 queue）：默认无 env 时高非门禁基线
        # （嵌入服务+多会话≈15069MiB）不再阻塞——空 slice+est 6000 即刻放行。
        self.admit([(0, 15000)], estimate=6000)
        body = self.journal()
        self.assertNotIn("gate-queue", body)
        self.assertIn("gate-admit", body)
        # outside 沦为信息项：admit 行照记数字（诊断用）
        self.assertIn("outside=15000MiB", body)

    def test_envelope_over_cap_queues_then_admits_on_relief(self):
        # slice 10000 + est 20000 = 30000 > 25600 → 排队；回落后放行
        self.admit([(10000, 8000), (1000, 8000)], estimate=20000)
        body = self.journal()
        self.assertIn("gate-queue", body)
        self.assertIn("envelope slice=10000MiB est=20000MiB "
                      "predicted=30000MiB cap=25600MiB", body)
        self.assertIn("gate-admit", body)

    def test_boundary_at_cap_admits(self):
        # 信封整好压线（slice 20000 + est 5600 == 25600）→ 不排队
        self.admit([(20000, 8000)], estimate=5600)
        self.assertNotIn("gate-queue", self.journal())

    def test_env_opt_in_guard_queues_high_baseline(self):
        # env 钉绝对阈：外压 15000 > env 8000 → 排队（信封本身宽裕），
        # 理由行带 outside 数字；外压回落后放行
        old = os.environ.get("GT6_GATE_MEM_LIMIT_MIB")
        try:
            os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "8000"
            self.admit([(0, 15000), (0, 7000)], estimate=6000)
            body = self.journal()
            self.assertIn("outside used=15000MiB slice=0MiB "
                          "outside=15000MiB limit=8000MiB", body)
            self.assertIn("gate-admit", body)
        finally:
            if old is None:
                os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
            else:
                os.environ["GT6_GATE_MEM_LIMIT_MIB"] = old


class TaskCapTest(unittest.TestCase):
    """v3.3 每任务预算：分级表、wrap 携带 MemoryMax、CLI 覆盖。"""

    def test_task_cap_table_covers_classes(self):
        self.assertEqual(set(gate.TASK_CAP_MIB), set(gate.CLASSES))
        self.assertEqual(gate.TASK_CAP_MIB["full-test"], 12288)
        self.assertEqual(gate.TASK_CAP_MIB["filtered-test"], 8192)
        self.assertEqual(gate.TASK_CAP_MIB["compile"], 6144)
        self.assertEqual(gate.TASK_CAP_MIB["rundata"], 8192)

    def test_wrap_carries_memory_max(self):
        argv = gate.slice_wrap(["./gradlew", "build"], unit="u.scope",
                               memory_max_mib=8192)
        self.assertIn("-p", argv)
        self.assertIn("MemoryMax=8192M", argv)    # 建型即写，无 set-property 竞态
        self.assertNotIn("MemoryMax", gate.slice_wrap(["c"]))  # 缺省不写

    def test_cli_task_cap_plumbed(self):
        old, kw = gate.run_gated, {}

        def fake(cmd, **k):
            kw.update(k)
            return 0

        gate.run_gated = fake
        try:
            gate.main(["run", "--task-cap", "3", "--", "true"])
        finally:
            gate.run_gated = old
        self.assertEqual(kw["task_cap_gib"], 3)


class WatchdogTickTest(unittest.TestCase):
    """v3.3 看门狗单 tick 判定表：自预算超→杀己；聚合超帽→最大者逐杀；
    平静/零占用/缺席不动。cgroup 树用 fixture 注入。"""

    def make_tree(self, children):
        root = tempfile.mkdtemp()
        uid = os.getuid()
        base = Path(root) / "user.slice" / f"user-{uid}.slice" \
            / f"user@{uid}.service" / "gt6gate.slice"
        for name, mib in children.items():
            d = base / name
            d.mkdir(parents=True)
            (d / "memory.current").write_text(f"{mib * 1024 * 1024}\n")
        return root

    def tick(self, children, unit="me.scope", budget=8192, cap=25600):
        root = self.make_tree(children) if children else tempfile.mkdtemp()
        stops = []
        try:
            own, kills = gate.watchdog_tick(
                unit, budget, cap, cgroup_root=root,
                stop_fn=lambda name: stops.append(name))
        finally:
            shutil.rmtree(root)
        return own, kills, stops

    def test_own_over_budget_kills_self(self):
        own, kills, stops = self.tick({"me.scope": 9000, "sib.scope": 100})
        self.assertEqual(own, 9000)
        self.assertEqual(kills, [("task", "me.scope", 9000)])
        self.assertEqual(stops, ["me.scope"])

    def test_quiet_under_both_limits(self):
        own, kills, stops = self.tick({"me.scope": 100, "sib.scope": 200})
        self.assertEqual((own, kills, stops), (100, [], []))

    def test_aggregate_kills_largest_sibling_first(self):
        # 总 27100 > 25600：最大者 sibA(15000) 先杀 → 回到 12100 ≤ 帽即停
        own, kills, stops = self.tick({"me.scope": 100, "sibA.scope": 15000,
                                       "sibB.scope": 12000})
        self.assertEqual(kills, [("aggregate", "sibA.scope", 15000)])
        self.assertEqual(stops, ["sibA.scope"])   # 兄弟各杀各的 cgroup

    def test_aggregate_kills_multiple_until_under_cap(self):
        # 帽 10000：sibA(15000)→剩 14100 仍超 → 再杀 sibB(14000)→100 ≤ 帽
        own, kills, stops = self.tick({"me.scope": 100, "sibA.scope": 15000,
                                       "sibB.scope": 14000}, cap=10000)
        self.assertEqual([k[1] for k in kills], ["sibA.scope", "sibB.scope"])
        self.assertEqual(stops, ["sibA.scope", "sibB.scope"])
        self.assertEqual(own, 100)                # 自己活着

    def test_self_largest_means_aggregate_self_kill(self):
        # 自己是最大占用但仍在自己预算内（8000 ≤ 8192）：规则②最大者优先
        # 命中自己 → aggregate 自杀（victim==unit → 上层转 BUDGET_EXIT）
        own, kills, stops = self.tick({"me.scope": 8000, "sib.scope": 7000},
                                      cap=12000)
        self.assertEqual(kills, [("aggregate", "me.scope", 8000)])
        self.assertEqual(stops, ["me.scope"])

    def test_zero_usage_children_skipped(self):
        own, kills, stops = self.tick({"me.scope": 100, "zomb.scope": 0,
                                       "sib.scope": 15000}, cap=14000)
        # 总 15100 > 14000：zomb(0) 跳过，杀 sib → 100 ≤ 帽
        self.assertEqual(kills, [("aggregate", "sib.scope", 15000)])

    def test_missing_slice_dir_kills_nothing(self):
        own, kills, stops = self.tick(None)
        self.assertEqual((own, kills, stops), (0, [], []))


class WatchdogRunTest(unittest.TestCase):
    """v3.3 run_gated rc 语义：杀己→BUDGET_EXIT 97（≠普通失败）；
    杀兄弟→自身照常透传；未包装→不看门狗。"""

    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.tmp = Path(self.dir.name)
        self._mem_env = os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "999999"
        self._slice_env = os.environ.pop(gate.SLICE_ENV, None)
        os.environ[gate.SLICE_ENV] = "1"          # 强制包装分支
        self._slice_read = gate.slice_usage_mib
        gate.slice_usage_mib = lambda *a, **k: 0
        self._sample = gate.SAMPLE_SECONDS       # 提速：看门狗节奏钉小
        gate.SAMPLE_SECONDS = 0.05
        self._ci = os.environ.pop("GITHUB_ACTIONS", None)

    def tearDown(self):
        self.dir.cleanup()
        if self._mem_env is None:
            os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
        else:
            os.environ["GT6_GATE_MEM_LIMIT_MIB"] = self._mem_env
        if self._slice_env is None:
            os.environ.pop(gate.SLICE_ENV, None)
        else:
            os.environ[gate.SLICE_ENV] = self._slice_env
        gate.slice_usage_mib = self._slice_read
        gate.SAMPLE_SECONDS = self._sample
        if self._ci is not None:
            os.environ["GITHUB_ACTIONS"] = self._ci

    def gated(self, watchdog_fn, auto_exit=None):
        """auto_exit=None：marker-wait 子进程（stop 触碰 marker 才退）——
        模拟被看门狗杀；数字：立即退出该码（透传路径）。"""
        marker = self.tmp / "die"
        script = self.tmp / "child.sh"
        if auto_exit is None:
            script.write_text(f"#!/bin/sh\n"
                              f"while [ ! -f {marker} ]; do sleep 0.02; done\n"
                              f"exit 0\n")
        else:
            script.write_text(f"#!/bin/sh\nexit {auto_exit}\n")
        script.chmod(0o755)
        stops = []
        err = io.StringIO()

        def spy_stop(name, log=None):
            stops.append(name)
            marker.touch()

        log_file = self.tmp / "gate.log"
        with contextlib.redirect_stderr(err):
            rc = gate.run_gated([str(script)], poll=0.01,
                                slot_dir=self.tmp / "slots",
                                ledger_path=self.tmp / "ledger.json",
                                log_file=log_file,
                                slice_bootstrap_fn=lambda c, s: True,
                                stop_fn=spy_stop, watchdog_fn=watchdog_fn)
        return rc, stops, err.getvalue(), log_file.read_text(encoding="utf-8")

    def test_own_budget_kill_exits_97(self):
        calls = []

        def watchdog(unit, budget, cap, stop_fn=None, **kw):
            calls.append((unit, budget, cap))
            if len(calls) == 1:
                return 100, []
            if len(calls) == 2:
                stop_fn(unit)                 # 与真 watchdog_tick 同形：先杀
                return 9000, [("task", unit, 9000)]
            return 0, []                      # 子进程死后不再补刀

        rc, stops, err, journal = self.gated(watchdog)
        self.assertEqual(rc, gate.BUDGET_EXIT)    # 专码≠普通失败
        # stops[0]=看门狗杀己；stops[1]=子进程退出后的常规收尾杀（幂等空转）
        self.assertEqual(len(stops), 2)
        self.assertEqual(stops[0], calls[0][0])
        self.assertEqual(stops[1], calls[0][0])
        self.assertEqual(calls[0][1], gate.TASK_CAP_MIB["other"])  # 预算来自分级表
        self.assertEqual(calls[0][2], 25 * 1024)  # 项目帽=cap_gib*1024
        self.assertIn("task exceeded 9000 MiB budget (8192 MiB, class other)",
                      err)
        self.assertIn("budget-kill", journal)     # 审计流水

    def test_aggregate_self_kill_exits_97(self):
        def watchdog(unit, budget, cap, stop_fn=None, **kw):
            if not watchdog.fired:
                watchdog.fired = True
                return 100, []
            stop_fn(unit)
            return 20000, [("aggregate", unit, 20000)]

        watchdog.fired = False
        rc, stops, err, journal = self.gated(watchdog)
        self.assertEqual(rc, gate.BUDGET_EXIT)
        self.assertIn("aggregate over 25G cap — killed own cgroup (20000 MiB)",
                      err)
        self.assertIn("aggregate-kill", journal)

    def test_sibling_kill_passes_child_rc_through(self):
        # 透传属性本身已由 unwrapped 直跑（RunGatedTest）+ fake systemd-run
        # 端到端（SliceWrapTest）双钉；本钉增量=兄弟被杀时自身退出码经
        # 真 systemd-run 仍透传。用户 session bus 不可达的环境（agent 沙箱
        # 壳实测 Connection refused，main 基线即红）真 wrapper 起不来——
        # 探针 skip，同 SliceLiveTest 先例（2026-09-29 v3.5 卡补）。
        probe = subprocess.run(
            ["systemd-run", "--user", "--scope", "--", "/bin/true"],
            capture_output=True, timeout=30)
        if probe.returncode != 0:
            self.skipTest("user scope bus unreachable: "
                          f"{probe.stderr.decode(errors='replace').strip()[:80]}")

        def watchdog(unit, budget, cap, stop_fn=None, **kw):
            if not watchdog.fired:
                watchdog.fired = True
                stop_fn("sib.scope")
                return 100, [("aggregate", "sib.scope", 20000)]
            return 100, []

        watchdog.fired = False
        rc, stops, err, journal = self.gated(watchdog, auto_exit=5)
        self.assertEqual(rc, 5)                   # 自己照常透传
        # stops[0]=看门狗杀兄弟；stops[1]=自己的常规收尾杀
        self.assertEqual(len(stops), 2)
        self.assertEqual(stops[0], "sib.scope")
        self.assertIn("killed sib.scope (largest, 20000 MiB)", err)
        self.assertIn("aggregate-kill", journal)

    def test_unwrapped_run_skips_watchdog(self):
        os.environ[gate.SLICE_ENV] = "0"          # 降级直跑分支
        calls = []
        rc = gate.run_gated(["true"], poll=0.01, slot_dir=self.tmp / "slots",
                            ledger_path=self.tmp / "ledger.json",
                            slice_bootstrap_fn=lambda c, s: False,
                            watchdog_fn=lambda u, b, c, **k: calls.append(1))
        self.assertEqual(rc, 0)
        self.assertEqual(calls, [])               # 无 cgroup 即无看门狗


@unittest.skipUnless(os.environ.get("GT6_GATE_SLICE_LIVE"),
                     "live systemd probe (slow): set GT6_GATE_SLICE_LIVE=1")
class SliceLiveTest(unittest.TestCase):
    """可选 live 探针：真 systemd 建 100M scope（主会话 2026-09-29 已实证）。"""

    def test_scope_creates_and_runs(self):
        self.assertTrue(gate.slice_bootstrap())          # 真 systemctl 属性幂等
        probe = subprocess.run(
            ["systemd-run", "--user", "--scope", "-p", "MemoryMax=100M",
             "--", "/bin/true"], capture_output=True, timeout=30)
        self.assertEqual(probe.returncode, 0, probe.stderr.decode())

    def test_stop_unit_reaps_hidden_daemon(self):
        # 复现实测案例（2026-09-29）：leader 退出、daemon 留守 scope cgroup
        # → stop_unit TERM 后组内清空，scope 自灭
        unit = gate.unit_name()
        leader = subprocess.Popen(
            ["systemd-run", "--user", "--scope", f"--unit={unit}",
             "-p", "Slice=gt6gate.slice", "--", "sh", "-c",
             "nohup sleep 60 >/dev/null 2>&1 & sleep 0.3; exit 0"],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        leader.wait(timeout=30)
        deadline = time.monotonic() + 10    # 等留守 sleep 浮现在 cgroup.procs
        while not gate.unit_pids(unit) and time.monotonic() < deadline:
            time.sleep(0.1)
        leftovers = gate.unit_pids(unit)
        self.assertTrue(leftovers, f"leftover never appeared in {unit}")
        self.assertTrue(gate.stop_unit(unit, log=print))
        self.assertEqual(gate.unit_pids(unit), [])   # 组已清空

    def test_watchdog_kills_malloc_bloat(self):
        # v3.3 live：真 scope 里 1G 预算跑持续膨胀脚本——必须死在预算上，
        # 退出码区分执行者（2026-09-29 实测：本机 scope 级 memory.max 其实
        # 被内核执行，300M 探针 OOM rc=137——故内核先到即 -9，看门狗先到
        # 即 97；两者都算「按预算杀」）
        old = os.environ.get("GT6_GATE_MEM_LIMIT_MIB")
        os.environ["GT6_GATE_MEM_LIMIT_MIB"] = "999999"  # 外压护栏不拦探针
        bloat = ("import time;b=bytearray();"
                 "[ (b.extend(bytearray(512*1024*1024)), time.sleep(0.4))"
                 "  for _ in range(12)]")
        tmp = tempfile.mkdtemp()
        err = io.StringIO()
        try:
            with contextlib.redirect_stderr(err):
                rc = gate.run_gated(
                    [sys.executable, "-c", bloat], poll=0.01,
                    slot_dir=Path(tmp) / "slots",
                    ledger_path=Path(tmp) / "ledger.json",
                    task_cap_gib=1)
            self.assertIn(rc, (gate.BUDGET_EXIT, -9))
            if rc == gate.BUDGET_EXIT:
                self.assertIn("MiB budget (1024 MiB, class other)",
                              err.getvalue())
        finally:
            if old is None:
                os.environ.pop("GT6_GATE_MEM_LIMIT_MIB", None)
            else:
                os.environ["GT6_GATE_MEM_LIMIT_MIB"] = old
            shutil.rmtree(tmp, ignore_errors=True)


class DryRunTest(unittest.TestCase):
    """v3.2 --dry-run 四场景：冷启动 ADMIT / 信封超限 QUEUE / 高基线
    ADMIT（v3.5 外压退役，outside 仅诊断行）/ full+coder REJECT。
    宿主读数全注入。"""

    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.tmp = Path(self.dir.name)
        self.ledger = self.tmp / "ledger.json"

    def tearDown(self):
        self.dir.cleanup()

    def report(self, cmd, used=8000, slice_cur=0, **kw):
        lines = []
        word = gate.dry_run(cmd, ledger_path=self.ledger, log=lines.append,
                            read_used=lambda: used, read_slice=lambda: slice_cur,
                            **kw)
        return word, lines

    def test_cold_start_admits(self):
        word, lines = self.report(["./gradlew", "compileJava"])
        self.assertEqual(word, "ADMIT")
        body = "\n".join(lines)
        self.assertIn("class=compile", body)
        self.assertIn("slice current:  0 MiB", body)
        self.assertIn("cold default", body)
        self.assertIn("4000 MiB", body)          # 冷启动估算
        # envelope: 0 + 4000 = 4000 ≤ cap 25600；outside 沦为信息项
        self.assertIn("envelope:       slice 0 + estimate 4000 = 4000 MiB "
                      "(cap 25600 MiB)", body)
        self.assertIn("outside:        used 8000 - slice 0 = 8000 MiB "
                      "(informational", body)

    def test_envelope_over_cap_queues(self):
        gate.record_observation("full-test", 26000, self.ledger)
        word, lines = self.report(["./gradlew", ":mdk:cleanTest"],
                                  role="review", used=8000, slice_cur=1000)
        self.assertEqual(word, "QUEUE")
        body = "\n".join(lines)
        self.assertIn("ledger", body)            # 来源=台账非冷启动
        # 信封判据：slice 1000 + est 26000 = 27000 > cap 25600
        self.assertIn("envelope slice 1000 + estimate 26000 = 27000 MiB "
                      "> cap 25600 MiB", body)

    def test_high_baseline_admits_outside_informational(self):
        # v3.5 回归钉（旧行为=QUEUE ungated pressure）：非门禁进程吃满机器
        # 不再阻塞准入——decision 只由 envelope 决定，outside 降为诊断行
        word, lines = self.report(["./gradlew", "compileJava"],
                                  used=30000, slice_cur=500)
        self.assertEqual(word, "ADMIT")
        body = "\n".join(lines)
        self.assertIn("outside:        used 30000 - slice 500 = 29500 MiB", body)
        self.assertIn("informational", body)
        self.assertNotIn("ungated pressure", body)

    def test_full_coder_rejects_before_prediction(self):
        word, lines = self.report(["./gradlew", ":mdk:cleanTest"], used=100)
        self.assertEqual(word, "REJECT")
        body = "\n".join(lines)
        self.assertIn("exit 2", body)
        self.assertIn("review-seat", body)

    def test_cap_override_moves_thresholds(self):
        # --cap 30：信封帽与外压阈随之移动（envelope cap 30720）
        word, lines = self.report(["./gradlew", "compileJava"], cap_gib=30)
        self.assertEqual(word, "ADMIT")
        self.assertIn("(cap 30720 MiB)", "\n".join(lines))

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
