#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""test_datagen_tree_check.py — datagen_tree_check 的 stdlib unittest 单测。

零第三方依赖（python3 tools/test_datagen_tree_check.py 直跑）。覆盖：
  * p27 品牌段映射带：brand_normalize 路径折叠 / _fold 碰撞守卫 /
    _norm_add_features_brand 值形归一 / main() 端到端 17+17→0 形。
  * （p27 后续提交追加）陈旧守卫 STALE 判定与逃生阀。

证据基线：main 树实测（2026-09-12，HEAD fe519aa8）——canonical
data/gt6/forge/biome_modifier/ 17 文件 vs 节点 data/gt6/neoforge/biome_modifier/
17 文件互为 only-*，逐对仅 "type" 一键差（forge:add_features ↔ neoforge:add_features）。
"""

from __future__ import annotations

import contextlib
import importlib.util
import io
import json
import subprocess
import sys
import tempfile
import time
import unittest
from pathlib import Path, PurePosixPath

_TOOLS = Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("datagen_tree_check",
                                               _TOOLS / "datagen_tree_check.py")
mod = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(mod)


def _gson(obj: dict) -> bytes:
    """与双腿 datagen Gson setIndent("  ") 契约同形：indent=2 / 无尾换行 / 非 ASCII 原样。"""
    return json.dumps(obj, indent=2, ensure_ascii=False).encode()


def _biome_json(brand_prefix: str) -> bytes:
    """census 实测形（overworld_stone_andesite.json，双腿仅 type 前缀差）。"""
    return _gson({
        "type": f"{brand_prefix}:add_features",
        "biomes": "#minecraft:is_overworld",
        "features": "gt6:overworld_stone_andesite",
        "step": "underground_ores",
    })


class TestBrandNormalize(unittest.TestCase):
    """层 1 品牌段作用域折叠（路径面）。"""

    def test_node_neoforge_biome_modifier_folds_to_forge(self):
        rel = PurePosixPath("data/gt6/neoforge/biome_modifier/overworld_stone_andesite.json")
        self.assertEqual(
            mod.normalize(rel),
            PurePosixPath("data/gt6/forge/biome_modifier/overworld_stone_andesite.json"))

    def test_canonical_forge_form_is_identity(self):
        rel = PurePosixPath("data/gt6/forge/biome_modifier/overworld_stone_andesite.json")
        self.assertEqual(mod.normalize(rel), rel)
        self.assertEqual(mod.brand_normalize(rel), rel)

    def test_fold_is_symmetric_on_the_pair(self):
        c = PurePosixPath("data/gt6/forge/biome_modifier/x.json")
        n = PurePosixPath("data/gt6/neoforge/biome_modifier/x.json")
        self.assertEqual(mod.normalize(c), mod.normalize(n))

    def test_brand_out_of_band_paths_untouched(self):
        # 带外（band 段不符 / 顶层非 data / 深度不足）一概不动
        for rel in (
            PurePosixPath("data/gt6/neoforge/tags/blocks/x.json"),   # 段[3]≠biome_modifier
            PurePosixPath("assets/gt6/models/neoforge/x.json"),      # 顶层非 data
            PurePosixPath("data/gt6/neoforge.json"),                 # 深度不足
            PurePosixPath("data/neoforge/biome_modifier/x.json"),    # 段[2]非品牌位（ns 位）
        ):
            self.assertEqual(mod.brand_normalize(rel), rel, msg=str(rel))


class TestFoldGuard(unittest.TestCase):
    """_fold：品牌折叠撞键 → 硬 ERROR；既有全局折叠阴影维持后写覆盖现行为。"""

    def test_dual_brand_same_side_hard_errors(self):
        files = {
            PurePosixPath("data/gt6/forge/biome_modifier/a.json"): Path("/c/a.json"),
            PurePosixPath("data/gt6/neoforge/biome_modifier/a.json"): Path("/c2/a.json"),
        }
        with self.assertRaises(SystemExit) as cm:
            mod._fold(files)
        self.assertIn("brand-fold collision", str(cm.exception))

    def test_brand_folded_key_then_plain_key_also_errors(self):
        # 折叠文件先到、原形 forge 文件后到：同样撞键（守卫须双向）
        files = {
            PurePosixPath("data/gt6/neoforge/biome_modifier/a.json"): Path("/n/a.json"),
            PurePosixPath("data/gt6/forge/biome_modifier/a.json"): Path("/c/a.json"),
        }
        with self.assertRaises(SystemExit):
            mod._fold(files)

    def test_legacy_global_fold_shadow_keeps_last_wins(self):
        # SEGMENT_MAP 层既有阴影（singular/plural 瞬态并存）不归本守卫管：行为不变
        files = {
            PurePosixPath("data/gt6/loot_table/blocks/a.json"): Path("/n/a.json"),
            PurePosixPath("data/gt6/loot_tables/blocks/a.json"): Path("/c/a.json"),
        }
        out = mod._fold(files)
        self.assertEqual(len(out), 1)
        key = PurePosixPath("data/gt6/loot_tables/blocks/a.json")
        self.assertIn(key, out)
        self.assertEqual(out[key], Path("/c/a.json"))  # 后写覆盖（插入序）

    def test_distinct_biome_names_do_not_collide(self):
        files = {
            PurePosixPath("data/gt6/forge/biome_modifier/a.json"): Path("/c/a.json"),
            PurePosixPath("data/gt6/neoforge/biome_modifier/b.json"): Path("/n/b.json"),
        }
        out = mod._fold(files)
        self.assertEqual(len(out), 2)


class TestAddFeaturesValueNormalizer(unittest.TestCase):
    """层 2 品牌段值形归一（type 键前缀）。"""

    def test_registered_band_matches_canonical_form(self):
        rel = PurePosixPath("data/gt6/forge/biome_modifier/overworld_stone_andesite.json")
        regs = mod._registered_band(rel)
        self.assertIsNotNone(regs)
        self.assertEqual([name for name, _ in regs],
                         ["add-features-neoforge→forge"])

    def test_node_brand_normalizes_to_canonical_bytes(self):
        rel = PurePosixPath("data/gt6/forge/biome_modifier/overworld_stone_andesite.json")
        c = _biome_json("forge")
        n = _biome_json("neoforge")
        out = mod.try_value_normalize(rel, c, n)
        self.assertIsNotNone(out)
        self.assertEqual(out[0], c)
        self.assertEqual(out[1], "add-features-neoforge→forge")

    def test_unregistered_brand_shape_stays_fail_visible(self):
        # remove_features 未注册：归一通道不吞，返回 None → 原样 FAIL
        rel = PurePosixPath("data/gt6/forge/biome_modifier/x.json")
        c = _gson({"type": "forge:remove_features", "biomes": []})
        n = _gson({"type": "neoforge:remove_features", "biomes": []})
        self.assertIsNone(mod.try_value_normalize(rel, c, n))

    def test_non_biome_band_not_matched(self):
        rel = PurePosixPath("data/gt6/recipes/x.json")
        c = _biome_json("forge")
        n = _biome_json("neoforge")
        self.assertIsNone(mod.try_value_normalize(rel, c, n))

    def test_byte_equal_never_enters_normalizer(self):
        rel = PurePosixPath("data/gt6/forge/biome_modifier/x.json")
        b = _biome_json("forge")
        # 快路径语义由 main() 保证（byte 相等不进归一通道）；此处只钉归一器对同字节
        # 输入的零施用（canonical 形 forge: 前缀不被 _norm_add_features_brand 消费）
        self.assertIsNone(mod.try_value_normalize(rel, b, b))


class TestMainEndToEndBrand(unittest.TestCase):
    """端到端：canonical forge/ + node neoforge/ 双目录 → biome 带 17+17→0 形。"""

    def test_biome_brand_pair_goes_green(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            canon = root / "canonical"
            node = root / "node"
            (canon / "data/gt6/forge/biome_modifier").mkdir(parents=True)
            (node / "data/gt6/neoforge/biome_modifier").mkdir(parents=True)
            (canon / "assets/gt6/lang").mkdir(parents=True)
            (node / "assets/gt6/lang").mkdir(parents=True)
            (canon / "data/gt6/forge/biome_modifier/overworld_stone_andesite.json"
             ).write_bytes(_biome_json("forge"))
            (node / "data/gt6/neoforge/biome_modifier/overworld_stone_andesite.json"
             ).write_bytes(_biome_json("neoforge"))
            lang = _gson({"gt6.row.x": "X"})
            (canon / "assets/gt6/lang/en_us.json").write_bytes(lang)
            (node / "assets/gt6/lang/en_us.json").write_bytes(lang)

            argv = sys.argv
            buf = io.StringIO()
            try:
                sys.argv = ["datagen_tree_check.py",
                            "--canonical", str(canon), "--node-output", str(node)]
                with contextlib.redirect_stdout(buf):
                    rc = mod.main()
            finally:
                sys.argv = argv
            out = buf.getvalue()
            self.assertEqual(rc, 0, msg=out)
            self.assertIn("NORMALIZED [add-features-neoforge→forge] "
                          "data/gt6/forge/biome_modifier/overworld_stone_andesite.json",
                          out)
            self.assertIn("biome band: canonical 1 (data/*/forge/biome_modifier)"
                          "  node 1 (data/*/neoforge/biome_modifier)", out)
            self.assertIn("RESULT: OK", out)

    def test_unpaired_brand_shape_still_fails(self):
        # 归一不是吞差：node 侧连 features 都改了 → 归一后仍不等 → FAIL + 定位
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            canon = root / "canonical"
            node = root / "node"
            (canon / "data/gt6/forge/biome_modifier").mkdir(parents=True)
            (node / "data/gt6/neoforge/biome_modifier").mkdir(parents=True)
            (canon / "data/gt6/forge/biome_modifier/a.json"
             ).write_bytes(_biome_json("forge"))
            drift = json.loads(_biome_json("neoforge"))
            drift["features"] = "gt6:somewhere_else"
            (node / "data/gt6/neoforge/biome_modifier/a.json").write_bytes(_gson(drift))

            argv = sys.argv
            buf = io.StringIO()
            try:
                sys.argv = ["datagen_tree_check.py",
                            "--canonical", str(canon), "--node-output", str(node)]
                with contextlib.redirect_stdout(buf):
                    rc = mod.main()
            finally:
                sys.argv = argv
            out = buf.getvalue()
            self.assertEqual(rc, 1, msg=out)
            self.assertIn("DIFF [content]", out)
            self.assertIn("RESULT: FAIL", out)


class TestStaleGuardMarkers(unittest.TestCase):
    """陈旧守卫的时戳标记面（p27-ops）。"""

    def test_parse_cache_ledger_header(self):
        # 实测样本形（main 节点输出 .cache/<sha1> 首行，纳秒精度）
        line = ("// 1.21.1\t2026-09-12T00:57:47.204892185\t"
                "Language Provider: gt6:mold[en_us]")
        expected = (time.mktime(time.strptime("2026-09-12T00:57:47",
                                              "%Y-%m-%dT%H:%M:%S")) + 0.204892)
        self.assertAlmostEqual(mod._parse_cache_ts(line), expected, places=3)

    def test_parse_cache_ledger_garbage(self):
        self.assertIsNone(mod._parse_cache_ts("not a ledger line"))
        self.assertIsNone(mod._parse_cache_ts("// 1.21.1\tnot-a-date\tprovider"))
        self.assertIsNone(mod._parse_cache_ts(""))

    def test_node_gen_epoch_prefers_ledger_over_mtime(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            (root / ".cache").mkdir()
            old = time.strftime("%Y-%m-%dT%H:%M:%S", time.localtime(10 ** 9)) + ".5"
            (root / ".cache" / "aaa").write_text(f"// 1.21.1\t{old}\tProvider P\n")
            prod = root / "assets" / "x.json"
            prod.parent.mkdir(parents=True)
            prod.write_text("{}")  # mtime = now >> 账本时戳(2001)
            gen, marker = mod.node_gen_epoch(
                root, {PurePosixPath("assets/x.json"): prod})
            self.assertIsNotNone(gen)
            self.assertLess(gen, time.time())  # 选了账本时戳而非 mtime
            self.assertIn(".cache ledger", marker)

    def test_node_gen_epoch_mtime_fallback_without_ledger(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            prod = root / "x.json"
            prod.write_text("{}")
            gen, marker = mod.node_gen_epoch(root, {PurePosixPath("x.json"): prod})
            self.assertIsNotNone(gen)
            self.assertIn("mtime fallback", marker)
            self.assertAlmostEqual(gen, prod.stat().st_mtime, places=6)

    def test_node_gen_epoch_none_when_empty(self):
        with tempfile.TemporaryDirectory() as td:
            self.assertIsNone(mod.node_gen_epoch(Path(td), {}))


class TestStaleGuardEndToEnd(unittest.TestCase):
    """端到端：STALE FAIL（默认）→ --allow-stale 逃生 → fresh 快照放行。"""

    @staticmethod
    def _git(cwd: Path, *args: str) -> None:
        subprocess.run(["git", "-C", str(cwd), *args],
                       check=True, capture_output=True, text=True, timeout=60)

    def _run_main(self, canon: Path, node: Path, extra: list[str]) -> tuple[int, str]:
        argv = sys.argv
        buf = io.StringIO()
        try:
            sys.argv = ["datagen_tree_check.py", "--canonical", str(canon),
                        "--node-output", str(node), *extra]
            with contextlib.redirect_stdout(buf):
                rc = mod.main()
        finally:
            sys.argv = argv
        return rc, buf.getvalue()

    def test_stale_snapshot_fails_then_escape_allows(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            # git 仓库放上层、canonical 根是其子目录——collect_files 会把 .git/ 收进
            # 产物集，故不能在 canonical 根内 init
            repo = root / "repo"
            canon = repo / "tree"
            node = root / "node"
            (canon / "data/gt6/forge/biome_modifier").mkdir(parents=True)
            (node / "data/gt6/neoforge/biome_modifier").mkdir(parents=True)
            (canon / "data/gt6/forge/biome_modifier/a.json"
             ).write_bytes(_biome_json("forge"))
            (node / "data/gt6/neoforge/biome_modifier/a.json"
             ).write_bytes(_biome_json("neoforge"))
            # canonical 侧：真 git 仓库，HEAD 提交时间 = now
            self._git(repo, "init", "-q")
            self._git(repo, "-c", "user.email=t@t", "-c", "user.name=t",
                      "add", "-A")
            self._git(repo, "-c", "user.email=t@t", "-c", "user.name=t",
                      "commit", "-q", "-m", "canonical write")
            # node 侧：账本时戳钉在 2001（<< HEAD）→ 陈旧
            (node / ".cache").mkdir()
            (node / ".cache" / "led").write_text(
                "// 1.21.1\t2001-01-01T00:00:00.0\tProvider P\n")

            rc, out = self._run_main(canon, node, [])
            self.assertEqual(rc, 1, msg=out)
            self.assertIn("RESULT: FAIL — STALE node snapshot", out)
            self.assertIn("--allow-stale", out)

            rc, out = self._run_main(canon, node, ["--allow-stale"])
            self.assertEqual(rc, 0, msg=out)
            self.assertIn("STALE-WARN (allowed by --allow-stale)", out)
            self.assertIn("NORMALIZED [add-features-neoforge→forge] "
                          "data/gt6/forge/biome_modifier/a.json", out)
            self.assertIn("RESULT: OK", out)

    def test_fresh_snapshot_passes_guard(self):
        # 账本时戳 = 现在（>= HEAD 提交钟面）→ STALE-CHECK OK，无 WARN 无 FAIL
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            repo = root / "repo"
            canon = repo / "tree"
            node = root / "node"
            (canon / "data/gt6/forge/biome_modifier").mkdir(parents=True)
            (node / "data/gt6/neoforge/biome_modifier").mkdir(parents=True)
            (canon / "data/gt6/forge/biome_modifier/a.json"
             ).write_bytes(_biome_json("forge"))
            (node / "data/gt6/neoforge/biome_modifier/a.json"
             ).write_bytes(_biome_json("neoforge"))
            self._git(repo, "init", "-q")
            self._git(repo, "-c", "user.email=t@t", "-c", "user.name=t",
                      "add", "-A")
            self._git(repo, "-c", "user.email=t@t", "-c", "user.name=t",
                      "commit", "-q", "-m", "canonical write")
            now = time.time()
            stamp = time.strftime("%Y-%m-%dT%H:%M:%S", time.localtime(now))
            (node / ".cache").mkdir()
            (node / ".cache" / "led").write_text(
                f"// 1.21.1\t{stamp}.0\tProvider P\n")

            rc, out = self._run_main(canon, node, [])
            self.assertEqual(rc, 0, msg=out)
            self.assertIn("STALE-CHECK OK", out)


if __name__ == "__main__":
    unittest.main()
